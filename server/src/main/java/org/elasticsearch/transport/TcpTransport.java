/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.transport;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Booleans;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.metrics.MeanMetric;
import org.elasticsearch.common.network.CloseableChannel;
import org.elasticsearch.common.network.NetworkAddress;
import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.BoundTransportAddress;
import org.elasticsearch.common.transport.PortsRange;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.PageCacheRecycler;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.CountDown;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.indices.breaker.CircuitBreakerService;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticsearch.node.Node;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.CancelledKeyException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableMap;
import static org.elasticsearch.common.transport.NetworkExceptionHelper.isCloseConnectionException;
import static org.elasticsearch.common.transport.NetworkExceptionHelper.isConnectException;
import static org.elasticsearch.common.util.concurrent.ConcurrentCollections.newConcurrentMap;

public abstract class TcpTransport extends AbstractLifecycleComponent implements Transport {
    private static final Logger logger = LogManager.getLogger(TcpTransport.class);

    public static final String TRANSPORT_WORKER_THREAD_NAME_PREFIX = "transport_worker";


    // This is the number of bytes necessary to read the message size
    private static final int BYTES_NEEDED_FOR_MESSAGE_SIZE = TcpHeader.MARKER_BYTES_SIZE + TcpHeader.MESSAGE_LENGTH_SIZE;
    private static final long NINETY_PER_HEAP_SIZE = (long) (JvmInfo.jvmInfo().getMem().getHeapMax().getBytes() * 0.9);
    private static final BytesReference EMPTY_BYTES_REFERENCE = new BytesArray(new byte[0]);

    private final String[] features;

    protected final Settings settings;
    private final CircuitBreakerService circuitBreakerService;
    private final Version version;
    protected final ThreadPool threadPool;
    protected final BigArrays bigArrays;
    protected final PageCacheRecycler pageCacheRecycler;
    protected final NetworkService networkService;
    protected final Set<ProfileSettings> profileSettings;

    private final DelegatingTransportMessageListener messageListener = new DelegatingTransportMessageListener();

    private final ConcurrentMap<String, BoundTransportAddress> profileBoundAddresses = newConcurrentMap();
    private final Map<String, List<TcpServerChannel>> serverChannels = newConcurrentMap();
    private final Set<TcpChannel> acceptedChannels = ConcurrentCollections.newConcurrentSet();

    // this lock is here to make sure we close this transport and disconnect all the client nodes
    // connections while no connect operations is going on
    private final ReadWriteLock closeLock = new ReentrantReadWriteLock();
    private volatile BoundTransportAddress boundAddress;
    private final String transportName;

    private final MeanMetric readBytesMetric = new MeanMetric();
    private volatile Map<String, RequestHandlerRegistry<? extends TransportRequest>> requestHandlers = Collections.emptyMap();
    private final ResponseHandlers responseHandlers = new ResponseHandlers();
    private final TransportLogger transportLogger;
    private final TransportHandshaker handshaker;
    private final TransportKeepAlive keepAlive;
    private final InboundMessage.Reader reader;
    private final OutboundHandler outboundHandler;
    private final String nodeName;

    public TcpTransport(String transportName, Settings settings, Version version, ThreadPool threadPool,
                        PageCacheRecycler pageCacheRecycler, CircuitBreakerService circuitBreakerService,
                        NamedWriteableRegistry namedWriteableRegistry, NetworkService networkService) {
        this.settings = settings;
        this.profileSettings = getProfileSettings(settings);
        this.version = version;
        this.threadPool = threadPool;
        this.bigArrays = new BigArrays(pageCacheRecycler, circuitBreakerService, CircuitBreaker.IN_FLIGHT_REQUESTS);
        this.pageCacheRecycler = pageCacheRecycler;
        this.circuitBreakerService = circuitBreakerService;
        this.networkService = networkService;
        this.transportName = transportName;
        this.transportLogger = new TransportLogger();
        this.outboundHandler = new OutboundHandler(threadPool, bigArrays, transportLogger);
        this.handshaker = new TransportHandshaker(version, threadPool,
            (node, channel, requestId, v) -> sendRequestToChannel(node, channel, requestId,
                TransportHandshaker.HANDSHAKE_ACTION_NAME, new TransportHandshaker.HandshakeRequest(version),
                TransportRequestOptions.EMPTY, v, false, true),
            (v, features, channel, response, requestId) -> sendResponse(v, features, channel, response, requestId,
                TransportHandshaker.HANDSHAKE_ACTION_NAME, false, true));
        this.keepAlive = new TransportKeepAlive(threadPool, this.outboundHandler::sendBytes);
        this.reader = new InboundMessage.Reader(version, namedWriteableRegistry, threadPool.getThreadContext());
        this.nodeName = Node.NODE_NAME_SETTING.get(settings);

        final Settings defaultFeatures = TransportSettings.DEFAULT_FEATURES_SETTING.get(settings);
        if (defaultFeatures == null) {
            this.features = new String[0];
        } else {
            defaultFeatures.names().forEach(key -> {
                if (Booleans.parseBoolean(defaultFeatures.get(key)) == false) {
                    throw new IllegalArgumentException("feature settings must have default [true] value");
                }
            });
            // use a sorted set to present the features in a consistent order
            this.features = new TreeSet<>(defaultFeatures.names()).toArray(new String[defaultFeatures.names().size()]);
        }
    }

    @Override
    protected void doStart() {
    }

    public void addMessageListener(TransportMessageListener listener) {
        messageListener.listeners.add(listener);
    }

    public boolean removeMessageListener(TransportMessageListener listener) {
        return messageListener.listeners.remove(listener);
    }

    @Override
    public CircuitBreaker getInFlightRequestBreaker() {
        // We always obtain a fresh breaker to reflect changes to the breaker configuration.
        return circuitBreakerService.getBreaker(CircuitBreaker.IN_FLIGHT_REQUESTS);
    }

    @Override
    public synchronized <Request extends TransportRequest> void registerRequestHandler(RequestHandlerRegistry<Request> reg) {
        if (requestHandlers.containsKey(reg.getAction())) {
            throw new IllegalArgumentException("transport handlers for action " + reg.getAction() + " is already registered");
        }
        requestHandlers = MapBuilder.newMapBuilder(requestHandlers).put(reg.getAction(), reg).immutableMap();
    }

