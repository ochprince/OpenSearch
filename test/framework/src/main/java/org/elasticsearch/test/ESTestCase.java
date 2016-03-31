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
package org.elasticsearch.test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Listeners;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope.Scope;
import com.carrotsearch.randomizedtesting.annotations.TimeoutSuite;
import com.carrotsearch.randomizedtesting.generators.RandomInts;
import com.carrotsearch.randomizedtesting.generators.RandomPicks;
import com.carrotsearch.randomizedtesting.generators.RandomStrings;
import com.carrotsearch.randomizedtesting.rules.TestRuleAdapter;

import org.apache.lucene.uninverting.UninvertingReader;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.LuceneTestCase.SuppressCodecs;
import org.apache.lucene.util.TestRuleMarkFailure;
import org.apache.lucene.util.TestUtil;
import org.apache.lucene.util.TimeUnits;
import org.elasticsearch.Version;
import org.elasticsearch.bootstrap.BootstrapForTesting;
import org.elasticsearch.cache.recycler.MockPageCacheRecycler;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.io.PathUtilsForTesting;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.common.util.MockBigArrays;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.analysis.AnalysisService;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.search.MockSearchService;
import org.elasticsearch.test.junit.listeners.LoggingListener;
import org.elasticsearch.test.junit.listeners.ReproduceInfoPrinter;
import org.elasticsearch.threadpool.ThreadPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.RuleChain;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.elasticsearch.common.util.CollectionUtils.arrayAsArrayList;
import static org.hamcrest.Matchers.equalTo;

/**
 * Base testcase for randomized unit testing with Elasticsearch
 */
@Listeners({
        ReproduceInfoPrinter.class,
        LoggingListener.class
})
@ThreadLeakScope(Scope.SUITE)
@ThreadLeakLingering(linger = 5000) // 5 sec lingering
@TimeoutSuite(millis = 20 * TimeUnits.MINUTE)
@LuceneTestCase.SuppressSysoutChecks(bugUrl = "we log a lot on purpose")
// we suppress pretty much all the lucene codecs for now, except asserting
// assertingcodec is the winner for a codec here: it finds bugs and gives clear exceptions.
@SuppressCodecs({
        "SimpleText", "Memory", "CheapBastard", "Direct", "Compressing", "FST50", "FSTOrd50",
        "TestBloomFilteredLucenePostings", "MockRandom", "BlockTreeOrds", "LuceneFixedGap",
        "LuceneVarGapFixedInterval", "LuceneVarGapDocFreqInterval", "Lucene50"
})
@LuceneTestCase.SuppressReproduceLine
public abstract class ESTestCase extends LuceneTestCase {

    static {
        BootstrapForTesting.ensureInitialized();
    }

    protected final ESLogger logger = Loggers.getLogger(getClass());

    // -----------------------------------------------------------------
    // Suite and test case setup/cleanup.
    // -----------------------------------------------------------------

    @Rule
    public RuleChain failureAndSuccessEvents = RuleChain.outerRule(new TestRuleAdapter() {
        @Override
        protected void afterIfSuccessful() throws Throwable {
            ESTestCase.this.afterIfSuccessful();
        }

        @Override
        protected void afterAlways(List<Throwable> errors) throws Throwable {
            if (errors != null && errors.isEmpty() == false) {
                ESTestCase.this.afterIfFailed(errors);
            }
            super.afterAlways(errors);
        }
    });

    /** called when a test fails, supplying the errors it generated */
    protected void afterIfFailed(List<Throwable> errors) {
    }

    /** called after a test is finished, but only if successful */
    protected void afterIfSuccessful() throws Exception {
    }

    // setup mock filesystems for this test run. we change PathUtils
    // so that all accesses are plumbed thru any mock wrappers

    @BeforeClass
    public static void setFileSystem() throws Exception {
        PathUtilsForTesting.setup();
    }

    @AfterClass
    public static void restoreFileSystem() throws Exception {
        PathUtilsForTesting.teardown();
    }

    // randomize content type for request builders

