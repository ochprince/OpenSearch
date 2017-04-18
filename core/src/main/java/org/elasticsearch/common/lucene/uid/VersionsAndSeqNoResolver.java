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

package org.elasticsearch.common.lucene.uid;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReader.CoreClosedListener;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.CloseableThreadLocal;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.index.mapper.UidFieldMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.elasticsearch.common.lucene.uid.Versions.NOT_FOUND;

/** Utility class to resolve the Lucene doc ID, version, seqNo and primaryTerms for a given uid. */
public final class VersionsAndSeqNoResolver {

    static final ConcurrentMap<Object, CloseableThreadLocal<PerThreadIDVersionAndSeqNoLookup>> lookupStates =
        ConcurrentCollections.newConcurrentMapWithAggressiveConcurrency();

    // Evict this reader from lookupStates once it's closed:
    private static final CoreClosedListener removeLookupState = key -> {
        CloseableThreadLocal<PerThreadIDVersionAndSeqNoLookup> ctl = lookupStates.remove(key);
        if (ctl != null) {
            ctl.close();
        }
    };

    private static PerThreadIDVersionAndSeqNoLookup getLookupState(LeafReader reader) throws IOException {
        Object key = reader.getCoreCacheKey();
        CloseableThreadLocal<PerThreadIDVersionAndSeqNoLookup> ctl = lookupStates.get(key);
        if (ctl == null) {
            // First time we are seeing this reader's core; make a new CTL:
            ctl = new CloseableThreadLocal<>();
            CloseableThreadLocal<PerThreadIDVersionAndSeqNoLookup> other = lookupStates.putIfAbsent(key, ctl);
            if (other == null) {
                // Our CTL won, we must remove it when the core is closed:
                reader.addCoreClosedListener(removeLookupState);
            } else {
                // Another thread beat us to it: just use their CTL:
                ctl = other;
            }
        }

        PerThreadIDVersionAndSeqNoLookup lookupState = ctl.get();
        if (lookupState == null) {
            lookupState = new PerThreadIDVersionAndSeqNoLookup(reader);
            ctl.set(lookupState);
        }

        return lookupState;
    }

    private VersionsAndSeqNoResolver() {
    }

    /** Wraps an {@link LeafReaderContext}, a doc ID <b>relative to the context doc base</b> and a version. */
    public static class DocIdAndVersion {
        public final int docId;
        public final long version;
        public final LeafReaderContext context;

        DocIdAndVersion(int docId, long version, LeafReaderContext context) {
            this.docId = docId;
            this.version = version;
            this.context = context;
        }
    }

    /** Wraps an {@link LeafReaderContext}, a doc ID <b>relative to the context doc base</b> and a seqNo. */
    public static class DocIdAndSeqNo {
        public final int docId;
        public final long seqNo;
        public final LeafReaderContext context;

        DocIdAndSeqNo(int docId, long seqNo, LeafReaderContext context) {
            this.docId = docId;
            this.seqNo = seqNo;
            this.context = context;
        }
    }


    /**
     * Load the internal doc ID and version for the uid from the reader, returning<ul>
     * <li>null if the uid wasn't found,
     * <li>a doc ID and a version otherwise
     * </ul>
     */
    public static DocIdAndVersion loadDocIdAndVersion(IndexReader reader, Term term) throws IOException {
        assert term.field().equals(UidFieldMapper.NAME) : "unexpected term field " + term.field();
        List<LeafReaderContext> leaves = reader.leaves();
        if (leaves.isEmpty()) {
            return null;
        }
        // iterate backwards to optimize for the frequently updated documents
        // which are likely to be in the last segments
        for (int i = leaves.size() - 1; i >= 0; i--) {
            LeafReaderContext context = leaves.get(i);
            LeafReader leaf = context.reader();
            PerThreadIDVersionAndSeqNoLookup lookup = getLookupState(leaf);
            DocIdAndVersion result = lookup.lookupVersion(term.bytes(), leaf.getLiveDocs(), context);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Load the internal doc ID and sequence number for the uid from the reader, returning<ul>
     * <li>null if the uid wasn't found,
     * <li>a doc ID and the associated seqNo otherwise
     * </ul>
     */
    public static DocIdAndSeqNo loadDocIdAndSeqNo(IndexReader reader, Term term) throws IOException {
        assert term.field().equals(UidFieldMapper.NAME) : "unexpected term field " + term.field();
        List<LeafReaderContext> leaves = reader.leaves();
        if (leaves.isEmpty()) {
            return null;
        }
        // iterate backwards to optimize for the frequently updated documents
        // which are likely to be in the last segments
        for (int i = leaves.size() - 1; i >= 0; i--) {
            LeafReaderContext context = leaves.get(i);
            LeafReader leaf = context.reader();
            PerThreadIDVersionAndSeqNoLookup lookup = getLookupState(leaf);
            DocIdAndSeqNo result = lookup.lookupSeqNo(term.bytes(), leaf.getLiveDocs(), context);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Load the primaryTerm associated with the given {@link DocIdAndSeqNo}
     */
    public static long loadPrimaryTerm(DocIdAndSeqNo docIdAndSeqNo) throws IOException {
        LeafReader leaf = docIdAndSeqNo.context.reader();
        PerThreadIDVersionAndSeqNoLookup lookup = getLookupState(leaf);
        long result = lookup.lookUpPrimaryTerm(docIdAndSeqNo.docId);
        assert result > 0 : "should always resolve a primary term for a resolved sequence number. primary_term [" + result + "]"
            + " docId [" + docIdAndSeqNo.docId + "] seqNo [" + docIdAndSeqNo.seqNo + "]";
        return result;
    }

    /**
     * Load the version for the uid from the reader, returning<ul>
     * <li>{@link Versions#NOT_FOUND} if no matching doc exists,
     * <li>the version associated with the provided uid otherwise
     * </ul>
     */
    public static long loadVersion(IndexReader reader, Term term) throws IOException {
        final DocIdAndVersion docIdAndVersion = loadDocIdAndVersion(reader, term);
        return docIdAndVersion == null ? NOT_FOUND : docIdAndVersion.version;
    }
}
