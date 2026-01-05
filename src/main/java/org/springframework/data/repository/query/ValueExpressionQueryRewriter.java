/*
 * Copyright 2018-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Range;
import org.springframework.data.domain.Range.Bound;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueEvaluationContextProvider;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.util.Assert;

/**
 * A {@literal ValueExpressionQueryRewriter} is able to detect Value expressions in a query string and to replace them
 * with bind variables.
 * <p>
 * Result of the parse process is a {@link ParsedQuery} which provides the transformed query string. Alternatively and
 * preferred one may provide a {@link QueryMethodValueEvaluationContextAccessor} via
 * {@link #withEvaluationContextAccessor(QueryMethodValueEvaluationContextAccessor)} which will yield the more powerful
 * {@link EvaluatingValueExpressionQueryRewriter}.
 * <p>
 * Typical usage looks like
 *
 * <pre class="code">
 * ValueExpressionQueryRewriter.EvaluatingValueExpressionQueryRewriter rewriter = ValueExpressionQueryRewriter
 * 		.of(valueExpressionParser, (counter, expression) -> String.format("__$synthetic$__%d", counter), String::concat)
 * 		.withEvaluationContextAccessor(evaluationContextProviderFactory);
 *
 * ValueExpressionQueryRewriter.QueryExpressionEvaluator evaluator = rewriter.parse(query, queryMethod.getParameters());
 *
 * evaluator.evaluate(objects).forEach(parameterMap::addValue);
 * </pre>
 *
 * @author Jens Schauder
 * @author Gerrit Meier
 * @author Mark Paluch
 * @since 3.3
 * @see ValueExpression
 */
public class ValueExpressionQueryRewriter {

	private static final Pattern EXPRESSION_PATTERN = Pattern.compile("([:?])([#$]\\{[^}]+})");

	private final ValueExpressionParser expressionParser;

	/**
	 * A function from the index of a Value expression in a query and the actual Value Expression to the parameter name to
	 * be used in place of the Value Expression. A typical implementation is expected to look like
	 * {@code (index, expression) -> "__some_placeholder_" + index}.
	 */
	private final BiFunction<Integer, String, String> parameterNameSource;

	/**
	 * A function from a prefix used to demarcate a Value Expression in a query and a parameter name as returned from
	 * {@link #parameterNameSource} to a {@literal String} to be used as a replacement of the Value Expressions in the
	 * query. The returned value should normally be interpretable as a bind parameter by the underlying persistence
	 * mechanism. A typical implementation is expected to look like {@code (prefix, name) -> prefix + name} or
	 * {@code (prefix, name) -> "{" + name + "}"}.
	 */
	private final BiFunction<String, String, String> replacementSource;

	private ValueExpressionQueryRewriter(ValueExpressionParser expressionParser,
			BiFunction<Integer, String, String> parameterNameSource, BiFunction<String, String, String> replacementSource) {

		Assert.notNull(expressionParser, "ValueExpressionParser must not be null");
		Assert.notNull(parameterNameSource, "Parameter name source must not be null");
		Assert.notNull(replacementSource, "Replacement source must not be null");

		this.parameterNameSource = parameterNameSource;
		this.replacementSource = replacementSource;
		this.expressionParser = expressionParser;
	}

	/**
	 * Creates a new ValueExpressionQueryRewriter using the given {@link ValueExpressionParser} and rewrite functions.
	 *
	 * @param expressionParser the expression parser to use.
	 * @param parameterNameSource function to generate parameter names. Typically, a function of the form
	 *          {@code (index, expression) -> "__some_placeholder_" + index}.
	 * @param replacementSource function to generate replacements. Typically, a concatenation of the prefix and the
	 *          parameter name such as {@code String::concat}.
	 * @return a ValueExpressionQueryRewriter instance to rewrite queries and extract parsed {@link ValueExpression}s.
	 */
	public static ValueExpressionQueryRewriter of(ValueExpressionParser expressionParser,
			BiFunction<Integer, String, String> parameterNameSource, BiFunction<String, String, String> replacementSource) {
		return new ValueExpressionQueryRewriter(expressionParser, parameterNameSource, replacementSource);
	}