    @BeforeClass
    public static void setContentType() throws Exception {
        Requests.CONTENT_TYPE = randomFrom(XContentType.values());
        Requests.INDEX_CONTENT_TYPE = randomFrom(XContentType.values());
    }

    @AfterClass
    public static void restoreContentType() {
        Requests.CONTENT_TYPE = XContentType.SMILE;
        Requests.INDEX_CONTENT_TYPE = XContentType.JSON;
    }

    @After
    public final void ensureCleanedUp() throws Exception {
        MockPageCacheRecycler.ensureAllPagesAreReleased();
        MockBigArrays.ensureAllArraysAreReleased();
        // field cache should NEVER get loaded.
        String[] entries = UninvertingReader.getUninvertedStats();
        assertEquals("fieldcache must never be used, got=" + Arrays.toString(entries), 0, entries.length);
    }

    // this must be a separate method from other ensure checks above so suite scoped integ tests can call...TODO: fix that
    @After
    public final void ensureAllSearchContextsReleased() throws Exception {
        assertBusy(new Runnable() {
            @Override
            public void run() {
                MockSearchService.assertNoInFLightContext();
            }
        });
    }

    // mockdirectorywrappers currently set this boolean if checkindex fails
    // TODO: can we do this cleaner???

    /** MockFSDirectoryService sets this: */
    public static boolean checkIndexFailed;

    @Before
    public final void resetCheckIndexStatus() throws Exception {
        checkIndexFailed = false;
    }

    @After
    public final void ensureCheckIndexPassed() throws Exception {
        assertFalse("at least one shard failed CheckIndex", checkIndexFailed);
    }

    // -----------------------------------------------------------------
    // Test facilities and facades for subclasses.
    // -----------------------------------------------------------------

    // TODO: decide on one set of naming for between/scaledBetween and remove others
    // TODO: replace frequently() with usually()

    /**
     * Returns a "scaled" random number between min and max (inclusive).
     *
     * @see RandomizedTest#scaledRandomIntBetween(int, int)
     */
    public static int scaledRandomIntBetween(int min, int max) {
        return RandomizedTest.scaledRandomIntBetween(min, max);
    }

    /**
     * A random integer from <code>min</code> to <code>max</code> (inclusive).
     *
     * @see #scaledRandomIntBetween(int, int)
     */
    public static int randomIntBetween(int min, int max) {
        return RandomInts.randomIntBetween(random(), min, max);
    }

    /**
     * Returns a "scaled" number of iterations for loops which can have a variable
     * iteration count. This method is effectively
     * an alias to {@link #scaledRandomIntBetween(int, int)}.
     */
    public static int iterations(int min, int max) {
        return scaledRandomIntBetween(min, max);
    }

    /**
     * An alias for {@link #randomIntBetween(int, int)}.
     *
     * @see #scaledRandomIntBetween(int, int)
     */
    public static int between(int min, int max) {
        return randomIntBetween(min, max);
    }

    /**
     * The exact opposite of {@link #rarely()}.
     */
    public static boolean frequently() {
        return !rarely();
    }

    public static boolean randomBoolean() {
        return random().nextBoolean();
    }

    public static byte randomByte() {
        return (byte) random().nextInt();
    }

    public static short randomShort() {
        return (short) random().nextInt();
    }

    public static int randomInt() {
        return random().nextInt();
    }

    public static float randomFloat() {
        return random().nextFloat();
    }

    public static double randomDouble() {
        return random().nextDouble();
    }

    /**
     * Returns a double value in the interval [start, end) if lowerInclusive is
     * set to true, (start, end) otherwise.
     *
     * @param start          lower bound of interval to draw uniformly distributed random numbers from
     * @param end            upper bound
     * @param lowerInclusive whether or not to include lower end of the interval
     */
    public static double randomDoubleBetween(double start, double end, boolean lowerInclusive) {
        double result = 0.0;

        if (start == -Double.MAX_VALUE || end == Double.MAX_VALUE) {
            // formula below does not work with very large doubles
            result = Double.longBitsToDouble(randomLong());
            while (result < start || result > end || Double.isNaN(result)) {
                result = Double.longBitsToDouble(randomLong());
            }
        } else {
            result = randomDouble();
            if (lowerInclusive == false) {
                while (result <= 0.0) {
                    result = randomDouble();
                }
            }
            result = result * end + (1.0 - result) * start;
        }
        return result;
    }