    public final class NodeChannels extends CloseableConnection {
        private final Map<TransportRequestOptions.Type, ConnectionProfile.ConnectionTypeHandle> typeMapping;
        private final List<TcpChannel> channels;
        private final DiscoveryNode node;
        private final Version version;
        private final boolean compress;
        private final AtomicBoolean isClosing = new AtomicBoolean(false);

        NodeChannels(DiscoveryNode node, List<TcpChannel> channels, ConnectionProfile connectionProfile, Version handshakeVersion) {
            this.node = node;
            this.channels = Collections.unmodifiableList(channels);
            assert channels.size() == connectionProfile.getNumConnections() : "expected channels size to be == "
                + connectionProfile.getNumConnections() + " but was: [" + channels.size() + "]";
            typeMapping = new EnumMap<>(TransportRequestOptions.Type.class);
            for (ConnectionProfile.ConnectionTypeHandle handle : connectionProfile.getHandles()) {
                for (TransportRequestOptions.Type type : handle.getTypes())
                    typeMapping.put(type, handle);
            }
            version = handshakeVersion;
            compress = connectionProfile.getCompressionEnabled();
        }

        @Override
        public Version getVersion() {
            return version;
        }

        public List<TcpChannel> getChannels() {
            return channels;
        }

        public TcpChannel channel(TransportRequestOptions.Type type) {
            ConnectionProfile.ConnectionTypeHandle connectionTypeHandle = typeMapping.get(type);
            if (connectionTypeHandle == null) {
                throw new IllegalArgumentException("no type channel for [" + type + "]");
            }
            return connectionTypeHandle.getChannel(channels);
        }

        @Override
        public void close() {
            if (isClosing.compareAndSet(false, true)) {
                try {
                    boolean block = lifecycle.stopped() && Transports.isTransportThread(Thread.currentThread()) == false;
                    CloseableChannel.closeChannels(channels, block);
                } finally {
                    // Call the super method to trigger listeners
                    super.close();
                }
            }
        }

        @Override
        public DiscoveryNode getNode() {
            return this.node;
        }

        @Override
        public void sendRequest(long requestId, String action, TransportRequest request, TransportRequestOptions options)
            throws IOException, TransportException {
            if (isClosing.get()) {
                throw new NodeNotConnectedException(node, "connection already closed");
            }
            TcpChannel channel = channel(options.type());
            sendRequestToChannel(this.node, channel, requestId, action, request, options, getVersion(), compress);
        }
    }

    // This allows transport implementations to potentially override specific connection profiles. This
    // primarily exists for the test implementations.
    protected ConnectionProfile maybeOverrideConnectionProfile(ConnectionProfile connectionProfile) {
        return connectionProfile;
    }

    @Override
    public Releasable openConnection(DiscoveryNode node, ConnectionProfile profile, ActionListener<Transport.Connection> listener) {
        Objects.requireNonNull(profile, "connection profile cannot be null");
        if (node == null) {
            throw new ConnectTransportException(null, "can't open connection to a null node");
        }
        ConnectionProfile finalProfile = maybeOverrideConnectionProfile(profile);
        closeLock.readLock().lock(); // ensure we don't open connections while we are closing
        try {
            ensureOpen();
            List<TcpChannel> pendingChannels = initiateConnection(node, finalProfile, listener);
            return () -> CloseableChannel.closeChannels(pendingChannels, false);
        } finally {
            closeLock.readLock().unlock();
        }
    }

    private List<TcpChannel> initiateConnection(DiscoveryNode node, ConnectionProfile connectionProfile,
                                                ActionListener<Transport.Connection> listener) {
        int numConnections = connectionProfile.getNumConnections();
        assert numConnections > 0 : "A connection profile must be configured with at least one connection";

        final List<TcpChannel> channels = new ArrayList<>(numConnections);

        for (int i = 0; i < numConnections; ++i) {
            try {
                TcpChannel channel = initiateChannel(node);
                logger.trace(() -> new ParameterizedMessage("Tcp transport client channel opened: {}", channel));
                channels.add(channel);
            } catch (ConnectTransportException e) {
                CloseableChannel.closeChannels(channels, false);
                listener.onFailure(e);
                return channels;
            } catch (Exception e) {
                CloseableChannel.closeChannels(channels, false);
                listener.onFailure(new ConnectTransportException(node, "general node connection failure", e));
                return channels;
            }
        }

        ChannelsConnectedListener channelsConnectedListener = new ChannelsConnectedListener(node, connectionProfile, channels, listener);

        for (TcpChannel channel : channels) {
            channel.addConnectListener(channelsConnectedListener);
        }

        TimeValue connectTimeout = connectionProfile.getConnectTimeout();
        threadPool.schedule(channelsConnectedListener::onTimeout, connectTimeout, ThreadPool.Names.GENERIC);
        return channels;
    }

    @Override
    public BoundTransportAddress boundAddress() {
        return this.boundAddress;
    }

    @Override
    public Map<String, BoundTransportAddress> profileBoundAddresses() {
        return unmodifiableMap(new HashMap<>(profileBoundAddresses));
    }

    @Override
    public List<String> getLocalAddresses() {
        List<String> local = new ArrayList<>();
        local.add("127.0.0.1");
        // check if v6 is supported, if so, v4 will also work via mapped addresses.
        if (NetworkUtils.SUPPORTS_V6) {
            local.add("[::1]"); // may get ports appended!
        }
        return local;
    }