	/**
	 * Creates a new EvaluatingValueExpressionQueryRewriter using the given {@link ValueExpressionDelegate} and rewrite
	 * functions.
	 *
	 * @param delegate the ValueExpressionDelegate to use for parsing and to obtain EvaluationContextAccessor from.
	 * @param parameterNameSource function to generate parameter names. Typically, a function of the form
	 *          {@code (index, expression) -> "__some_placeholder_" + index}.
	 * @param replacementSource function to generate replacements. Typically, a concatenation of the prefix and the
	 *          parameter name such as {@code String::concat}.
	 * @return a EvaluatingValueExpressionQueryRewriter instance to rewrite queries and extract parsed
	 *         {@link ValueExpression}s.
	 * @since 3.4
	 */
	public static EvaluatingValueExpressionQueryRewriter of(ValueExpressionDelegate delegate,
			BiFunction<Integer, String, String> parameterNameSource, BiFunction<String, String, String> replacementSource) {
		return of((ValueExpressionParser) delegate, parameterNameSource, replacementSource)
				.withEvaluationContextAccessor(delegate.getEvaluationContextAccessor());
	}

	/**
	 * Parses the query for {@link org.springframework.data.expression.ValueExpression value expressions} using the
	 * pattern:
	 *
	 * <pre>
	 * &lt;prefix&gt;#{&lt;spel&gt;}
	 * &lt;prefix&gt;${&lt;property placeholder&gt;}
	 * </pre>
	 * <p>
	 * with prefix being the character ':' or '?'. Parsing honors quoted {@literal String}s enclosed in single or double
	 * quotation marks.
	 *
	 * @param query a query containing Value Expressions in the format described above. Must not be {@literal null}.
	 * @return A {@link ParsedQuery} which makes the query with Value Expressions replaced by bind parameters and a map
	 *         from bind parameter to Value Expression available. Guaranteed to be not {@literal null}.
	 */
	public ParsedQuery parse(String query) {
		return new ParsedQuery(expressionParser, query);
	}

	/**
	 * Creates a {@link EvaluatingValueExpressionQueryRewriter} from the current one and the given
	 * {@link QueryMethodValueEvaluationContextAccessor}.
	 *
	 * @param accessor must not be {@literal null}.
	 * @return EvaluatingValueExpressionQueryRewriter instance to rewrite and evaluate Value Expressions.
	 */
	public EvaluatingValueExpressionQueryRewriter withEvaluationContextAccessor(
			QueryMethodValueEvaluationContextAccessor accessor) {

		Assert.notNull(accessor, "QueryMethodValueEvaluationContextAccessor must not be null");

		return new EvaluatingValueExpressionQueryRewriter(expressionParser, accessor, parameterNameSource,
				replacementSource);
	}

	/**
	 * An extension of {@link ValueExpressionQueryRewriter} that can create {@link QueryExpressionEvaluator} instances as
	 * it also knows about a {@link QueryMethodValueEvaluationContextAccessor}.
	 *
	 * @author Oliver Gierke
	 */
	public static class EvaluatingValueExpressionQueryRewriter extends ValueExpressionQueryRewriter {

		private final QueryMethodValueEvaluationContextAccessor contextProviderFactory;

		/**
		 * Creates a new {@link EvaluatingValueExpressionQueryRewriter} for the given
		 * {@link QueryMethodValueEvaluationContextAccessor}, parameter name source and replacement source.
		 *
		 * @param factory must not be {@literal null}.
		 * @param parameterNameSource must not be {@literal null}.
		 * @param replacementSource must not be {@literal null}.
		 */
		private EvaluatingValueExpressionQueryRewriter(ValueExpressionParser expressionParser,
				QueryMethodValueEvaluationContextAccessor factory, BiFunction<Integer, String, String> parameterNameSource,
				BiFunction<String, String, String> replacementSource) {

			super(expressionParser, parameterNameSource, replacementSource);

			this.contextProviderFactory = factory;
		}