    public static long randomLong() {
        return random().nextLong();
    }

    /** A random integer from 0..max (inclusive). */
    public static int randomInt(int max) {
        return RandomizedTest.randomInt(max);
    }

    /** Pick a random object from the given array. The array must not be empty. */
    public static <T> T randomFrom(T... array) {
        return RandomPicks.randomFrom(random(), array);
    }

    /** Pick a random object from the given list. */
    public static <T> T randomFrom(List<T> list) {
        return RandomPicks.randomFrom(random(), list);
    }

    public static String randomAsciiOfLengthBetween(int minCodeUnits, int maxCodeUnits) {
        return RandomizedTest.randomAsciiOfLengthBetween(minCodeUnits, maxCodeUnits);
    }

    public static String randomAsciiOfLength(int codeUnits) {
        return RandomizedTest.randomAsciiOfLength(codeUnits);
    }

    public static String randomUnicodeOfLengthBetween(int minCodeUnits, int maxCodeUnits) {
        return RandomizedTest.randomUnicodeOfLengthBetween(minCodeUnits, maxCodeUnits);
    }

    public static String randomUnicodeOfLength(int codeUnits) {
        return RandomizedTest.randomUnicodeOfLength(codeUnits);
    }

    public static String randomUnicodeOfCodepointLengthBetween(int minCodePoints, int maxCodePoints) {
        return RandomizedTest.randomUnicodeOfCodepointLengthBetween(minCodePoints, maxCodePoints);
    }

    public static String randomUnicodeOfCodepointLength(int codePoints) {
        return RandomizedTest.randomUnicodeOfCodepointLength(codePoints);
    }

    public static String randomRealisticUnicodeOfLengthBetween(int minCodeUnits, int maxCodeUnits) {
        return RandomizedTest.randomRealisticUnicodeOfLengthBetween(minCodeUnits, maxCodeUnits);
    }

    public static String randomRealisticUnicodeOfLength(int codeUnits) {
        return RandomizedTest.randomRealisticUnicodeOfLength(codeUnits);
    }

    public static String randomRealisticUnicodeOfCodepointLengthBetween(int minCodePoints, int maxCodePoints) {
        return RandomizedTest.randomRealisticUnicodeOfCodepointLengthBetween(minCodePoints, maxCodePoints);
    }

    public static String randomRealisticUnicodeOfCodepointLength(int codePoints) {
        return RandomizedTest.randomRealisticUnicodeOfCodepointLength(codePoints);
    }

    public static String[] generateRandomStringArray(int maxArraySize, int maxStringSize, boolean allowNull, boolean allowEmpty) {
        if (allowNull && random().nextBoolean()) {
            return null;
        }
        int arraySize = randomIntBetween(allowEmpty ? 0 : 1, maxArraySize);
        String[] array = new String[arraySize];
        for (int i = 0; i < arraySize; i++) {
            array[i] = RandomStrings.randomAsciiOfLength(random(), maxStringSize);
        }
        return array;
    }

    public static String[] generateRandomStringArray(int maxArraySize, int maxStringSize, boolean allowNull) {
        return generateRandomStringArray(maxArraySize, maxStringSize, allowNull, true);
    }

    private static String[] TIME_SUFFIXES = new String[]{"d", "H", "ms", "s", "S", "w"};

    private static String randomTimeValue(int lower, int upper) {
        return randomIntBetween(lower, upper) + randomFrom(TIME_SUFFIXES);
    }

    public static String randomTimeValue() {
        return randomTimeValue(0, 1000);
    }

    public static String randomPositiveTimeValue() {
        return randomTimeValue(1, 1000);
    }

    /**
     * helper to randomly perform on <code>consumer</code> with <code>value</code>
     */
    public static <T> void maybeSet(Consumer<T> consumer, T value) {
        if (randomBoolean()) {
            consumer.accept(value);
        }
    }

