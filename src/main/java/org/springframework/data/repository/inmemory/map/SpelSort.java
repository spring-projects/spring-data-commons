/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.inmemory.map;

import java.util.Comparator;

import org.springframework.data.util.SpelUtil;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * {@link Comparator} implementation using {@link SpelExpression}.
 * 
 * @author Christoph Strobl
 * @param <T>
 */
public class SpelSort<T> implements Comparator<T> {

	private final String path;
	private SpelExpression expression;

	private boolean asc = true;
	private boolean nullsFirst = true;

	/**
	 * Create new {@link SpelSort} comparing given property path.
	 * 
	 * @param path
	 */
	public SpelSort(String path) {
		this.path = path;
	}

	/**
	 * @return
	 */
	public SpelSort<T> asc() {
		this.asc = true;
		return this;
	}

	/**
	 * @return
	 */
	public SpelSort<T> desc() {
		this.asc = false;
		return this;
	}

	/**
	 * @return
	 */
	public SpelSort<T> nullsFirst() {
		this.nullsFirst = true;
		return this;
	}

	/**
	 * @return
	 */
	public SpelSort<T> nullsLast() {
		this.nullsFirst = false;
		return this;
	}

	protected SpelExpression getExpression() {

		if (this.expression == null) {
			SpelParserConfiguration config = SpelUtil.silentlyCreateParserConfiguration("IMMEDIATE");
			this.expression = new SpelExpressionParser(config).parseRaw(buildExpressionForPath());
		}

		return this.expression;
	}

	protected String buildExpressionForPath() {

		StringBuilder rawExpression = new StringBuilder(
				"new org.springframework.util.comparator.NullSafeComparator(new org.springframework.util.comparator.ComparableComparator(), "
						+ Boolean.toString(this.nullsFirst) + ").compare(");

		rawExpression.append("#arg1?.");
		rawExpression.append(path != null ? path.replace(".", ".?") : "");
		rawExpression.append(",");
		rawExpression.append("#arg2?.");
		rawExpression.append(path != null ? path.replace(".", ".?") : "");
		rawExpression.append(")");

		return rawExpression.toString();
	}

	@Override
	public int compare(T arg1, T arg2) {

		SpelExpression expressionToUse = getExpression();

		expressionToUse.getEvaluationContext().setVariable("arg1", arg1);
		expressionToUse.getEvaluationContext().setVariable("arg2", arg2);

		return expressionToUse.getValue(Integer.class) * (asc ? 1 : -1);
	}

	public String getPath() {
		return path;
	}

}