    protected void bindServer(ProfileSettings profileSettings) {
        // Bind and start to accept incoming connections.
        InetAddress[] hostAddresses;
        List<String> profileBindHosts = profileSettings.bindHosts;
        try {
            hostAddresses = networkService.resolveBindHostAddresses(profileBindHosts.toArray(Strings.EMPTY_ARRAY));
        } catch (IOException e) {
            throw new BindTransportException("Failed to resolve host " + profileBindHosts, e);
        }
        if (logger.isDebugEnabled()) {
            String[] addresses = new String[hostAddresses.length];
            for (int i = 0; i < hostAddresses.length; i++) {
                addresses[i] = NetworkAddress.format(hostAddresses[i]);
            }
            logger.debug("binding server bootstrap to: {}", (Object) addresses);
        }

        assert hostAddresses.length > 0;

        List<InetSocketAddress> boundAddresses = new ArrayList<>();
        for (InetAddress hostAddress : hostAddresses) {
            boundAddresses.add(bindToPort(profileSettings.profileName, hostAddress, profileSettings.portOrRange));
        }

        final BoundTransportAddress boundTransportAddress = createBoundTransportAddress(profileSettings, boundAddresses);

        if (profileSettings.isDefaultProfile) {
            this.boundAddress = boundTransportAddress;
        } else {
            profileBoundAddresses.put(profileSettings.profileName, boundTransportAddress);
        }
    }

    private InetSocketAddress bindToPort(final String name, final InetAddress hostAddress, String port) {
        PortsRange portsRange = new PortsRange(port);
        final AtomicReference<Exception> lastException = new AtomicReference<>();
        final AtomicReference<InetSocketAddress> boundSocket = new AtomicReference<>();
        closeLock.writeLock().lock();
        try {
            if (lifecycle.initialized() == false && lifecycle.started() == false) {
                throw new IllegalStateException("transport has been stopped");
            }
            boolean success = portsRange.iterate(portNumber -> {
                try {
                    TcpServerChannel channel = bind(name, new InetSocketAddress(hostAddress, portNumber));
                    serverChannels.computeIfAbsent(name, k -> new ArrayList<>()).add(channel);
                    boundSocket.set(channel.getLocalAddress());
                } catch (Exception e) {
                    lastException.set(e);
                    return false;
                }
                return true;
            });
            if (!success) {
                throw new BindTransportException("Failed to bind to [" + port + "]", lastException.get());
            }
        } finally {
            closeLock.writeLock().unlock();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Bound profile [{}] to address {{}}", name, NetworkAddress.format(boundSocket.get()));
        }

        return boundSocket.get();
    }

    private BoundTransportAddress createBoundTransportAddress(ProfileSettings profileSettings,
                                                              List<InetSocketAddress> boundAddresses) {
        String[] boundAddressesHostStrings = new String[boundAddresses.size()];
        TransportAddress[] transportBoundAddresses = new TransportAddress[boundAddresses.size()];
        for (int i = 0; i < boundAddresses.size(); i++) {
            InetSocketAddress boundAddress = boundAddresses.get(i);
            boundAddressesHostStrings[i] = boundAddress.getHostString();
            transportBoundAddresses[i] = new TransportAddress(boundAddress);
        }

        List<String> publishHosts = profileSettings.publishHosts;
        if (profileSettings.isDefaultProfile == false && publishHosts.isEmpty()) {
            publishHosts = Arrays.asList(boundAddressesHostStrings);
        }
        if (publishHosts.isEmpty()) {
            publishHosts = NetworkService.GLOBAL_NETWORK_PUBLISH_HOST_SETTING.get(settings);
        }

        final InetAddress publishInetAddress;
        try {
            publishInetAddress = networkService.resolvePublishHostAddresses(publishHosts.toArray(Strings.EMPTY_ARRAY));
        } catch (Exception e) {
            throw new BindTransportException("Failed to resolve publish address", e);
        }

        final int publishPort = resolvePublishPort(profileSettings, boundAddresses, publishInetAddress);
        final TransportAddress publishAddress = new TransportAddress(new InetSocketAddress(publishInetAddress, publishPort));
        return new BoundTransportAddress(transportBoundAddresses, publishAddress);
    }

    // package private for tests
    static int resolvePublishPort(ProfileSettings profileSettings, List<InetSocketAddress> boundAddresses,
                                  InetAddress publishInetAddress) {
        int publishPort = profileSettings.publishPort;

        // if port not explicitly provided, search for port of address in boundAddresses that matches publishInetAddress
        if (publishPort < 0) {
            for (InetSocketAddress boundAddress : boundAddresses) {
                InetAddress boundInetAddress = boundAddress.getAddress();
                if (boundInetAddress.isAnyLocalAddress() || boundInetAddress.equals(publishInetAddress)) {
                    publishPort = boundAddress.getPort();
                    break;
                }
            }
        }

        // if no matching boundAddress found, check if there is a unique port for all bound addresses
        if (publishPort < 0) {
            final IntSet ports = new IntHashSet();
            for (InetSocketAddress boundAddress : boundAddresses) {
                ports.add(boundAddress.getPort());
            }
            if (ports.size() == 1) {
                publishPort = ports.iterator().next().value;
            }
        }

        if (publishPort < 0) {
            String profileExplanation = profileSettings.isDefaultProfile ? "" : " for profile " + profileSettings.profileName;
            throw new BindTransportException("Failed to auto-resolve publish port" + profileExplanation + ", multiple bound addresses " +
                boundAddresses + " with distinct ports and none of them matched the publish address (" + publishInetAddress + "). " +
                "Please specify a unique port by setting " + TransportSettings.PORT.getKey() + " or " +
                TransportSettings.PUBLISH_PORT.getKey());
        }
        return publishPort;
    }

    @Override
    public TransportAddress[] addressesFromString(String address, int perAddressLimit) throws UnknownHostException {
        return parse(address, settings.get("transport.profiles.default.port", TransportSettings.PORT.get(settings)), perAddressLimit);
    }

    // this code is a take on guava's HostAndPort, like a HostAndPortRange

    // pattern for validating ipv6 bracket addresses.
    // not perfect, but PortsRange should take care of any port range validation, not a regex
    private static final Pattern BRACKET_PATTERN = Pattern.compile("^\\[(.*:.*)\\](?::([\\d\\-]*))?$");

