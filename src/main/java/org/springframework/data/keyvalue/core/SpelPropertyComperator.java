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
package org.springframework.data.keyvalue.core;

import java.util.Comparator;

import org.springframework.data.keyvalue.core.spel.SpelExpressionFactory;
import org.springframework.expression.spel.standard.SpelExpression;

/**
 * {@link Comparator} implementation using {@link SpelExpression}.
 *
 * @author Christoph Strobl
 * @since 1.10
 * @param <T>
 */
public class SpelPropertyComperator<T> implements Comparator<T> {

	private final String path;
	private SpelExpression expression;

	private boolean asc = true;
	private boolean nullsFirst = true;

	/**
	 * Create new {@link SpelPropertyComperator} comparing given property path.
	 * 
	 * @param path
	 */
	public SpelPropertyComperator(String path) {
		this.path = path;
	}

	/**
	 * Sort {@literal ascending}.
	 * 
	 * @return
	 */
	public SpelPropertyComperator<T> asc() {
		this.asc = true;
		return this;
	}

	/**
	 * Sort {@literal descending}.
	 * 
	 * @return
	 */
	public SpelPropertyComperator<T> desc() {
		this.asc = false;
		return this;
	}

	/**
	 * Sort {@literal null} values first.
	 * 
	 * @return
	 */
	public SpelPropertyComperator<T> nullsFirst() {
		this.nullsFirst = true;
		return this;
	}

	/**
	 * Sort {@literal null} values last.
	 * 
	 * @return
	 */
	public SpelPropertyComperator<T> nullsLast() {
		this.nullsFirst = false;
		return this;
	}

	/**
	 * Parse values to {@link SpelExpression}
	 * 
	 * @return
	 */
	protected SpelExpression getExpression() {

		if (this.expression == null) {
			this.expression = SpelExpressionFactory.parseRaw(buildExpressionForPath());
		}

		return this.expression;
	}

	/**
	 * Create the expression raw value.
	 * 
	 * @return
	 */
	protected String buildExpressionForPath() {

		StringBuilder rawExpression = new StringBuilder(
				"new org.springframework.util.comparator.NullSafeComparator(new org.springframework.util.comparator.ComparableComparator(), "
						+ Boolean.toString(this.nullsFirst) + ").compare(");

		rawExpression.append("#arg1?.");
		rawExpression.append(path != null ? path.replace(".", "?.") : "");
		rawExpression.append(",");
		rawExpression.append("#arg2?.");
		rawExpression.append(path != null ? path.replace(".", "?.") : "");
		rawExpression.append(")");

		return rawExpression.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(T arg1, T arg2) {

		SpelExpression expressionToUse = getExpression();

		expressionToUse.getEvaluationContext().setVariable("arg1", arg1);
		expressionToUse.getEvaluationContext().setVariable("arg2", arg2);

		return expressionToUse.getValue(Integer.class) * (asc ? 1 : -1);
	}

	/**
	 * Get dot path to property.
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}

}
