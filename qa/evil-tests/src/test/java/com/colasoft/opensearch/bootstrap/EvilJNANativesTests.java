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

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package com.colasoft.opensearch.bootstrap;

import org.apache.lucene.util.Constants;
import com.colasoft.opensearch.common.io.PathUtils;
import com.colasoft.opensearch.test.OpenSearchTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class EvilJNANativesTests extends OpenSearchTestCase {

    public void testSetMaximumNumberOfThreads() throws IOException {
        if (Constants.LINUX) {
            final List<String> lines = Files.readAllLines(PathUtils.get("/proc/self/limits"));
            for (final String line : lines) {
                if (line != null && line.startsWith("Max processes")) {
                    final String[] fields = line.split("\\s+");
                    final long limit =
                            "unlimited".equals(fields[2])
                                    ? JNACLibrary.RLIM_INFINITY
                                    : Long.parseLong(fields[2]);
                    assertThat(JNANatives.MAX_NUMBER_OF_THREADS, equalTo(limit));
                    return;
                }
            }
            fail("should have read max processes from /proc/self/limits");
        } else {
            assertThat(JNANatives.MAX_NUMBER_OF_THREADS, equalTo(-1L));
        }
    }

    public void testSetMaxSizeVirtualMemory() throws IOException {
        if (Constants.LINUX) {
            final List<String> lines = Files.readAllLines(PathUtils.get("/proc/self/limits"));
            for (final String line : lines) {
                if (line != null && line.startsWith("Max address space")) {
                    final String[] fields = line.split("\\s+");
                    final String limit = fields[3];
                    assertThat(
                            JNANatives.rlimitToString(JNANatives.MAX_SIZE_VIRTUAL_MEMORY),
                            equalTo(limit));
                    return;
                }
            }
            fail("should have read max size virtual memory from /proc/self/limits");
        } else if (Constants.MAC_OS_X) {
            assertThat(
                    JNANatives.MAX_SIZE_VIRTUAL_MEMORY,
                    anyOf(equalTo(Long.MIN_VALUE), greaterThanOrEqualTo(0L)));
        } else {
            assertThat(JNANatives.MAX_SIZE_VIRTUAL_MEMORY, equalTo(Long.MIN_VALUE));
        }
    }

    public void testSetMaxFileSize() throws IOException {
        if (Constants.LINUX) {
            final List<String> lines = Files.readAllLines(PathUtils.get("/proc/self/limits"));
            for (final String line : lines) {
                if (line != null && line.startsWith("Max file size")) {
                    final String[] fields = line.split("\\s+");
                    final String limit = fields[3];
                    assertThat(
                            JNANatives.rlimitToString(JNANatives.MAX_FILE_SIZE),
                            equalTo(limit));
                    return;
                }
            }
            fail("should have read max file size from /proc/self/limits");
        } else if (Constants.MAC_OS_X) {
            assertThat(
                    JNANatives.MAX_FILE_SIZE,
                    anyOf(equalTo(Long.MIN_VALUE), greaterThanOrEqualTo(0L)));
        } else {
            assertThat(JNANatives.MAX_FILE_SIZE, equalTo(Long.MIN_VALUE));
        }
    }

}