    /**
     * parse a hostname+port range spec into its equivalent addresses
     */
    static TransportAddress[] parse(String hostPortString, String defaultPortRange, int perAddressLimit) throws UnknownHostException {
        Objects.requireNonNull(hostPortString);
        String host;
        String portString = null;

        if (hostPortString.startsWith("[")) {
            // Parse a bracketed host, typically an IPv6 literal.
            Matcher matcher = BRACKET_PATTERN.matcher(hostPortString);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid bracketed host/port range: " + hostPortString);
            }
            host = matcher.group(1);
            portString = matcher.group(2);  // could be null
        } else {
            int colonPos = hostPortString.indexOf(':');
            if (colonPos >= 0 && hostPortString.indexOf(':', colonPos + 1) == -1) {
                // Exactly 1 colon.  Split into host:port.
                host = hostPortString.substring(0, colonPos);
                portString = hostPortString.substring(colonPos + 1);
            } else {
                // 0 or 2+ colons.  Bare hostname or IPv6 literal.
                host = hostPortString;
                // 2+ colons and not bracketed: exception
                if (colonPos >= 0) {
                    throw new IllegalArgumentException("IPv6 addresses must be bracketed: " + hostPortString);
                }
            }
        }

        // if port isn't specified, fill with the default
        if (portString == null || portString.isEmpty()) {
            portString = defaultPortRange;
        }