    /**
     * helper to get a random value in a certain range that's different from the input
     */
    public static <T> T randomValueOtherThan(T input, Supplier<T> randomSupplier) {
        T randomValue = null;
        do {
            randomValue = randomSupplier.get();
        } while (randomValue.equals(input));
        return randomValue;
    }

    /**
     * Runs the code block for 10 seconds waiting for no assertion to trip.
     */
    public static void assertBusy(Runnable codeBlock) throws Exception {
        assertBusy(Executors.callable(codeBlock), 10, TimeUnit.SECONDS);
    }

    public static void assertBusy(Runnable codeBlock, long maxWaitTime, TimeUnit unit) throws Exception {
        assertBusy(Executors.callable(codeBlock), maxWaitTime, unit);
    }

    /**
     * Runs the code block for 10 seconds waiting for no assertion to trip.
     */
    public static <V> V assertBusy(Callable<V> codeBlock) throws Exception {
        return assertBusy(codeBlock, 10, TimeUnit.SECONDS);
    }

    /**
     * Runs the code block for the provided interval, waiting for no assertions to trip.
     */
    public static <V> V assertBusy(Callable<V> codeBlock, long maxWaitTime, TimeUnit unit) throws Exception {
        long maxTimeInMillis = TimeUnit.MILLISECONDS.convert(maxWaitTime, unit);
        long iterations = Math.max(Math.round(Math.log10(maxTimeInMillis) / Math.log10(2)), 1);
        long timeInMillis = 1;
        long sum = 0;
        List<AssertionError> failures = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            try {
                return codeBlock.call();
            } catch (AssertionError e) {
                failures.add(e);
            }
            sum += timeInMillis;
            Thread.sleep(timeInMillis);
            timeInMillis *= 2;
        }
        timeInMillis = maxTimeInMillis - sum;
        Thread.sleep(Math.max(timeInMillis, 0));
        try {
            return codeBlock.call();
        } catch (AssertionError e) {
            for (AssertionError failure : failures) {
                e.addSuppressed(failure);
            }
            throw e;
        }
    }

    public static boolean awaitBusy(BooleanSupplier breakSupplier) throws InterruptedException {
        return awaitBusy(breakSupplier, 10, TimeUnit.SECONDS);
    }

    // After 1s, we stop growing the sleep interval exponentially and just sleep 1s until maxWaitTime
    private static final long AWAIT_BUSY_THRESHOLD = 1000L;

    public static boolean awaitBusy(BooleanSupplier breakSupplier, long maxWaitTime, TimeUnit unit) throws InterruptedException {
        long maxTimeInMillis = TimeUnit.MILLISECONDS.convert(maxWaitTime, unit);
        long timeInMillis = 1;
        long sum = 0;
        while (sum + timeInMillis < maxTimeInMillis) {
            if (breakSupplier.getAsBoolean()) {
                return true;
            }
            Thread.sleep(timeInMillis);
            sum += timeInMillis;
            timeInMillis = Math.min(AWAIT_BUSY_THRESHOLD, timeInMillis * 2);
        }
        timeInMillis = maxTimeInMillis - sum;
        Thread.sleep(Math.max(timeInMillis, 0));
        return breakSupplier.getAsBoolean();
    }

    public static boolean terminate(ExecutorService... services) throws InterruptedException {
        boolean terminated = true;
        for (ExecutorService service : services) {
            if (service != null) {
                terminated &= ThreadPool.terminate(service, 10, TimeUnit.SECONDS);
            }
        }
        return terminated;
    }

    public static boolean terminate(ThreadPool service) throws InterruptedException {
        return ThreadPool.terminate(service, 10, TimeUnit.SECONDS);
    }

    /**
     * Returns a {@link java.nio.file.Path} pointing to the class path relative resource given
     * as the first argument. In contrast to
     * <code>getClass().getResource(...).getFile()</code> this method will not
     * return URL encoded paths if the parent path contains spaces or other
     * non-standard characters.
     */
    @Override
    public Path getDataPath(String relativePath) {
        // we override LTC behavior here: wrap even resources with mockfilesystems,
        // because some code is buggy when it comes to multiple nio.2 filesystems
        // (e.g. FileSystemUtils, and likely some tests)
        try {
            return PathUtils.get(getClass().getResource(relativePath).toURI());
        } catch (Exception e) {
            throw new RuntimeException("resource not found: " + relativePath, e);
        }
    }

    public Path getBwcIndicesPath() {
        return getDataPath("/indices/bwc");
    }

    /** Returns a random number of temporary paths. */
    public String[] tmpPaths() {
        final int numPaths = TestUtil.nextInt(random(), 1, 3);
        final String[] absPaths = new String[numPaths];
        for (int i = 0; i < numPaths; i++) {
            absPaths[i] = createTempDir().toAbsolutePath().toString();
        }
        return absPaths;
    }

    public NodeEnvironment newNodeEnvironment() throws IOException {
        return newNodeEnvironment(Settings.EMPTY);
    }

    public NodeEnvironment newNodeEnvironment(Settings settings) throws IOException {
        Settings build = Settings.builder()
                .put(settings)
                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toAbsolutePath())
                .putArray(Environment.PATH_DATA_SETTING.getKey(), tmpPaths()).build();
        return new NodeEnvironment(build, new Environment(build));
    }

    /** Return consistent index settings for the provided index version. */
    public static Settings.Builder settings(Version version) {
        Settings.Builder builder = Settings.builder().put(IndexMetaData.SETTING_VERSION_CREATED, version);
        return builder;
    }

    private static String threadName(Thread t) {
        return "Thread[" +
                "id=" + t.getId() +
                ", name=" + t.getName() +
                ", state=" + t.getState() +
                ", group=" + groupName(t.getThreadGroup()) +
                "]";
    }

    private static String groupName(ThreadGroup threadGroup) {
        if (threadGroup == null) {
            return "{null group}";
        } else {
            return threadGroup.getName();
        }
    }

    /**
     * Returns size random values
     */
    public static <T> List<T> randomSubsetOf(int size, T... values) {
        List<T> list = arrayAsArrayList(values);
        return randomSubsetOf(size, list);
    }

    /**
     * Returns a random subset of values (including a potential empty list)
     */
    public static <T> List<T> randomSubsetOf(Collection<T> collection) {
        return randomSubsetOf(randomInt(collection.size() - 1), collection);
    }

    /**
     * Returns size random values
     */
    public static <T> List<T> randomSubsetOf(int size, Collection<T> collection) {
        if (size > collection.size()) {
            throw new IllegalArgumentException("Can\'t pick " + size + " random objects from a collection of " + collection.size() + " objects");
        }
        List<T> tempList = new ArrayList<>(collection);
        Collections.shuffle(tempList, random());
        return tempList.subList(0, size);
    }

    /**
     * Randomly shuffles the fields inside objects in the {@link XContentBuilder} passed in.
     * Recursively goes through inner objects and also shuffles them. Exceptions for this
     * recursive shuffling behavior can be made by passing in the names of fields which
     * internally should stay untouched.
     */
    public static XContentBuilder shuffleXContent(XContentBuilder builder, Set<String> exceptFieldNames) throws IOException {
        BytesReference bytes = builder.bytes();
        XContentParser parser = XContentFactory.xContent(bytes).createParser(bytes);
        // use ordered maps for reproducibility
        Map<String, Object> shuffledMap = shuffleMap(parser.mapOrdered(), exceptFieldNames);
        XContentBuilder jsonBuilder = XContentFactory.contentBuilder(builder.contentType());
        return jsonBuilder.map(shuffledMap);
    }

    private static Map<String, Object> shuffleMap(Map<String, Object> map, Set<String> exceptFieldNames) {
        List<String> keys = new ArrayList<>(map.keySet());
        // even though we shuffle later, we need this to make tests reproduce on different jvms
        Collections.sort(keys);
        Map<String, Object> targetMap = new TreeMap<>();
        Collections.shuffle(keys, random());
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Map && exceptFieldNames.contains(key) == false) {
                targetMap.put(key, shuffleMap((Map) value, exceptFieldNames));
            } else {
                targetMap.put(key, value);
            }
        }
        return targetMap;
    }

    /**
     * Returns true iff assertions for elasticsearch packages are enabled
     */
    public static boolean assertionsEnabled() {
        boolean enabled = false;
        assert (enabled = true);
        return enabled;
    }

    /**
     * Asserts that there are no files in the specified path
     */
    public void assertPathHasBeenCleared(String path) throws Exception {
        assertPathHasBeenCleared(PathUtils.get(path));
    }

    /**
     * Asserts that there are no files in the specified path
     */
    public void assertPathHasBeenCleared(Path path) throws Exception {
        logger.info("--> checking that [{}] has been cleared", path);
        int count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (Files.exists(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path file : stream) {
                    // Skip files added by Lucene's ExtraFS
                    if (file.getFileName().toString().startsWith("extra")) {
                        continue;
                    }
                    logger.info("--> found file: [{}]", file.toAbsolutePath().toString());
                    if (Files.isDirectory(file)) {
                        assertPathHasBeenCleared(file);
                    } else if (Files.isRegularFile(file)) {
                        count++;
                        sb.append(file.toAbsolutePath().toString());
                        sb.append("\n");
                    }
                }
            }
        }
        sb.append("]");
        assertThat(count + " files exist that should have been cleaned:\n" + sb.toString(), count, equalTo(0));
    }

    /** Returns the suite failure marker: internal use only! */
    public static TestRuleMarkFailure getSuiteFailureMarker() {
        return suiteFailureMarker;
    }

    /** Compares two stack traces, ignoring module (which is not yet serialized) */
    public static void assertArrayEquals(StackTraceElement expected[], StackTraceElement actual[]) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    /** Compares two stack trace elements, ignoring module (which is not yet serialized) */
    public static void assertEquals(StackTraceElement expected, StackTraceElement actual) {
        assertEquals(expected.getClassName(), actual.getClassName());
        assertEquals(expected.getMethodName(), actual.getMethodName());
        assertEquals(expected.getFileName(), actual.getFileName());
        assertEquals(expected.getLineNumber(), actual.getLineNumber());
        assertEquals(expected.isNativeMethod(), actual.isNativeMethod());
    }

    protected static long spinForAtLeastOneMillisecond() {
        long nanosecondsInMillisecond = TimeUnit.NANOSECONDS.convert(1, TimeUnit.MILLISECONDS);
        // force at least one millisecond to elapse, but ensure the
        // clock has enough resolution to observe the passage of time
        long start = System.nanoTime();
        long elapsed;
        while ((elapsed = (System.nanoTime() - start)) < nanosecondsInMillisecond) {
            // busy spin
        }
        return elapsed;
    }

    /**
     * Creates an AnalysisService to test analysis factories and analyzers.
     */
    @SafeVarargs
    public static AnalysisService createAnalysisService(Index index, Settings settings, Consumer<AnalysisModule>... moduleConsumers) throws IOException {
        Settings nodeSettings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir()).build();
        return createAnalysisService(index, nodeSettings, settings, moduleConsumers);
    }

    /**
     * Creates an AnalysisService to test analysis factories and analyzers.
     */
    @SafeVarargs
    public static AnalysisService createAnalysisService(Index index, Settings nodeSettings, Settings settings, Consumer<AnalysisModule>... moduleConsumers) throws IOException {
        Settings indexSettings = settingsBuilder().put(settings)
            .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .build();
        Environment env = new Environment(nodeSettings);
        AnalysisModule analysisModule = new AnalysisModule(env);
        for (Consumer<AnalysisModule> consumer : moduleConsumers) {
            consumer.accept(analysisModule);
        }
        SettingsModule settingsModule = new SettingsModule(nodeSettings);
        settingsModule.registerSetting(InternalSettingsPlugin.VERSION_CREATED);
        final AnalysisService analysisService = analysisModule.buildRegistry().build(IndexSettingsModule.newIndexSettings(index, indexSettings));
        return analysisService;
    }
}
