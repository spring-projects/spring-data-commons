/*
 * Copyright 2026-present the original author or authors.
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

import org.junit.platform.commons.annotation.Testable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import org.springframework.data.BenchmarkSettings;
import org.springframework.data.expression.ValueExpressionParser;

/**
 * Benchmarks for {@link ValueExpressionQueryRewriter}.
 *
 * @author Greg Taube
 */
@Testable
@State(Scope.Benchmark)
public class ValueExpressionQueryRewriterBenchmarks extends BenchmarkSettings {

	private final ValueExpressionQueryRewriter rewriter = ValueExpressionQueryRewriter.of(
			ValueExpressionParser.create(), (index, expression) -> "__$synthetic$__" + index,
			(prefix, name) -> prefix + name);

	private final String ordinaryQuery = "select u from User u where u.firstname = :firstname "
			+ "and u.lastname = :lastname and u.active = :active";

	private final String quotedExpressionQuery = "select u from User u where u.firstname = :firstname "
			+ "and u.comment <> ':#{notAnExpression}'";

	private final String expressionQuery = "select u from User u where u.firstname = :#{#user.firstname} "
			+ "and u.lastname = :#{#user.lastname}";

	@Benchmark
	public Object ordinaryQuery() {
		return rewriter.parse(ordinaryQuery);
	}

	@Benchmark
	public Object quotedExpressionQuery() {
		return rewriter.parse(quotedExpressionQuery);
	}

	@Benchmark
	public Object expressionQuery() {
		return rewriter.parse(expressionQuery);
	}
}
