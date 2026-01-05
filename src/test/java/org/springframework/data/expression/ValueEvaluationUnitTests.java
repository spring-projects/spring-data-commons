/*
 * Copyright 2024-present the original author or authors.
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
package org.springframework.data.expression;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * Unit tests for {@link ValueExpression} and its parsing.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class ValueEvaluationUnitTests {

	private ValueExpressionParser parser = ValueExpressionParser.create();
	private ValueEvaluationContext evaluationContext;

	@BeforeEach
	void setUp() {

		MapPropertySource propertySource = new MapPropertySource("map", Map.of("env.key.one", "value", "env.key.two", 42L));
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(propertySource);

		record MyRecord(String foo, @Nullable String bar) {

		}

		this.evaluationContext = new ValueEvaluationContext() {
			@Override
			public Environment getEnvironment() {
				return environment;
			}

			@Override
			public EvaluationContext getEvaluationContext() {
				StandardEvaluationContext context = new StandardEvaluationContext();
				context.setVariable("contextVar", "contextVal");
				context.setVariable("nullVar", null);
				context.setVariable("someRecord", new MyRecord("foo", null));

				return context;
			}
		};
	}

	@Test // GH-2369
	void shouldParseAndEvaluateExpressions() {

		assertThat(eval("foo")).isEqualTo("foo");
		assertThat(eval("${env.key.one}")).isEqualTo("value");
		assertThat(eval("${env.key.two}")).isEqualTo("42");
		assertThat(eval("${env.key.one")).isEqualTo("${env.key.one");
		assertThat(eval("#{'foo'}-")).isEqualTo("foo-");
		assertThat(eval("#{'fo\"o'}-")).isEqualTo("fo\"o-");
		assertThat(eval("#{\"foo\"}-")).isEqualTo("foo-");
		assertThat(eval("#{\"fo'o\"}-")).isEqualTo("fo'o-");
		assertThat(eval("${env.key.one}-")).isEqualTo("value-");
		assertThat(eval("#{#contextVar}")).isEqualTo("contextVal");
		assertThat(eval("${env.does.not.exist:some-default}-")).isEqualTo("some-default-");

		assertThatExceptionOfType(EvaluationException.class).isThrownBy(() -> eval("${env.does.not.exist}-"))
				.withMessageContaining("Could not resolve placeholder 'env.does.not.exist'");
	}

	@Test // GH-3169
	void shouldReturnValueType() {

		assertThat(getValueType("foo")).isEqualTo(String.class);
		assertThat(getValueType("${env.key.one}")).isEqualTo(String.class);
		assertThat(getValueType("#{'foo'}")).isEqualTo(String.class);
		assertThat(getValueType("#{1+1}")).isEqualTo(Integer.class);
		assertThat(getValueType("#{null}")).isNull();
		assertThat(getValueType("#{#contextVar}")).isEqualTo(String.class);
		assertThat(getValueType("#{#nullVar}")).isNull();
		assertThat(getValueType("#{#someRecord.foo}")).isEqualTo(String.class);
		assertThat(getValueType("#{#someRecord.bar}")).isEqualTo(String.class);
	}

	@Test // GH-2369
	void shouldParseLiteral() {

		ValueExpressionParser parser = ValueExpressionParser.create();

		assertThat(parser.parse("#{'foo'}-${key.one}").isLiteral()).isFalse();
		assertThat(parser.parse("foo").isLiteral()).isTrue();
		assertThat(parser.parse("#{'foo'}").isLiteral()).isFalse();
		assertThat(parser.parse("${key.one}").isLiteral()).isFalse();
	}

	@Test // GH-2369
	void shouldParseCompoundExpression() {

		assertThat(eval("#{'foo'}-")).isEqualTo("foo-");
		assertThat(eval("#{'fo\"o'}-")).isEqualTo("fo\"o-");
		assertThat(eval("#{\"foo\"}-")).isEqualTo("foo-");
		assertThat(eval("#{\"fo'o\"}-")).isEqualTo("fo'o-");
		assertThat(eval("${env.key.one}-")).isEqualTo("value-");
		assertThat(eval("${env.key.one}-and-some-#{'foo'}-#{#contextVar}")).isEqualTo("value-and-some-foo-contextVal");
	}

	@Test // GH-2369
	void shouldRaiseParseException() {

		assertThatExceptionOfType(ParseException.class).isThrownBy(() -> eval("#{'foo'}-${key.one")).withMessageContaining(
				"Expression [#{'foo'}-${key.one] @9: No ending suffix '}' for expression starting at character 9: ${key.one");

		assertThatExceptionOfType(ParseException.class).isThrownBy(() -> eval("#{'foo'}-${env.key.one"));
		assertThatExceptionOfType(ParseException.class).isThrownBy(() -> eval("#{'foo'"));

		assertThatExceptionOfType(ParseException.class).isThrownBy(() -> eval("#{#foo - ${numeric.key}}"))
				.withMessageContaining("[#foo - ${numeric.key}]");

	}

	@Test // GH-2369
	void shouldParseQuoted() {

		assertThat(eval("#{(1+1) + '-foo'+\"-bar\"}")).isEqualTo("2-foo-bar");
		assertThat(eval("#{(1+1) + '-foo'+\"-bar}\"}")).isEqualTo("2-foo-bar}");
		assertThat(eval("#{(1+1) + '-foo}'}")).isEqualTo("2-foo}");
		assertThat(eval("#{(1+1) + '-foo\\}'}")).isEqualTo("2-foo\\}");
		assertThat(eval("#{(1+1) + \"-foo'}\" + '-bar}'}")).isEqualTo("2-foo'}-bar}");
	}

	@SuppressWarnings("DataFlowIssue")
	private String eval(String expressionString) {
		return (String) parser.parse(expressionString).evaluate(evaluationContext);
	}

	@Nullable
	private Class<?> getValueType(String expressionString) {
		return parser.parse(expressionString).getValueType(evaluationContext);
	}

}