        // generate address for each port in the range
        Set<InetAddress> addresses = new HashSet<>(Arrays.asList(InetAddress.getAllByName(host)));
        List<TransportAddress> transportAddresses = new ArrayList<>();
        int[] ports = new PortsRange(portString).ports();
        int limit = Math.min(ports.length, perAddressLimit);
        for (int i = 0; i < limit; i++) {
            for (InetAddress address : addresses) {
                transportAddresses.add(new TransportAddress(address, ports[i]));
            }
        }
        return transportAddresses.toArray(new TransportAddress[transportAddresses.size()]);
    }

    @Override
    protected final void doClose() {
    }

    @Override
    protected final void doStop() {
        final CountDownLatch latch = new CountDownLatch(1);
        // make sure we run it on another thread than a possible IO handler thread
        assert threadPool.generic().isShutdown() == false : "Must stop transport before terminating underlying threadpool";
        threadPool.generic().execute(() -> {
            closeLock.writeLock().lock();
            try {
                keepAlive.close();

                // first stop to accept any incoming connections so nobody can connect to this transport
                for (Map.Entry<String, List<TcpServerChannel>> entry : serverChannels.entrySet()) {
                    String profile = entry.getKey();
                    List<TcpServerChannel> channels = entry.getValue();
                    ActionListener<Void> closeFailLogger = ActionListener.wrap(c -> {
                        },
                        e -> logger.warn(() -> new ParameterizedMessage("Error closing serverChannel for profile [{}]", profile), e));
                    channels.forEach(c -> c.addCloseListener(closeFailLogger));
                    CloseableChannel.closeChannels(channels, true);
                }
                serverChannels.clear();

                // close all of the incoming channels. The closeChannels method takes a list so we must convert the set.
                CloseableChannel.closeChannels(new ArrayList<>(acceptedChannels), true);
                acceptedChannels.clear();

                stopInternal();
            } finally {
                closeLock.writeLock().unlock();
                latch.countDown();
            }
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // ignore
        }
    }

    public void onException(TcpChannel channel, Exception e) {
        if (!lifecycle.started()) {
            // just close and ignore - we are already stopped and just need to make sure we release all resources
            CloseableChannel.closeChannel(channel);
            return;
        }

        if (isCloseConnectionException(e)) {
            logger.trace(() -> new ParameterizedMessage(
                "close connection exception caught on transport layer [{}], disconnecting from relevant node", channel), e);
            // close the channel, which will cause a node to be disconnected if relevant
            CloseableChannel.closeChannel(channel);
        } else if (isConnectException(e)) {
            logger.trace(() -> new ParameterizedMessage("connect exception caught on transport layer [{}]", channel), e);
            // close the channel as safe measure, which will cause a node to be disconnected if relevant
            CloseableChannel.closeChannel(channel);
        } else if (e instanceof BindException) {
            logger.trace(() -> new ParameterizedMessage("bind exception caught on transport layer [{}]", channel), e);
            // close the channel as safe measure, which will cause a node to be disconnected if relevant
            CloseableChannel.closeChannel(channel);
        } else if (e instanceof CancelledKeyException) {
            logger.trace(() -> new ParameterizedMessage(
                "cancelled key exception caught on transport layer [{}], disconnecting from relevant node", channel), e);
            // close the channel as safe measure, which will cause a node to be disconnected if relevant
            CloseableChannel.closeChannel(channel);
        } else if (e instanceof TcpTransport.HttpOnTransportException) {
            // in case we are able to return data, serialize the exception content and sent it back to the client
            if (channel.isOpen()) {
                BytesArray message = new BytesArray(e.getMessage().getBytes(StandardCharsets.UTF_8));
                outboundHandler.sendBytes(channel, message, ActionListener.wrap(() -> CloseableChannel.closeChannel(channel)));
            }
        } else {
            logger.warn(() -> new ParameterizedMessage("exception caught on transport layer [{}], closing connection", channel), e);
            // close the channel, which will cause a node to be disconnected if relevant
            CloseableChannel.closeChannel(channel);
        }
    }

    protected void onServerException(TcpServerChannel channel, Exception e) {
        logger.error(new ParameterizedMessage("exception from server channel caught on transport layer [channel={}]", channel), e);
    }

    protected void serverAcceptedChannel(TcpChannel channel) {
        boolean addedOnThisCall = acceptedChannels.add(channel);
        assert addedOnThisCall : "Channel should only be added to accepted channel set once";
        // Mark the channel init time
        channel.getChannelStats().markAccessed(threadPool.relativeTimeInMillis());
        channel.addCloseListener(ActionListener.wrap(() -> acceptedChannels.remove(channel)));
        logger.trace(() -> new ParameterizedMessage("Tcp transport channel accepted: {}", channel));
    }

    /**
     * Binds to the given {@link InetSocketAddress}
     *
     * @param name    the profile name
     * @param address the address to bind to
     */
    protected abstract TcpServerChannel bind(String name, InetSocketAddress address) throws IOException;

    /**
     * Initiate a single tcp socket channel.
     *
     * @param node for the initiated connection
     * @return the pending connection
     * @throws IOException if an I/O exception occurs while opening the channel
     */
    protected abstract TcpChannel initiateChannel(DiscoveryNode node) throws IOException;

    /**
     * Called to tear down internal resources
     */
    protected abstract void stopInternal();

    private void sendRequestToChannel(final DiscoveryNode node, final TcpChannel channel, final long requestId, final String action,
                                      final TransportRequest request, TransportRequestOptions options, Version channelVersion,
                                      boolean compressRequest) throws IOException, TransportException {
        sendRequestToChannel(node, channel, requestId, action, request, options, channelVersion, compressRequest, false);
    }

    private void sendRequestToChannel(final DiscoveryNode node, final TcpChannel channel, final long requestId, final String action,
                                      final TransportRequest request, TransportRequestOptions options, Version channelVersion,
                                      boolean compressRequest, boolean isHandshake) throws IOException, TransportException {
        Version version = Version.min(this.version, channelVersion);
        OutboundMessage.Request message = new OutboundMessage.Request(threadPool.getThreadContext(), features, request, version, action,
            requestId, isHandshake, compressRequest);
        ActionListener<Void> listener = ActionListener.wrap(() ->
            messageListener.onRequestSent(node, requestId, action, request, options));
        outboundHandler.sendMessage(channel, message, listener);
    }

    /**
     * Sends back an error response to the caller via the given channel
     *
     * @param nodeVersion the caller node version
     * @param features    the caller features
     * @param channel     the channel to send the response to
     * @param error       the error to return
     * @param requestId   the request ID this response replies to
     * @param action      the action this response replies to
     */
    public void sendErrorResponse(
        final Version nodeVersion,
        final Set<String> features,
        final TcpChannel channel,
        final Exception error,
        final long requestId,
        final String action) throws IOException {
        Version version = Version.min(this.version, nodeVersion);
        TransportAddress address = new TransportAddress(channel.getLocalAddress());
        RemoteTransportException tx = new RemoteTransportException(nodeName, address, action, error);
        OutboundMessage.Response message = new OutboundMessage.Response(threadPool.getThreadContext(), features, tx, version, requestId,
            false, false);
        ActionListener<Void> listener = ActionListener.wrap(() -> messageListener.onResponseSent(requestId, action, error));
        outboundHandler.sendMessage(channel, message, listener);
    }

    /**
     * Sends the response to the given channel. This method should be used to send {@link TransportResponse} objects back to the caller.
     *
     * @see #sendErrorResponse(Version, Set, TcpChannel, Exception, long, String) for sending back errors to the caller
     */
    public void sendResponse(
        final Version nodeVersion,
        final Set<String> features,
        final TcpChannel channel,
        final TransportResponse response,
        final long requestId,
        final String action,
        final boolean compress) throws IOException {
        sendResponse(nodeVersion, features, channel, response, requestId, action, compress, false);
    }

    private void sendResponse(
        final Version nodeVersion,
        final Set<String> features,
        final TcpChannel channel,
        final TransportResponse response,
        final long requestId,
        final String action,
        boolean compress,
        boolean isHandshake) throws IOException {
        Version version = Version.min(this.version, nodeVersion);
        OutboundMessage.Response message = new OutboundMessage.Response(threadPool.getThreadContext(), features, response, version,
            requestId, isHandshake, compress);
        ActionListener<Void> listener = ActionListener.wrap(() -> messageListener.onResponseSent(requestId, action, response));
        outboundHandler.sendMessage(channel, message, listener);
    }

    /**
     * Handles inbound message that has been decoded.
     *
     * @param channel the channel the message is from
     * @param message the message
     */
    public void inboundMessage(TcpChannel channel, BytesReference message) {
        try {
            channel.getChannelStats().markAccessed(threadPool.relativeTimeInMillis());
            transportLogger.logInboundMessage(channel, message);
            // Message length of 0 is a ping
            if (message.length() != 0) {
                messageReceived(message, channel);
            } else {
                keepAlive.receiveKeepAlive(channel);
            }
        } catch (Exception e) {
            onException(channel, e);
        }
    }

    /**
     * Consumes bytes that are available from network reads. This method returns the number of bytes consumed
     * in this call.
     *
     * @param channel        the channel read from
     * @param bytesReference the bytes available to consume
     * @return the number of bytes consumed
     * @throws StreamCorruptedException              if the message header format is not recognized
     * @throws TcpTransport.HttpOnTransportException if the message header appears to be an HTTP message
     * @throws IllegalArgumentException              if the message length is greater that the maximum allowed frame size.
     *                                               This is dependent on the available memory.
     */
    public int consumeNetworkReads(TcpChannel channel, BytesReference bytesReference) throws IOException {
        BytesReference message = decodeFrame(bytesReference);

        if (message == null) {
            return 0;
        } else {
            inboundMessage(channel, message);
            return message.length() + BYTES_NEEDED_FOR_MESSAGE_SIZE;
        }
    }

    /**
     * Attempts to a decode a message from the provided bytes. If a full message is not available, null is
     * returned. If the message is a ping, an empty {@link BytesReference} will be returned.
     *
     * @param networkBytes the will be read
     * @return the message decoded
     * @throws StreamCorruptedException              if the message header format is not recognized
     * @throws TcpTransport.HttpOnTransportException if the message header appears to be an HTTP message
     * @throws IllegalArgumentException              if the message length is greater that the maximum allowed frame size.
     *                                               This is dependent on the available memory.
     */
    static BytesReference decodeFrame(BytesReference networkBytes) throws IOException {
        int messageLength = readMessageLength(networkBytes);
        if (messageLength == -1) {
            return null;
        } else {
            int totalLength = messageLength + BYTES_NEEDED_FOR_MESSAGE_SIZE;
            if (totalLength > networkBytes.length()) {
                return null;
            } else if (totalLength == 6) {
                return EMPTY_BYTES_REFERENCE;
            } else {
                return networkBytes.slice(BYTES_NEEDED_FOR_MESSAGE_SIZE, messageLength);
            }
        }
    }

    /**
     * Validates the first 6 bytes of the message header and returns the length of the message. If 6 bytes
     * are not available, it returns -1.
     *
     * @param networkBytes the will be read
     * @return the length of the message
     * @throws StreamCorruptedException              if the message header format is not recognized
     * @throws TcpTransport.HttpOnTransportException if the message header appears to be an HTTP message
     * @throws IllegalArgumentException              if the message length is greater that the maximum allowed frame size.
     *                                               This is dependent on the available memory.
     */
    public static int readMessageLength(BytesReference networkBytes) throws IOException {
        if (networkBytes.length() < BYTES_NEEDED_FOR_MESSAGE_SIZE) {
            return -1;
        } else {
            return readHeaderBuffer(networkBytes);
        }
    }

    private static int readHeaderBuffer(BytesReference headerBuffer) throws IOException {
        if (headerBuffer.get(0) != 'E' || headerBuffer.get(1) != 'S') {
            if (appearsToBeHTTP(headerBuffer)) {
                throw new TcpTransport.HttpOnTransportException("This is not an HTTP port");
            }

            throw new StreamCorruptedException("invalid internal transport message format, got ("
                + Integer.toHexString(headerBuffer.get(0) & 0xFF) + ","
                + Integer.toHexString(headerBuffer.get(1) & 0xFF) + ","
                + Integer.toHexString(headerBuffer.get(2) & 0xFF) + ","
                + Integer.toHexString(headerBuffer.get(3) & 0xFF) + ")");
        }
        final int messageLength = headerBuffer.getInt(TcpHeader.MARKER_BYTES_SIZE);

        if (messageLength == TransportKeepAlive.PING_DATA_SIZE) {
            // This is a ping
            return 0;
        }

        if (messageLength <= 0) {
            throw new StreamCorruptedException("invalid data length: " + messageLength);
        }

        if (messageLength > NINETY_PER_HEAP_SIZE) {
            throw new IllegalArgumentException("transport content length received [" + new ByteSizeValue(messageLength) + "] exceeded ["
                + new ByteSizeValue(NINETY_PER_HEAP_SIZE) + "]");
        }

        return messageLength;
    }

    private static boolean appearsToBeHTTP(BytesReference headerBuffer) {
        return bufferStartsWith(headerBuffer, "GET") ||
            bufferStartsWith(headerBuffer, "POST") ||
            bufferStartsWith(headerBuffer, "PUT") ||
            bufferStartsWith(headerBuffer, "HEAD") ||
            bufferStartsWith(headerBuffer, "DELETE") ||
            // Actually 'OPTIONS'. But we are only guaranteed to have read six bytes at this point.
            bufferStartsWith(headerBuffer, "OPTION") ||
            bufferStartsWith(headerBuffer, "PATCH") ||
            bufferStartsWith(headerBuffer, "TRACE");
    }

    private static boolean bufferStartsWith(BytesReference buffer, String method) {
        char[] chars = method.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (buffer.get(i) != chars[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * A helper exception to mark an incoming connection as potentially being HTTP
     * so an appropriate error code can be returned
     */
    public static class HttpOnTransportException extends ElasticsearchException {

        private HttpOnTransportException(String msg) {
            super(msg);
        }

        @Override
        public RestStatus status() {
            return RestStatus.BAD_REQUEST;
        }

        public HttpOnTransportException(StreamInput in) throws IOException {
            super(in);
        }
    }

    /**
     * This method handles the message receive part for both request and responses
     */
    public final void messageReceived(BytesReference reference, TcpChannel channel) throws IOException {
        readBytesMetric.inc(reference.length() + TcpHeader.MARKER_BYTES_SIZE + TcpHeader.MESSAGE_LENGTH_SIZE);
        InetSocketAddress remoteAddress = channel.getRemoteAddress();

        ThreadContext threadContext = threadPool.getThreadContext();
        try (ThreadContext.StoredContext existing = threadContext.stashContext();
             InboundMessage message = reader.deserialize(reference)) {
            // Place the context with the headers from the message
            message.getStoredContext().restore();
            threadContext.putTransient("_remote_address", remoteAddress);
            if (message.isRequest()) {
                handleRequest(channel, (InboundMessage.RequestMessage) message, reference.length());
            } else {
                final TransportResponseHandler<?> handler;
                long requestId = message.getRequestId();
                if (message.isHandshake()) {
                    handler = handshaker.removeHandlerForHandshake(requestId);
                } else {
                    TransportResponseHandler<? extends TransportResponse> theHandler =
                        responseHandlers.onResponseReceived(requestId, messageListener);
                    if (theHandler == null && message.isError()) {
                        handler = handshaker.removeHandlerForHandshake(requestId);
                    } else {
                        handler = theHandler;
                    }
                }
                // ignore if its null, the service logs it
                if (handler != null) {
                    if (message.isError()) {
                        handlerResponseError(message.getStreamInput(), handler);
                    } else {
                        handleResponse(remoteAddress, message.getStreamInput(), handler);
                    }
                    // Check the entire message has been read
                    final int nextByte = message.getStreamInput().read();
                    // calling read() is useful to make sure the message is fully read, even if there is an EOS marker
                    if (nextByte != -1) {
                        throw new IllegalStateException("Message not fully read (response) for requestId [" + requestId + "], handler ["
                            + handler + "], error [" + message.isError() + "]; resetting");
                    }
                }
            }
        }
    }

    private <T extends TransportResponse> void handleResponse(InetSocketAddress remoteAddress, final StreamInput stream,
                                                              final TransportResponseHandler<T> handler) {
        final T response;
        try {
            response = handler.read(stream);
            response.remoteAddress(new TransportAddress(remoteAddress));
        } catch (Exception e) {
            handleException(handler, new TransportSerializationException(
                "Failed to deserialize response from handler [" + handler.getClass().getName() + "]", e));
            return;
        }
        threadPool.executor(handler.executor()).execute(new AbstractRunnable() {
            @Override
            public void onFailure(Exception e) {
                handleException(handler, new ResponseHandlerFailureTransportException(e));
            }

            @Override
            protected void doRun() throws Exception {
                handler.handleResponse(response);
            }
        });

    }

    /**
     * Executed for a received response error
     */
    private void handlerResponseError(StreamInput stream, final TransportResponseHandler handler) {
        Exception error;
        try {
            error = stream.readException();
        } catch (Exception e) {
            error = new TransportSerializationException("Failed to deserialize exception response from stream", e);
        }
        handleException(handler, error);
    }

    private void handleException(final TransportResponseHandler handler, Throwable error) {
        if (!(error instanceof RemoteTransportException)) {
            error = new RemoteTransportException(error.getMessage(), error);
        }
        final RemoteTransportException rtx = (RemoteTransportException) error;
        threadPool.executor(handler.executor()).execute(() -> {
            try {
                handler.handleException(rtx);
            } catch (Exception e) {
                logger.error(() -> new ParameterizedMessage("failed to handle exception response [{}]", handler), e);
            }
        });
    }

    protected void handleRequest(TcpChannel channel, InboundMessage.RequestMessage message, int messageLengthBytes) throws IOException {
        final Set<String> features = message.getFeatures();
        final String profileName = channel.getProfile();
        final String action = message.getActionName();
        final long requestId = message.getRequestId();
        final StreamInput stream = message.getStreamInput();
        final Version version = message.getVersion();
        messageListener.onRequestReceived(requestId, action);
        TransportChannel transportChannel = null;
        try {
            if (message.isHandshake()) {
                handshaker.handleHandshake(version, features, channel, requestId, stream);
            } else {
                final RequestHandlerRegistry reg = getRequestHandler(action);
                if (reg == null) {
                    throw new ActionNotFoundTransportException(action);
                }
                if (reg.canTripCircuitBreaker()) {
                    getInFlightRequestBreaker().addEstimateBytesAndMaybeBreak(messageLengthBytes, "<transport_request>");
                } else {
                    getInFlightRequestBreaker().addWithoutBreaking(messageLengthBytes);
                }
                transportChannel = new TcpTransportChannel(this, channel, transportName, action, requestId, version, features, profileName,
                    messageLengthBytes, message.isCompress());
                final TransportRequest request = reg.newRequest(stream);
                request.remoteAddress(new TransportAddress(channel.getRemoteAddress()));
                // in case we throw an exception, i.e. when the limit is hit, we don't want to verify
                validateRequest(stream, requestId, action);
                threadPool.executor(reg.getExecutor()).execute(new RequestHandler(reg, request, transportChannel));
            }
        } catch (Exception e) {
            // the circuit breaker tripped
            if (transportChannel == null) {
                transportChannel = new TcpTransportChannel(this, channel, transportName, action, requestId, version, features,
                    profileName, 0, message.isCompress());
            }
            try {
                transportChannel.sendResponse(e);
            } catch (IOException inner) {
                inner.addSuppressed(e);
                logger.warn(() -> new ParameterizedMessage("Failed to send error message back to client for action [{}]", action), inner);
            }
        }
    }

    // This template method is needed to inject custom error checking logic in tests.
    protected void validateRequest(StreamInput stream, long requestId, String action) throws IOException {
        final int nextByte = stream.read();
        // calling read() is useful to make sure the message is fully read, even if there some kind of EOS marker
        if (nextByte != -1) {
            throw new IllegalStateException("Message not fully read (request) for requestId [" + requestId + "], action [" + action
                + "], available [" + stream.available() + "]; resetting");
        }
    }

    class RequestHandler extends AbstractRunnable {
        private final RequestHandlerRegistry reg;
        private final TransportRequest request;
        private final TransportChannel transportChannel;

        RequestHandler(RequestHandlerRegistry reg, TransportRequest request, TransportChannel transportChannel) {
            this.reg = reg;
            this.request = request;
            this.transportChannel = transportChannel;
        }

        @SuppressWarnings({"unchecked"})
        @Override
        protected void doRun() throws Exception {
            reg.processMessageReceived(request, transportChannel);
        }

        @Override
        public boolean isForceExecution() {
            return reg.isForceExecution();
        }

        @Override
        public void onFailure(Exception e) {
            if (lifecycleState() == Lifecycle.State.STARTED) {
                // we can only send a response transport is started....
                try {
                    transportChannel.sendResponse(e);
                } catch (Exception inner) {
                    inner.addSuppressed(e);
                    logger.warn(() -> new ParameterizedMessage(
                        "Failed to send error message back to client for action [{}]", reg.getAction()), inner);
                }
            }
        }
    }

    public void executeHandshake(DiscoveryNode node, TcpChannel channel, ConnectionProfile profile, ActionListener<Version> listener) {
        handshaker.sendHandshake(responseHandlers.newRequestId(), node, channel, profile.getHandshakeTimeout(), listener);
    }

    final TransportKeepAlive getKeepAlive() {
        return keepAlive;
    }

    final int getNumPendingHandshakes() {
        return handshaker.getNumPendingHandshakes();
    }

    final long getNumHandshakes() {
        return handshaker.getNumHandshakes();
    }

    final Set<TcpChannel> getAcceptedChannels() {
        return Collections.unmodifiableSet(acceptedChannels);
    }

    /**
     * Ensures this transport is still started / open
     *
     * @throws IllegalStateException if the transport is not started / open
     */
    protected final void ensureOpen() {
        if (lifecycle.started() == false) {
            throw new IllegalStateException("transport has been stopped");
        }
    }

    @Override
    public final TransportStats getStats() {
        MeanMetric transmittedBytes = outboundHandler.getTransmittedBytes();
        return new TransportStats(acceptedChannels.size(), readBytesMetric.count(), readBytesMetric.sum(), transmittedBytes.count(),
            transmittedBytes.sum());
    }

    /**
     * Returns all profile settings for the given settings object
     */
    public static Set<ProfileSettings> getProfileSettings(Settings settings) {
        HashSet<ProfileSettings> profiles = new HashSet<>();
        boolean isDefaultSet = false;
        for (String profile : settings.getGroups("transport.profiles.", true).keySet()) {
            profiles.add(new ProfileSettings(settings, profile));
            if (TransportSettings.DEFAULT_PROFILE.equals(profile)) {
                isDefaultSet = true;
            }
        }
        if (isDefaultSet == false) {
            profiles.add(new ProfileSettings(settings, TransportSettings.DEFAULT_PROFILE));
        }
        return Collections.unmodifiableSet(profiles);
    }

    /**
     * Representation of a transport profile settings for a {@code transport.profiles.$profilename.*}
     */
    public static final class ProfileSettings {
        public final String profileName;
        public final boolean tcpNoDelay;
        public final boolean tcpKeepAlive;
        public final boolean reuseAddress;
        public final ByteSizeValue sendBufferSize;
        public final ByteSizeValue receiveBufferSize;
        public final List<String> bindHosts;
        public final List<String> publishHosts;
        public final String portOrRange;
        public final int publishPort;
        public final boolean isDefaultProfile;

        public ProfileSettings(Settings settings, String profileName) {
            this.profileName = profileName;
            isDefaultProfile = TransportSettings.DEFAULT_PROFILE.equals(profileName);
            tcpKeepAlive = TransportSettings.TCP_KEEP_ALIVE_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
            tcpNoDelay = TransportSettings.TCP_NO_DELAY_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
            reuseAddress = TransportSettings.TCP_REUSE_ADDRESS_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
            sendBufferSize = TransportSettings.TCP_SEND_BUFFER_SIZE_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
            receiveBufferSize = TransportSettings.TCP_RECEIVE_BUFFER_SIZE_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
            List<String> profileBindHosts = TransportSettings.BIND_HOST_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
            bindHosts = (profileBindHosts.isEmpty() ? NetworkService.GLOBAL_NETWORK_BIND_HOST_SETTING.get(settings) : profileBindHosts);
            publishHosts = TransportSettings.PUBLISH_HOST_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
            Setting<String> concretePort = TransportSettings.PORT_PROFILE.getConcreteSettingForNamespace(profileName);
            if (concretePort.exists(settings) == false && isDefaultProfile == false) {
                throw new IllegalStateException("profile [" + profileName + "] has no port configured");
            }
            portOrRange = TransportSettings.PORT_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
            publishPort = isDefaultProfile ? TransportSettings.PUBLISH_PORT.get(settings) :
                TransportSettings.PUBLISH_PORT_PROFILE.getConcreteSettingForNamespace(profileName).get(settings);
        }
    }

    private static final class DelegatingTransportMessageListener implements TransportMessageListener {

        private final List<TransportMessageListener> listeners = new CopyOnWriteArrayList<>();

        @Override
        public void onRequestReceived(long requestId, String action) {
            for (TransportMessageListener listener : listeners) {
                listener.onRequestReceived(requestId, action);
            }
        }

        @Override
        public void onResponseSent(long requestId, String action, TransportResponse response) {
            for (TransportMessageListener listener : listeners) {
                listener.onResponseSent(requestId, action, response);
            }
        }

        @Override
        public void onResponseSent(long requestId, String action, Exception error) {
            for (TransportMessageListener listener : listeners) {
                listener.onResponseSent(requestId, action, error);
            }
        }

        @Override
        public void onRequestSent(DiscoveryNode node, long requestId, String action, TransportRequest request,
                                  TransportRequestOptions finalOptions) {
            for (TransportMessageListener listener : listeners) {
                listener.onRequestSent(node, requestId, action, request, finalOptions);
            }
        }

        @Override
        public void onResponseReceived(long requestId, ResponseContext holder) {
            for (TransportMessageListener listener : listeners) {
                listener.onResponseReceived(requestId, holder);
            }
        }
    }

    @Override
    public final ResponseHandlers getResponseHandlers() {
        return responseHandlers;
    }

    @Override
    public final RequestHandlerRegistry<? extends TransportRequest> getRequestHandler(String action) {
        return requestHandlers.get(action);
    }

    private final class ChannelsConnectedListener implements ActionListener<Void> {

        private final DiscoveryNode node;
        private final ConnectionProfile connectionProfile;
        private final List<TcpChannel> channels;
        private final ActionListener<Transport.Connection> listener;
        private final CountDown countDown;

        private ChannelsConnectedListener(DiscoveryNode node, ConnectionProfile connectionProfile, List<TcpChannel> channels,
                                          ActionListener<Transport.Connection> listener) {
            this.node = node;
            this.connectionProfile = connectionProfile;
            this.channels = channels;
            this.listener = listener;
            this.countDown = new CountDown(channels.size());
        }

        @Override
        public void onResponse(Void v) {
            // Returns true if all connections have completed successfully
            if (countDown.countDown()) {
                final TcpChannel handshakeChannel = channels.get(0);
                try {
                    executeHandshake(node, handshakeChannel, connectionProfile, new ActionListener<Version>() {
                        @Override
                        public void onResponse(Version version) {
                            NodeChannels nodeChannels = new NodeChannels(node, channels, connectionProfile, version);
                            long relativeMillisTime = threadPool.relativeTimeInMillis();
                            nodeChannels.channels.forEach(ch -> {
                                // Mark the channel init time
                                ch.getChannelStats().markAccessed(relativeMillisTime);
                                ch.addCloseListener(ActionListener.wrap(nodeChannels::close));
                            });
                            keepAlive.registerNodeConnection(nodeChannels.channels, connectionProfile);
                            listener.onResponse(nodeChannels);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            CloseableChannel.closeChannels(channels, false);

                            if (e instanceof ConnectTransportException) {
                                listener.onFailure(e);
                            } else {
                                listener.onFailure(new ConnectTransportException(node, "general node connection failure", e));
                            }
                        }
                    });
                } catch (Exception ex) {
                    CloseableChannel.closeChannels(channels, false);
                    listener.onFailure(ex);
                }
            }
        }

        @Override
        public void onFailure(Exception ex) {
            if (countDown.fastForward()) {
                CloseableChannel.closeChannels(channels, false);
                listener.onFailure(new ConnectTransportException(node, "connect_exception", ex));
            }
        }

        public void onTimeout() {
            if (countDown.fastForward()) {
                CloseableChannel.closeChannels(channels, false);
                listener.onFailure(new ConnectTransportException(node, "connect_timeout[" + connectionProfile.getConnectTimeout() + "]"));
            }
        }
    }
}
