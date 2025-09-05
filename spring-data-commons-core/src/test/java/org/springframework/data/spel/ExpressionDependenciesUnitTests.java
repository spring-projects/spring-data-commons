/*
 * Copyright 2020-2025 the original author or authors.
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
package org.springframework.data.spel;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit tests for {@link ExpressionDependencies}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class ExpressionDependenciesUnitTests {

	static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Test // DATACMNS-1108
	void shouldExtractDependencies() {

		var expression = "hasRole('ROLE_ADMIN') ? '%' : principal.emailAddress";

		var spelExpression = (SpelExpression) PARSER.parseExpression(expression);
		var ast = spelExpression.getAST();

		var dependencies = ExpressionDependencies.discover(ast, true);

		assertThat(dependencies).extracting(ExpressionDependencies.ExpressionDependency::getSymbol).containsOnly("hasRole",
				"principal");
	}

	@Test // DATACMNS-1108
	void shouldExtractDependenciesFromMethodCallArgs() {

		var expression = "hasRole(principal.emailAddress)";

		var spelExpression = (SpelExpression) PARSER.parseExpression(expression);
		var ast = spelExpression.getAST();

		var dependencies = ExpressionDependencies.discover(ast, true);

		assertThat(dependencies).extracting(ExpressionDependencies.ExpressionDependency::getSymbol).containsOnly("hasRole",
				"principal");
	}

	@Test // DATACMNS-1108
	void shouldExtractFirstMethodAsDependency() {

		var expression = "hello().hasRole(principal.emailAddress, principal.somethingElse).somethingElse()";

		var spelExpression = (SpelExpression) PARSER.parseExpression(expression);
		var ast = spelExpression.getAST();

		var dependencies = ExpressionDependencies.discover(ast, true);

		assertThat(dependencies).extracting(ExpressionDependencies.ExpressionDependency::getSymbol).containsOnly("hello",
				"principal");
	}

	@Test // DATACMNS-1108
	void shouldExtractAll() {

		var expression = "hello().hasRole(principal.emailAddress, principal.somethingElse).somethingElse()";

		var spelExpression = (SpelExpression) PARSER.parseExpression(expression);
		var ast = spelExpression.getAST();

		var dependencies = ExpressionDependencies.discover(ast, false);

		assertThat(dependencies).extracting(ExpressionDependencies.ExpressionDependency::getSymbol).containsOnly("hello",
				"hasRole", "principal", "emailAddress", "somethingElse");
	}

	@Test // DATACMNS-1108
	void shouldMergeDependencies() {

		var left = ExpressionDependencies.discover(PARSER.parseExpression("hasRole('ROLE_ADMIN')"));
		var right = ExpressionDependencies.discover(PARSER.parseExpression("principal.somethingElse"));

		assertThat(left.mergeWith(right)).extracting(ExpressionDependencies.ExpressionDependency::getSymbol)
				.containsOnly("hasRole", "principal");
	}
}
