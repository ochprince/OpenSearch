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

package org.elasticsearch.index.query;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.PointRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.ParsingException;
import org.elasticsearch.common.lucene.BytesRefs;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.MappedFieldType.Relation;
import org.elasticsearch.test.AbstractQueryTestCase;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.sameInstance;

public class RangeQueryBuilderTests extends AbstractQueryTestCase<RangeQueryBuilder> {

    @Override
    protected RangeQueryBuilder doCreateTestQueryBuilder() {
        RangeQueryBuilder query;
        // switch between numeric and date ranges
        switch (randomIntBetween(0, 2)) {
            case 0:
                // use mapped integer field for numeric range queries
                query = new RangeQueryBuilder(INT_FIELD_NAME);
                query.from(randomIntBetween(1, 100));
                query.to(randomIntBetween(101, 200));
                break;
            case 1:
                // use mapped date field, using date string representation
                query = new RangeQueryBuilder(DATE_FIELD_NAME);
                query.from(new DateTime(System.currentTimeMillis() - randomIntBetween(0, 1000000), DateTimeZone.UTC).toString());
                query.to(new DateTime(System.currentTimeMillis() + randomIntBetween(0, 1000000), DateTimeZone.UTC).toString());
                // Create timestamp option only then we have a date mapper,
                // otherwise we could trigger exception.
                if (createShardContext().getMapperService().fullName(DATE_FIELD_NAME) != null) {
                    if (randomBoolean()) {
                        query.timeZone(randomDateTimeZone().getID());
                    }
                    if (randomBoolean()) {
                        query.format("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                    }
                }
                break;
            case 2:
            default:
                query = new RangeQueryBuilder(STRING_FIELD_NAME);
                query.from("a" + randomAsciiOfLengthBetween(1, 10));
                query.to("z" + randomAsciiOfLengthBetween(1, 10));
                break;
        }
        query.includeLower(randomBoolean()).includeUpper(randomBoolean());
        if (randomBoolean()) {
            query.from(null);
        }
        if (randomBoolean()) {
            query.to(null);
        }
        return query;
    }

    @Override
    protected Map<String, RangeQueryBuilder> getAlternateVersions() {
        Map<String, RangeQueryBuilder> alternateVersions = new HashMap<>();
        RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(INT_FIELD_NAME);
        rangeQueryBuilder.from(randomIntBetween(1, 100)).to(randomIntBetween(101, 200));
        rangeQueryBuilder.includeLower(randomBoolean());
        rangeQueryBuilder.includeUpper(randomBoolean());
        String query =
                "{\n" +
                "    \"range\":{\n" +
                "        \"" + INT_FIELD_NAME + "\": {\n" +
                "            \"" + (rangeQueryBuilder.includeLower() ? "gte" : "gt") + "\": " + rangeQueryBuilder.from() +  ",\n" +
                "            \"" + (rangeQueryBuilder.includeUpper() ? "lte" : "lt") + "\": " + rangeQueryBuilder.to() +  "\n" +
                "        }\n" +
                "    }\n" +
                "}";
        alternateVersions.put(query, rangeQueryBuilder);
        return alternateVersions;
    }

    @Override
    protected void doAssertLuceneQuery(RangeQueryBuilder queryBuilder, Query query, QueryShardContext context) throws IOException {
        if (getCurrentTypes().length == 0 || (queryBuilder.fieldName().equals(DATE_FIELD_NAME) == false && queryBuilder.fieldName().equals(INT_FIELD_NAME) == false)) {
            assertThat(query, instanceOf(TermRangeQuery.class));
            TermRangeQuery termRangeQuery = (TermRangeQuery) query;
            assertThat(termRangeQuery.getField(), equalTo(queryBuilder.fieldName()));
            assertThat(termRangeQuery.getLowerTerm(), equalTo(BytesRefs.toBytesRef(queryBuilder.from())));
            assertThat(termRangeQuery.getUpperTerm(), equalTo(BytesRefs.toBytesRef(queryBuilder.to())));
            assertThat(termRangeQuery.includesLower(), equalTo(queryBuilder.includeLower()));
            assertThat(termRangeQuery.includesUpper(), equalTo(queryBuilder.includeUpper()));
        } else if (queryBuilder.fieldName().equals(DATE_FIELD_NAME)) {
            //we can't properly test unmapped dates because LateParsingQuery is package private
        } else if (queryBuilder.fieldName().equals(INT_FIELD_NAME)) {
            assertThat(query, either(instanceOf(LegacyNumericRangeQuery.class)).or(instanceOf(PointRangeQuery.class)));
            if (query instanceof LegacyNumericRangeQuery) {
                LegacyNumericRangeQuery numericRangeQuery = (LegacyNumericRangeQuery) query;
                assertThat(numericRangeQuery.getField(), equalTo(queryBuilder.fieldName()));
                assertThat(numericRangeQuery.getMin(), equalTo(queryBuilder.from()));
                assertThat(numericRangeQuery.getMax(), equalTo(queryBuilder.to()));
                assertThat(numericRangeQuery.includesMin(), equalTo(queryBuilder.includeLower()));
                assertThat(numericRangeQuery.includesMax(), equalTo(queryBuilder.includeUpper()));
            } else {
                Integer min = (Integer) queryBuilder.from();
                Integer max = (Integer) queryBuilder.to();
                int minInt, maxInt;
                if (min == null) {
                    minInt = Integer.MIN_VALUE;
                } else {
                    minInt = min.intValue();
                    if (queryBuilder.includeLower() == false && minInt != Integer.MAX_VALUE) {
                        minInt++;
                    }
                }
                if (max == null) {
                    maxInt = Integer.MAX_VALUE;
                } else {
                    maxInt = max.intValue();
                    if (queryBuilder.includeUpper() == false && maxInt != Integer.MIN_VALUE) {
                        maxInt--;
                    }
                }
                try {
                assertEquals(IntPoint.newRangeQuery(INT_FIELD_NAME, minInt, maxInt), query);
                }catch(AssertionError e) {
                    throw e;
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void testIllegalArguments() {
        expectThrows(IllegalArgumentException.class, () -> new RangeQueryBuilder((String) null));
        expectThrows(IllegalArgumentException.class, () -> new RangeQueryBuilder(""));

        RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder("test");
        try {
            if (randomBoolean()) {
                rangeQueryBuilder.timeZone(null);
            } else {
                rangeQueryBuilder.timeZone("badID");
            }
            fail("cannot be null or unknown id");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            if (randomBoolean()) {
                rangeQueryBuilder.format(null);
            } else {
                rangeQueryBuilder.format("badFormat");
            }
            fail("cannot be null or bad format");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * Specifying a timezone together with a numeric range query should throw an exception.
     */
    public void testToQueryNonDateWithTimezone() throws QueryShardException, IOException {
        RangeQueryBuilder query = new RangeQueryBuilder(INT_FIELD_NAME);
        query.from(1).to(10).timeZone("UTC");
        try {
            query.toQuery(createShardContext());
            fail("Expected QueryShardException");
        } catch (QueryShardException e) {
            assertThat(e.getMessage(), containsString("[range] time_zone can not be applied"));
        }
    }

    /**
     * Specifying a timezone together with an unmapped field should throw an exception.
     */
    public void testToQueryUnmappedWithTimezone() throws QueryShardException, IOException {
        RangeQueryBuilder query = new RangeQueryBuilder("bogus_field");
        query.from(1).to(10).timeZone("UTC");
        try {
            query.toQuery(createShardContext());
            fail("Expected QueryShardException");
        } catch (QueryShardException e) {
            assertThat(e.getMessage(), containsString("[range] time_zone can not be applied"));
        }
    }

    public void testToQueryNumericField() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        Query parsedQuery = rangeQuery(INT_FIELD_NAME).from(23).to(54).includeLower(true).includeUpper(false).toQuery(createShardContext());
        // since age is automatically registered in data, we encode it as numeric
        assertThat(parsedQuery, either(instanceOf(LegacyNumericRangeQuery.class)).or(instanceOf(PointRangeQuery.class)));
        if (parsedQuery instanceof LegacyNumericRangeQuery) {
            LegacyNumericRangeQuery rangeQuery = (LegacyNumericRangeQuery) parsedQuery;
            assertThat(rangeQuery.getField(), equalTo(INT_FIELD_NAME));
            assertThat(rangeQuery.getMin().intValue(), equalTo(23));
            assertThat(rangeQuery.getMax().intValue(), equalTo(54));
            assertThat(rangeQuery.includesMin(), equalTo(true));
            assertThat(rangeQuery.includesMax(), equalTo(false));
        } else {
            assertEquals(IntPoint.newRangeQuery(INT_FIELD_NAME, 23, 53), parsedQuery);
        }
    }

    public void testDateRangeQueryFormat() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        // We test 01/01/2012 from gte and 2030 for lt
        String query = "{\n" +
                "    \"range\" : {\n" +
                "        \"" + DATE_FIELD_NAME + "\" : {\n" +
                "            \"gte\": \"01/01/2012\",\n" +
                "            \"lt\": \"2030\",\n" +
                "            \"format\": \"dd/MM/yyyy||yyyy\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        Query parsedQuery = parseQuery(query).toQuery(createShardContext()).rewrite(null);
        assertThat(parsedQuery, either(instanceOf(LegacyNumericRangeQuery.class)).or(instanceOf(PointRangeQuery.class)));

        if (parsedQuery instanceof LegacyNumericRangeQuery) {
            // Min value was 01/01/2012 (dd/MM/yyyy)
            DateTime min = DateTime.parse("2012-01-01T00:00:00.000+00");
            assertThat(((LegacyNumericRangeQuery) parsedQuery).getMin().longValue(), is(min.getMillis()));

            // Max value was 2030 (yyyy)
            DateTime max = DateTime.parse("2030-01-01T00:00:00.000+00");
            assertThat(((LegacyNumericRangeQuery) parsedQuery).getMax().longValue(), is(max.getMillis()));
        } else {
            assertEquals(LongPoint.newRangeQuery(DATE_FIELD_NAME,
                    DateTime.parse("2012-01-01T00:00:00.000+00").getMillis(),
                    DateTime.parse("2030-01-01T00:00:00.000+00").getMillis() - 1),
                    parsedQuery);
        }

        // Test Invalid format
        query = "{\n" +
                "    \"range\" : {\n" +
                "        \"" + DATE_FIELD_NAME + "\" : {\n" +
                "            \"gte\": \"01/01/2012\",\n" +
                "            \"lt\": \"2030\",\n" +
                "            \"format\": \"yyyy\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        try {
            parseQuery(query).toQuery(createShardContext()).rewrite(null);
            fail("A Range Query with a specific format but with an unexpected date should raise a ParsingException");
        } catch (ElasticsearchParseException e) {
            // We expect it
        }
    }

    public void testDateRangeBoundaries() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        String query = "{\n" +
                "    \"range\" : {\n" +
                "        \"" + DATE_FIELD_NAME + "\" : {\n" +
                "            \"gte\": \"2014-11-05||/M\",\n" +
                "            \"lte\": \"2014-12-08||/d\"\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        Query parsedQuery = parseQuery(query).toQuery(createShardContext()).rewrite(null);
        assertThat(parsedQuery, either(instanceOf(LegacyNumericRangeQuery.class)).or(instanceOf(PointRangeQuery.class)));
        if (parsedQuery instanceof LegacyNumericRangeQuery) {
            LegacyNumericRangeQuery rangeQuery = (LegacyNumericRangeQuery) parsedQuery;

            DateTime min = DateTime.parse("2014-11-01T00:00:00.000+00");
            assertThat(rangeQuery.getMin().longValue(), is(min.getMillis()));
            assertTrue(rangeQuery.includesMin());

            DateTime max = DateTime.parse("2014-12-08T23:59:59.999+00");
            assertThat(rangeQuery.getMax().longValue(), is(max.getMillis()));
            assertTrue(rangeQuery.includesMax());
        } else {
            assertEquals(LongPoint.newRangeQuery(DATE_FIELD_NAME,
                    DateTime.parse("2014-11-01T00:00:00.000+00").getMillis(),
                    DateTime.parse("2014-12-08T23:59:59.999+00").getMillis()),
                    parsedQuery);
        }

        query = "{\n" +
                "    \"range\" : {\n" +
                "        \"" + DATE_FIELD_NAME + "\" : {\n" +
                "            \"gt\": \"2014-11-05||/M\",\n" +
                "            \"lt\": \"2014-12-08||/d\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        parsedQuery = parseQuery(query).toQuery(createShardContext()).rewrite(null);
        assertThat(parsedQuery, either(instanceOf(LegacyNumericRangeQuery.class)).or(instanceOf(PointRangeQuery.class)));
        if (parsedQuery instanceof LegacyNumericRangeQuery) {
            LegacyNumericRangeQuery rangeQuery = (LegacyNumericRangeQuery) parsedQuery;

            DateTime min = DateTime.parse("2014-11-30T23:59:59.999+00");
            assertThat(rangeQuery.getMin().longValue(), is(min.getMillis()));
            assertFalse(rangeQuery.includesMin());

            DateTime max = DateTime.parse("2014-12-08T00:00:00.000+00");
            assertThat(rangeQuery.getMax().longValue(), is(max.getMillis()));
            assertFalse(rangeQuery.includesMax());
        } else {
            assertEquals(LongPoint.newRangeQuery(DATE_FIELD_NAME,
                    DateTime.parse("2014-11-30T23:59:59.999+00").getMillis() + 1,
                    DateTime.parse("2014-12-08T00:00:00.000+00").getMillis() - 1),
                    parsedQuery);
        }
    }

    public void testDateRangeQueryTimezone() throws IOException {
        assumeTrue("test runs only when at least a type is registered", getCurrentTypes().length > 0);
        long startDate = System.currentTimeMillis();
        String query = "{\n" +
                "    \"range\" : {\n" +
                "        \"" + DATE_FIELD_NAME + "\" : {\n" +
                "            \"gte\": \"2012-01-01\",\n" +
                "            \"lte\": \"now\",\n" +
                "            \"time_zone\": \"+01:00\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        Query parsedQuery = parseQuery(query).toQuery(createShardContext()).rewrite(null);
        if (parsedQuery instanceof PointRangeQuery) {
            // TODO what can we assert
        } else {
            assertThat(parsedQuery, instanceOf(LegacyNumericRangeQuery.class));

            // Min value was 2012-01-01 (UTC) so we need to remove one hour
            DateTime min = DateTime.parse("2012-01-01T00:00:00.000+01:00");
            // Max value is when we started the test. So it should be some ms from now
            DateTime max = new DateTime(startDate, DateTimeZone.UTC);

            assertThat(((LegacyNumericRangeQuery) parsedQuery).getMin().longValue(), is(min.getMillis()));

            // We should not have a big difference here (should be some ms)
            assertThat(((LegacyNumericRangeQuery) parsedQuery).getMax().longValue() - max.getMillis(), lessThanOrEqualTo(60000L));
        }

        query = "{\n" +
                "    \"range\" : {\n" +
                "        \"" + INT_FIELD_NAME + "\" : {\n" +
                "            \"gte\": \"0\",\n" +
                "            \"lte\": \"100\",\n" +
                "            \"time_zone\": \"-01:00\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
        try {
            parseQuery(query).toQuery(createShardContext());
            fail("A Range Query on a numeric field with a TimeZone should raise a ParsingException");
        } catch (QueryShardException e) {
            // We expect it
        }
    }

    public void testFromJson() throws IOException {
        String json =
                "{\n" +
                "  \"range\" : {\n" +
                "    \"timestamp\" : {\n" +
                "      \"from\" : \"2015-01-01 00:00:00\",\n" +
                "      \"to\" : \"now\",\n" +
                "      \"include_lower\" : true,\n" +
                "      \"include_upper\" : true,\n" +
                "      \"time_zone\" : \"+01:00\",\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}";

        RangeQueryBuilder parsed = (RangeQueryBuilder) parseQuery(json);
        checkGeneratedJson(json, parsed);

        assertEquals(json, "2015-01-01 00:00:00", parsed.from());
        assertEquals(json, "now", parsed.to());
    }

    public void testNamedQueryParsing() throws IOException {
        String json =
                "{\n" +
                "  \"range\" : {\n" +
                "    \"timestamp\" : {\n" +
                "      \"from\" : \"2015-01-01 00:00:00\",\n" +
                "      \"to\" : \"now\",\n" +
                "      \"boost\" : 1.0,\n" +
                "      \"_name\" : \"my_range\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
        assertNotNull(parseQuery(json));

        json =
                "{\n" +
                "  \"range\" : {\n" +
                "    \"timestamp\" : {\n" +
                "      \"from\" : \"2015-01-01 00:00:00\",\n" +
                "      \"to\" : \"now\",\n" +
                "      \"boost\" : 1.0\n" +
                "    },\n" +
                "    \"_name\" : \"my_range\"\n" +
                "  }\n" +
                "}";

        // non strict parsing should accept "_name" on top level
        assertNotNull(parseQuery(json, ParseFieldMatcher.EMPTY));

        // with strict parsing, ParseField will throw exception
        try {
            parseQuery(json, ParseFieldMatcher.STRICT);
            fail("Strict parsing should trigger exception for '_name' on top level");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), equalTo("Deprecated field [_name] used, replaced by [query name is not supported in short version of range query]"));
        }
    }

    public void testRewriteDateToMatchAll() throws IOException {
        String fieldName = randomAsciiOfLengthBetween(1, 20);
        RangeQueryBuilder query = new RangeQueryBuilder(fieldName) {
            @Override
            protected MappedFieldType.Relation getRelation(QueryRewriteContext queryRewriteContext) throws IOException {
                return Relation.WITHIN;
            }
        };
        DateTime queryFromValue = new DateTime(2015, 1, 1, 0, 0, 0, ISOChronology.getInstanceUTC());
        DateTime queryToValue = new DateTime(2016, 1, 1, 0, 0, 0, ISOChronology.getInstanceUTC());
        DateTime shardMinValue = new DateTime(2015, 3, 1, 0, 0, 0, ISOChronology.getInstanceUTC());
        DateTime shardMaxValue = new DateTime(2015, 9, 1, 0, 0, 0, ISOChronology.getInstanceUTC());
        query.from(queryFromValue);
        query.to(queryToValue);
        QueryShardContext queryShardContext = createShardContext();
        QueryBuilder rewritten = query.rewrite(queryShardContext);
        assertThat(rewritten, instanceOf(RangeQueryBuilder.class));
        RangeQueryBuilder rewrittenRange = (RangeQueryBuilder) rewritten;
        assertThat(rewrittenRange.fieldName(), equalTo(fieldName));
        assertThat(rewrittenRange.from(), equalTo(null));
        assertThat(rewrittenRange.to(), equalTo(null));
    }

    public void testRewriteDateToMatchNone() throws IOException {
        String fieldName = randomAsciiOfLengthBetween(1, 20);
        RangeQueryBuilder query = new RangeQueryBuilder(fieldName) {
            @Override
            protected MappedFieldType.Relation getRelation(QueryRewriteContext queryRewriteContext) throws IOException {
                return Relation.DISJOINT;
            }
        };
        DateTime queryFromValue = new DateTime(2015, 1, 1, 0, 0, 0, ISOChronology.getInstanceUTC());
        DateTime queryToValue = new DateTime(2016, 1, 1, 0, 0, 0, ISOChronology.getInstanceUTC());
        query.from(queryFromValue);
        query.to(queryToValue);
        QueryShardContext queryShardContext = createShardContext();
        QueryBuilder rewritten = query.rewrite(queryShardContext);
        assertThat(rewritten, instanceOf(MatchNoneQueryBuilder.class));
    }

    public void testRewriteDateToSame() throws IOException {
        String fieldName = randomAsciiOfLengthBetween(1, 20);
        RangeQueryBuilder query = new RangeQueryBuilder(fieldName) {
            @Override
            protected MappedFieldType.Relation getRelation(QueryRewriteContext queryRewriteContext) throws IOException {
                return Relation.INTERSECTS;
            }
        };
        DateTime queryFromValue = new DateTime(2015, 1, 1, 0, 0, 0, ISOChronology.getInstanceUTC());
        DateTime queryToValue = new DateTime(2016, 1, 1, 0, 0, 0, ISOChronology.getInstanceUTC());
        query.from(queryFromValue);
        query.to(queryToValue);
        QueryShardContext queryShardContext = createShardContext();
        QueryBuilder rewritten = query.rewrite(queryShardContext);
        assertThat(rewritten, sameInstance(query));
    }

    public void testRewriteOpenBoundsToSame() throws IOException {
        String fieldName = randomAsciiOfLengthBetween(1, 20);
        RangeQueryBuilder query = new RangeQueryBuilder(fieldName) {
            @Override
            protected MappedFieldType.Relation getRelation(QueryRewriteContext queryRewriteContext) throws IOException {
                return Relation.INTERSECTS;
            }
        };
        QueryShardContext queryShardContext = createShardContext();
        QueryBuilder rewritten = query.rewrite(queryShardContext);
        assertThat(rewritten, sameInstance(query));
    }

    public void testParseFailsWithMultipleFields() throws IOException {
        String json =
                "{\n" +
                "    \"range\": {\n" +
                "      \"age\": {\n" +
                "        \"gte\": 30,\n" +
                "        \"lte\": 40\n" +
                "      },\n" +
                "      \"price\": {\n" +
                "        \"gte\": 10,\n" +
                "        \"lte\": 30\n" +
                "      }\n" +
                "    }\n" +
                "  }";

        try {
            parseQuery(json);
            fail("parseQuery should have failed");
        } catch(ParsingException e) {
            assertEquals("[range] query doesn't support multiple fields, found [age] and [price]", e.getMessage());
        }
    }
}