		/**
		 * Parses the query for Value Expressions using the pattern:
		 *
		 * <pre>
		 * &lt;prefix&gt;#{&lt;spel&gt;}
		 * &lt;prefix&gt;${&lt;property placeholder&gt;}
		 * </pre>
		 * <p>
		 * with prefix being the character ':' or '?'. Parsing honors quoted {@literal String}s enclosed in single or double
		 * quotation marks.
		 *
		 * @param query a query containing Value Expressions in the format described above. Must not be {@literal null}.
		 * @param parameters a {@link Parameters} instance describing query method parameters
		 * @return A {@link QueryExpressionEvaluator} which allows to evaluate the Value Expressions.
		 */
		public QueryExpressionEvaluator parse(String query, Parameters<?, ?> parameters) {
			return new QueryExpressionEvaluator(contextProviderFactory.create(parameters), parse(query));
		}
	}

	/**
	 * Parses a query string, identifies the contained Value expressions, replaces them with bind parameters and offers a
	 * {@link Map} from those bind parameters to the value expression.
	 * <p>
	 * The parser detects quoted parts of the query string and does not detect value expressions inside such quoted parts
	 * of the query.
	 *
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	public class ParsedQuery {

		private static final int PREFIX_GROUP_INDEX = 1;
		private static final int EXPRESSION_GROUP_INDEX = 2;

		private final String query;
		private final Map<String, ValueExpression> expressions;
		private final QuotationMap quotations;

		/**
		 * Creates a ExpressionDetector from a query String.
		 *
		 * @param query must not be {@literal null}.
		 */
		ParsedQuery(ValueExpressionParser parser, String query) {

			Assert.notNull(query, "Query must not be null");

			Map<String, ValueExpression> expressions = new HashMap<>();
			Matcher matcher = EXPRESSION_PATTERN.matcher(query);
			StringBuilder resultQuery = new StringBuilder();
			QuotationMap quotedAreas = new QuotationMap(query);

			int expressionCounter = 0;
			int matchedUntil = 0;

			while (matcher.find()) {

				if (quotedAreas.isQuoted(matcher.start())) {
					resultQuery.append(query, matchedUntil, matcher.end());

				} else {

					String expressionString = matcher.group(EXPRESSION_GROUP_INDEX);
					String prefix = matcher.group(PREFIX_GROUP_INDEX);

					String parameterName = parameterNameSource.apply(expressionCounter, expressionString);
					String replacement = replacementSource.apply(prefix, parameterName);

					resultQuery.append(query, matchedUntil, matcher.start());
					resultQuery.append(replacement);

					expressions.put(parameterName, parser.parse(expressionString));
					expressionCounter++;
				}

				matchedUntil = matcher.end();
			}

			resultQuery.append(query.substring(matchedUntil));

			this.expressions = Collections.unmodifiableMap(expressions);
			this.query = resultQuery.toString();

			// recreate quotation map based on rewritten query.
			this.quotations = new QuotationMap(this.query);
		}

		/**
		 * The query with all the Value Expressions replaced with bind parameters.
		 *
		 * @return Guaranteed to be not {@literal null}.
		 */
		public String getQueryString() {
			return query;
		}

		/**
		 * Return whether the {@link #getQueryString() query} at {@code index} is quoted.
		 *
		 * @param index
		 * @return {@literal true} if quoted; {@literal false} otherwise.
		 */
		public boolean isQuoted(int index) {
			return quotations.isQuoted(index);
		}

		/**
		 * @param name
		 * @return
		 * @since 4.0
		 */
		public boolean hasExpression(String name) {
			return expressions.get(name) != null;
		}

		@Nullable
		public ValueExpression getParameter(String name) {
			return expressions.get(name);
		}

		/**
		 * Returns the required {@link ValueExpression} for the given name or throws an {@link IllegalArgumentException} if
		 * the parameter is not present.
		 *
		 * @param name
		 * @return
		 * @throws IllegalArgumentException if the parameter is not present.
		 * @since 4.0
		 */
		public ValueExpression getRequiredParameter(String name) {

			ValueExpression valueExpression = getParameter(name);

			if (valueExpression == null) {
				throw new IllegalArgumentException("No ValueExpression with name '%s' found in query".formatted(name));
			}

			return valueExpression;
		}

