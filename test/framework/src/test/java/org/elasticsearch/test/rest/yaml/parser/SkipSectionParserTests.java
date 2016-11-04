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
package org.elasticsearch.test.rest.yaml.parser;

import org.elasticsearch.Version;
import org.elasticsearch.common.xcontent.yaml.YamlXContent;
import org.elasticsearch.test.VersionUtils;
import org.elasticsearch.test.rest.yaml.parser.ClientYamlTestParseException;
import org.elasticsearch.test.rest.yaml.parser.ClientYamlTestSuiteParseContext;
import org.elasticsearch.test.rest.yaml.parser.SkipSectionParser;
import org.elasticsearch.test.rest.yaml.section.SkipSection;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class SkipSectionParserTests extends AbstractParserTestCase {
    public void testParseSkipSectionVersionNoFeature() throws Exception {
        parser = YamlXContent.yamlXContent.createParser(
                "version:     \" - 2.1.0\"\n" +
                "reason:      Delete ignores the parent param"
        );

        SkipSectionParser skipSectionParser = new SkipSectionParser();

        SkipSection skipSection = skipSectionParser.parse(new ClientYamlTestSuiteParseContext("api", "suite", parser));

        assertThat(skipSection, notNullValue());
        assertThat(skipSection.getLowerVersion(), equalTo(VersionUtils.getFirstVersion()));
        assertThat(skipSection.getUpperVersion(), equalTo(Version.V_2_1_0));
        assertThat(skipSection.getFeatures().size(), equalTo(0));
        assertThat(skipSection.getReason(), equalTo("Delete ignores the parent param"));
    }

    public void testParseSkipSectionAllVersions() throws Exception {
        parser = YamlXContent.yamlXContent.createParser(
            "version:     \" all \"\n" +
            "reason:      Delete ignores the parent param"
        );

        SkipSectionParser skipSectionParser = new SkipSectionParser();

        SkipSection skipSection = skipSectionParser.parse(new ClientYamlTestSuiteParseContext("api", "suite", parser));

        assertThat(skipSection, notNullValue());
        assertThat(skipSection.getLowerVersion(), equalTo(VersionUtils.getFirstVersion()));
        assertThat(skipSection.getUpperVersion(), equalTo(Version.CURRENT));
        assertThat(skipSection.getFeatures().size(), equalTo(0));
        assertThat(skipSection.getReason(), equalTo("Delete ignores the parent param"));
    }

    public void testParseSkipSectionFeatureNoVersion() throws Exception {
        parser = YamlXContent.yamlXContent.createParser(
                "features:     regex"
        );

        SkipSectionParser skipSectionParser = new SkipSectionParser();

        SkipSection skipSection = skipSectionParser.parse(new ClientYamlTestSuiteParseContext("api", "suite", parser));

        assertThat(skipSection, notNullValue());
        assertThat(skipSection.isVersionCheck(), equalTo(false));
        assertThat(skipSection.getFeatures().size(), equalTo(1));
        assertThat(skipSection.getFeatures().get(0), equalTo("regex"));
        assertThat(skipSection.getReason(), nullValue());
    }

    public void testParseSkipSectionFeaturesNoVersion() throws Exception {
        parser = YamlXContent.yamlXContent.createParser(
                "features:     [regex1,regex2,regex3]"
        );

        SkipSectionParser skipSectionParser = new SkipSectionParser();

        SkipSection skipSection = skipSectionParser.parse(new ClientYamlTestSuiteParseContext("api", "suite", parser));

        assertThat(skipSection, notNullValue());
        assertThat(skipSection.isVersionCheck(), equalTo(false));
        assertThat(skipSection.getFeatures().size(), equalTo(3));
        assertThat(skipSection.getFeatures().get(0), equalTo("regex1"));
        assertThat(skipSection.getFeatures().get(1), equalTo("regex2"));
        assertThat(skipSection.getFeatures().get(2), equalTo("regex3"));
        assertThat(skipSection.getReason(), nullValue());
    }

    public void testParseSkipSectionBothFeatureAndVersion() throws Exception {
        parser = YamlXContent.yamlXContent.createParser(
                "version:     \" - 0.90.2\"\n" +
                "features:     regex\n" +
                "reason:      Delete ignores the parent param"
        );

        SkipSectionParser skipSectionParser = new SkipSectionParser();
        SkipSection parse = skipSectionParser.parse(new ClientYamlTestSuiteParseContext("api", "suite", parser));
        assertEquals(VersionUtils.getFirstVersion(), parse.getLowerVersion());
        assertEquals(Version.fromString("0.90.2"), parse.getUpperVersion());
        assertEquals(Arrays.asList("regex"), parse.getFeatures());
        assertEquals("Delete ignores the parent param", parse.getReason());
    }

    public void testParseSkipSectionNoReason() throws Exception {
        parser = YamlXContent.yamlXContent.createParser(
                "version:     \" - 0.90.2\"\n"
        );

        SkipSectionParser skipSectionParser = new SkipSectionParser();
        try {
            skipSectionParser.parse(new ClientYamlTestSuiteParseContext("api", "suite", parser));
            fail("Expected RestTestParseException");
        } catch (ClientYamlTestParseException e) {
            assertThat(e.getMessage(), is("reason is mandatory within skip version section"));
        }
    }

    public void testParseSkipSectionNoVersionNorFeature() throws Exception {
        parser = YamlXContent.yamlXContent.createParser(
                "reason:      Delete ignores the parent param\n"
        );

        SkipSectionParser skipSectionParser = new SkipSectionParser();
        try {
            skipSectionParser.parse(new ClientYamlTestSuiteParseContext("api", "suite", parser));
            fail("Expected RestTestParseException");
        } catch (ClientYamlTestParseException e) {
            assertThat(e.getMessage(), is("version or features is mandatory within skip section"));
        }
    }
}
