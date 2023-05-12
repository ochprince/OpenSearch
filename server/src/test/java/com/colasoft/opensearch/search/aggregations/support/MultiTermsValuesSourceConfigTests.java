/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package com.colasoft.opensearch.search.aggregations.support;

import com.colasoft.opensearch.common.io.stream.NamedWriteableRegistry;
import com.colasoft.opensearch.common.io.stream.Writeable;
import com.colasoft.opensearch.common.settings.Settings;
import com.colasoft.opensearch.core.xcontent.NamedXContentRegistry;
import com.colasoft.opensearch.core.xcontent.XContentParser;
import com.colasoft.opensearch.script.Script;
import com.colasoft.opensearch.search.SearchModule;
import com.colasoft.opensearch.test.AbstractSerializingTestCase;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;

public class MultiTermsValuesSourceConfigTests extends AbstractSerializingTestCase<MultiTermsValuesSourceConfig> {

    @Override
    protected MultiTermsValuesSourceConfig doParseInstance(XContentParser parser) throws IOException {
        return MultiTermsValuesSourceConfig.PARSER.apply(true, true, true, true).apply(parser, null).build();
    }

    @Override
    protected MultiTermsValuesSourceConfig createTestInstance() {
        String field = randomAlphaOfLength(10);
        Object missing = randomBoolean() ? randomAlphaOfLength(10) : null;
        ZoneId timeZone = randomBoolean() ? randomZone() : null;
        Script script = randomBoolean() ? new Script(randomAlphaOfLength(10)) : null;
        return new MultiTermsValuesSourceConfig.Builder().setFieldName(field)
            .setMissing(missing)
            .setScript(script)
            .setTimeZone(timeZone)
            .build();
    }

    @Override
    protected Writeable.Reader<MultiTermsValuesSourceConfig> instanceReader() {
        return MultiTermsValuesSourceConfig::new;
    }

    public void testMissingFieldScript() {
        IllegalArgumentException e = expectThrows(IllegalArgumentException.class, () -> new MultiTermsValuesSourceConfig.Builder().build());
        assertThat(e.getMessage(), equalTo("[field] and [script] cannot both be null.  Please specify one or the other."));
    }

    @Override
    protected NamedWriteableRegistry getNamedWriteableRegistry() {
        return new NamedWriteableRegistry(new SearchModule(Settings.EMPTY, Collections.emptyList()).getNamedWriteables());
    }

    @Override
    protected NamedXContentRegistry xContentRegistry() {
        return new NamedXContentRegistry(new SearchModule(Settings.EMPTY, Collections.emptyList()).getNamedXContents());
    }
}