		/**
		 * Returns the number of expressions in this extractor.
		 *
		 * @return the number of expressions in this extractor.
		 */
		public int size() {
			return expressions.size();
		}

		/**
		 * Returns whether the query contains Value Expressions.
		 *
		 * @return {@literal true} if the query contains Value Expressions.
		 */
		public boolean hasParameterBindings() {
			return !expressions.isEmpty();
		}

		/**
		 * A {@literal Map} from parameter name to Value Expression.
		 *
		 * @return Guaranteed to be not {@literal null}.
		 */
		public Map<String, ValueExpression> getParameterMap() {
			return expressions;
		}

	}

	/**
	 * Value object to analyze a {@link String} to determine the parts of the {@link String} that are quoted and offers an
	 * API to query that information.
	 *
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @since 2.1
	 */
	static class QuotationMap {

		private static final Collection<Character> QUOTING_CHARACTERS = List.of('"', '\'');

		private final List<Range<Integer>> quotedRanges = new ArrayList<>();

		/**
		 * Creates a new {@link QuotationMap} for the query.
		 *
		 * @param query can be {@literal null}.
		 */
		public QuotationMap(@Nullable String query) {

			if (query == null) {
				return;
			}

			Character inQuotation = null;
			int start = 0;

			for (int i = 0; i < query.length(); i++) {

				char currentChar = query.charAt(i);

				if (QUOTING_CHARACTERS.contains(currentChar)) {

					if (inQuotation == null) {

						inQuotation = currentChar;
						start = i;

					} else if (currentChar == inQuotation) {

						inQuotation = null;

						quotedRanges.add(Range.from(Bound.inclusive(start)).to(Bound.inclusive(i)));
					}
				}
			}

			if (inQuotation != null) {
				throw new IllegalArgumentException(
						String.format("The string <%s> starts a quoted range at %d, but never ends it.", query, start));
			}
		}

		/**
		 * Checks if a given index is within a quoted range.
		 *
		 * @param index to check if it is part of a quoted range.
		 * @return whether the query contains a quoted range at {@literal index}.
		 */
		public boolean isQuoted(int index) {
			return quotedRanges.stream().anyMatch(r -> r.contains(index));
		}
	}

	/**
	 * Evaluates Value expressions as detected by {@link ParsedQuery} based on parameter information from a method and
	 * parameter values from a method call.
	 *
	 * @author Jens Schauder
	 * @author Gerrit Meier
	 * @author Oliver Gierke
	 * @see ValueExpressionQueryRewriter#parse(String)
	 */
	public class QueryExpressionEvaluator {

		private final ValueEvaluationContextProvider evaluationContextProvider;
		private final ParsedQuery detector;

		public QueryExpressionEvaluator(ValueEvaluationContextProvider evaluationContextProvider, ParsedQuery detector) {
			this.evaluationContextProvider = evaluationContextProvider;
			this.detector = detector;
		}

		/**
		 * Evaluate all value expressions in {@link ParsedQuery} based on values provided as an argument.
		 *
		 * @param values Parameter values. Must not be {@literal null}.
		 * @return a map from parameter name to evaluated value as of {@link ParsedQuery#getParameterMap()}.
		 */
		public Map<String, @Nullable Object> evaluate(Object[] values) {

			Assert.notNull(values, "Values must not be null.");

			Map<String, ValueExpression> parameterMap = detector.getParameterMap();
			Map<String, @Nullable Object> results = new LinkedHashMap<>(parameterMap.size());

			parameterMap.forEach((parameter, expression) -> results.put(parameter, evaluate(expression, values)));

			return results;
		}

		/**
		 * Returns the query string produced by the intermediate Value Expression collection step.
		 *
		 * @return
		 */
		public String getQueryString() {
			return detector.getQueryString();
		}

		private @Nullable Object evaluate(ValueExpression expression, Object[] values) {

			ValueEvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(values,
					expression.getExpressionDependencies());

			return expression.evaluate(evaluationContext);
		}
	}
}
