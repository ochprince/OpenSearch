/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.sort;

import static org.hamcrest.Matchers.equalTo;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.xcontent.json.JsonXContent;
import org.opensearch.plugins.Plugin;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.plugin.CustomSortBuilder;
import org.opensearch.search.sort.plugin.CustomSortPlugin;
import org.opensearch.test.InternalSettingsPlugin;
import org.opensearch.test.OpenSearchIntegTestCase;

import java.util.Arrays;
import java.util.Collection;

public class SortFromPluginIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(CustomSortPlugin.class, InternalSettingsPlugin.class);
    }

    public void testPluginSort() throws Exception {
        createIndex("test");
        ensureGreen();

        client().prepareIndex("test", "type", "1").setSource("field", 2).get();
        client().prepareIndex("test", "type", "2").setSource("field", 1).get();
        client().prepareIndex("test", "type", "3").setSource("field", 0).get();

        refresh();

        SearchResponse searchResponse = client().prepareSearch("test").addSort(new CustomSortBuilder("field", SortOrder.ASC)).get();
        assertThat(searchResponse.getHits().getAt(0).getId(), equalTo("3"));
        assertThat(searchResponse.getHits().getAt(1).getId(), equalTo("2"));
        assertThat(searchResponse.getHits().getAt(2).getId(), equalTo("1"));

        searchResponse = client().prepareSearch("test").addSort(new CustomSortBuilder("field", SortOrder.DESC)).get();
        assertThat(searchResponse.getHits().getAt(0).getId(), equalTo("1"));
        assertThat(searchResponse.getHits().getAt(1).getId(), equalTo("2"));
        assertThat(searchResponse.getHits().getAt(2).getId(), equalTo("3"));
    }

    public void testPluginSortXContent() throws Exception {
        createIndex("test");
        ensureGreen();

        client().prepareIndex("test", "type", "1").setSource("field", 2).get();
        client().prepareIndex("test", "type", "2").setSource("field", 1).get();
        client().prepareIndex("test", "type", "3").setSource("field", 0).get();

        refresh();

        // builder -> json -> builder
        SearchResponse searchResponse = client().prepareSearch("test")
            .setSource(
                SearchSourceBuilder.fromXContent(
                    createParser(
                        JsonXContent.jsonXContent,
                        new SearchSourceBuilder().sort(new CustomSortBuilder("field", SortOrder.ASC)).toString()
                    )
                )
            )
            .get();

        assertThat(searchResponse.getHits().getAt(0).getId(), equalTo("3"));
        assertThat(searchResponse.getHits().getAt(1).getId(), equalTo("2"));
        assertThat(searchResponse.getHits().getAt(2).getId(), equalTo("1"));

        searchResponse = client().prepareSearch("test")
            .setSource(
                SearchSourceBuilder.fromXContent(
                    createParser(
                        JsonXContent.jsonXContent,
                        new SearchSourceBuilder().sort(new CustomSortBuilder("field", SortOrder.DESC)).toString()
                    )
                )
            )
            .get();

        assertThat(searchResponse.getHits().getAt(0).getId(), equalTo("1"));
        assertThat(searchResponse.getHits().getAt(1).getId(), equalTo("2"));
        assertThat(searchResponse.getHits().getAt(2).getId(), equalTo("3"));
    }
}
