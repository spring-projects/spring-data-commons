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

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Evaluates SpEL expressions as extracted by the SpelExtractor based on parameter information from a method and
 * parameter values from a method call.
 *
 * @author Jens Schauder
 * @author Gerrit Meier
 */
@RequiredArgsConstructor
public class SpelEvaluator {

	private final static SpelExpressionParser PARSER = new SpelExpressionParser();

	@NonNull private final EvaluationContextProvider evaluationContextProvider;
	@NonNull private final Parameters<?, ?> parameters;

	/**
	 * A map from parameter name to SpEL expression as returned by {@link SpelQueryContext.SpelExtractor#parameterNameToSpelMap()}.
	 */
	@NonNull private final Map<String, String> parameterNameToSpelMap;

	/**
	 * Evaluate all the SpEL expressions in {@link #parameterNameToSpelMap} based on values provided as an argument.
	 *
	 * @param values Parameter values. Must not be {@literal null}.
	 * @return a map from parameter name to evaluated value. Guaranteed to be not {@literal null}.
	 */
	public Map<String, Object> evaluate(Object[] values) {

		Assert.notNull(values, "Values must not be null.");

		HashMap<String, Object> spelExpressionResults = new HashMap<>();
		EvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(parameters, values);
		for (Map.Entry<String, String> parameterNameToSpel : parameterNameToSpelMap.entrySet()) {

			Object spElValue = getSpElValue(evaluationContext, parameterNameToSpel.getValue());
			spelExpressionResults.put(parameterNameToSpel.getKey(), spElValue);
		}

		return spelExpressionResults;
	}

	@Nullable
	private Object getSpElValue(EvaluationContext evaluationContext, String expression) {
		return PARSER.parseExpression(expression).getValue(evaluationContext, Object.class);
	}
}
