/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.query.parser;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Source of {@link SpelExtractor} encapsulating configuration often common for all queries.
 *
 * @author Jens Schauder
 * @author Gerrit Meier
 */
@RequiredArgsConstructor
public class SpelQueryContext {

	private final static String SPEL_PATTERN_STRING = "([:?])#\\{([^}]+)}";
	private final static Pattern SPEL_PATTERN = Pattern.compile(SPEL_PATTERN_STRING);

	/**
	 * A function from the index of a SpEL expression in a query and the actual SpEL expression to the parameter name to
	 * be used in place of the SpEL expression. A typical implementation is expected to look like
	 * <code>(index, spel) -> "__some_placeholder_" + index</code>
	 */
	@NonNull private final BiFunction<Integer, String, String> parameterNameSource;

	/**
	 * A function from a prefix used to demarcate a SpEL expression in a query and a parameter name as returned from
	 * {@link #parameterNameSource} to a {@literal String} to be used as a replacement of the SpEL in the query. The
	 * returned value should normally be interpretable as a bind parameter by the underlying persistence mechanism. A
	 * typical implementation is expected to look like <code>(prefix, name) -> prefix + name</code> or
	 * <code>(prefix, name) -> "{" + name + "}"</code>
	 */
	@NonNull private final BiFunction<String, String, String> replacementSource;

	/**
	 * Parses the query for SpEL expressions using the pattern
	 * 
	 * <pre>
	 * &lt;prefix&gt;#{&lt;spel&gt;}
	 * </pre>
	 * 
	 * with prefix being the character ':' or '?'. Parsing honors quoted {@literal String}s enclosed in single or double
	 * quotation marks.
	 *
	 * @param query a query containing SpEL expressions in the format described above. Must not be {@literal null}.
	 * @return A {@link SpelExtractor} which makes the query with SpEL expressions replaced by bind parameters and a map
	 *         from bind parameter to SpEL expression available. Guaranteed to be not {@literal null}.
	 */
	public SpelExtractor parse(String query) {
		return new SpelExtractor(query);
	}

	/**
	 * Parses a query string, identifies the contained SpEL expressions, replaces them with bind parameters and offers a
	 * {@link Map} from those bind parameters to the spel expression.
	 * <p>
	 * The parser detects quoted parts of the query string and does not detect SpEL expressions inside such quoted parts
	 * of the query.
	 *
	 * @author Jens Schauder
	 */
	public class SpelExtractor {

		private static final int PREFIX_GROUP_INDEX = 1;
		private static final int EXPRESSION_GROUP_INDEX = 2;

		private final String query;
		private final Map<String, String> expressions;

		/**
		 * Creates a SpelExtractor from a query String.
		 * 
		 * @param query Must not be {@literal null}.
		 */
		private SpelExtractor(String query) {

			Assert.notNull(query, "Query must not be null");

			HashMap<String, String> expressions = new HashMap<>();

			Matcher matcher = SPEL_PATTERN.matcher(query);

			StringBuilder resultQuery = new StringBuilder();

			QuotationMap quotedAreas = new QuotationMap(query);

			int expressionCounter = 0;
			int matchedUntil = 0;

			while (matcher.find()) {

				if (quotedAreas.isQuoted(matcher.start())) {

					resultQuery.append(query.substring(matchedUntil, matcher.end()));

				} else {

					String spelExpression = matcher.group(EXPRESSION_GROUP_INDEX);
					String prefix = matcher.group(PREFIX_GROUP_INDEX);

					String parameterName = parameterNameSource.apply(expressionCounter, spelExpression);
					String replacement = replacementSource.apply(prefix, parameterName);

					resultQuery.append(query.substring(matchedUntil, matcher.start()));
					resultQuery.append(replacement);

					expressions.put(parameterName, spelExpression);
					expressionCounter++;
				}

				matchedUntil = matcher.end();
			}

			resultQuery.append(query.substring(matchedUntil));

			this.expressions = Collections.unmodifiableMap(expressions);
			this.query = resultQuery.toString();
		}

		/**
		 * The query with all the SpEL expressions replaced with bind parameters.
		 *
		 * @return Guaranteed to be not {@literal null}.
		 */
		public String query() {
			return query;
		}

		/**
		 * A {@literal Map} from parameter name to SpEL expression.
		 * 
		 * @return Guaranteed to be not {@literal null}.
		 */
		public Map<String, String> parameterNameToSpelMap() {
			return expressions;
		}
	}
}
