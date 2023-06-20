/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The ColaSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.index.codec.customcodecs;

import com.colasoft.opensearch.plugins.Plugin;
import com.colasoft.opensearch.plugins.EnginePlugin;

/**
 * A plugin that implements custom codecs. Supports these codecs:
 * <ul>
 * <li>ZSTD
 * <li>ZSTDNODICT
 * </ul>
 *
 * @opensearch.internal
 */
public final class CustomCodecPlugin extends Plugin implements EnginePlugin {
    /** Creates a new instance. */
    public CustomCodecPlugin() {}
}
