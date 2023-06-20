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

package com.colasoft.opensearch.index.analysis;

import java.util.function.Function;

/**
 * A parser that takes a raw string and returns the parsed data of type T.
 *
 * @param <T> type of parsed data
 */
@FunctionalInterface
public interface CustomMappingRuleParser<T> extends Function<String, T> {

}
