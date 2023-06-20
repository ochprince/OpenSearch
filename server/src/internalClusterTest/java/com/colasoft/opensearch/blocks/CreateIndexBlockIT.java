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

package com.colasoft.opensearch.blocks;

import org.junit.After;
import com.colasoft.opensearch.cluster.metadata.Metadata;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.test.OpenSearchIntegTestCase;

import static com.colasoft.opensearch.test.OpenSearchIntegTestCase.client;
import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertAcked;
import static com.colasoft.opensearch.test.hamcrest.OpenSearchAssertions.assertBlocked;

public class CreateIndexBlockIT extends OpenSearchIntegTestCase {

    public void testBlockCreateIndex() {
        setCreateIndexBlock("true");
        assertBlocked(client().admin().indices().prepareCreate("uncreated-idx"), Metadata.CLUSTER_CREATE_INDEX_BLOCK);
        setCreateIndexBlock("false");
        assertAcked(client().admin().indices().prepareCreate("created-idx").execute().actionGet());
    }

    @After
    public void cleanup() throws Exception {
        Settings settings = Settings.builder().putNull(Metadata.SETTING_CREATE_INDEX_BLOCK_SETTING.getKey()).build();
        assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(settings).get());
    }

    private void setCreateIndexBlock(String value) {
        Settings settings = Settings.builder().put(Metadata.SETTING_CREATE_INDEX_BLOCK_SETTING.getKey(), value).build();
        assertAcked(client().admin().cluster().prepareUpdateSettings().setTransientSettings(settings).get());
    }

}
