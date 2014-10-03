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

import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * {@link Comparator} implementation using {@link SpelExpression}.
 * 
 * @author Christoph Strobl
 * @param <T>
 */
public class SpelSort<T> implements Comparator<T> {

	boolean asc = true;
	SpelExpression expression;

	/**
	 * Create new {@link SpelSort} comparing given property path.
	 * 
	 * @param path
	 */
	public SpelSort(String path) {
		this.expression = new SpelExpressionParser().parseRaw(buildExpressionForPath(path));
	}

	public SpelSort(SpelExpression expression) {
		this.expression = expression;
	}

	public SpelSort<T> asc() {
		this.asc = true;
		return this;
	}

	public SpelSort<T> desc() {
		this.asc = false;
		return this;
	}

	protected String buildExpressionForPath(String path) {

		StringBuilder rawExpression = new StringBuilder(
				"new org.springframework.util.comparator.NullSafeComparator(new org.springframework.util.comparator.ComparableComparator(), true).compare(");

		rawExpression.append("#arg1?.");
		rawExpression.append(path != null ? path.replace(".", ".?") : "");
		rawExpression.append(",");
		rawExpression.append("#arg2?.");
		rawExpression.append(path != null ? path.replace(".", ".?") : "");
		rawExpression.append(")");
		return rawExpression.toString();
	}

	@Override
	public int compare(T o1, T o2) {

		expression.getEvaluationContext().setVariable("arg1", o1);
		expression.getEvaluationContext().setVariable("arg2", o2);

		return expression.getValue(Integer.class) * (asc ? 1 : -1);
	}

}
