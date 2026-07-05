/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.javapoet;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.CodeBlock;

/**
 * Unit tests for {@link LordOfTheStrings}.
 *
 * @author Mark Paluch
 */
class JavaPoetUnitTests {

	@Test // GH-3357
	void shouldConsiderConditionals() {

		assertThat(LordOfTheStrings.builder().addStatement(statementBuilder -> {
			statementBuilder.when(true).then("return $S", "The Return of the King");
			statementBuilder.when(false).then("return $S", "The Two Towers");
		}).build()).hasToString("return \"The Return of the King\";\n");

		assertThat(LordOfTheStrings.builder().addStatement(statementBuilder -> {
			assertThat(statementBuilder.isEmpty()).isTrue();
			statementBuilder.whenNot(true).then("return $S", "The Return of the King");
			assertThat(statementBuilder.isEmpty()).isTrue();

			statementBuilder.whenNot(false).then("return $S", "The Two Towers");
			assertThat(statementBuilder.isEmpty()).isFalse();
		}).build()).hasToString("return \"The Two Towers\";\n");

		assertThat(LordOfTheStrings.builder().addStatement(statementBuilder -> {

			assertThat(statementBuilder.isEmpty()).isTrue();
			statementBuilder.add("foo");
			assertThat(statementBuilder.isEmpty()).isFalse();
		}).build()).hasToString("foo;\n");
	}

	@Test // GH-3357
	void shouldConcatenateCollections() {

		assertThat(LordOfTheStrings.builder().addStatement(statementBuilder -> {
			statementBuilder.addAll(Arrays.asList("foo", "bar"), ", ", it -> CodeBlock.of(it + " $S", it));
		}).build()).hasToString("foo \"foo\", bar \"bar\";\n");

		assertThat(LordOfTheStrings.builder().addStatement(statementBuilder -> {
			statementBuilder.addAll(Arrays.asList("foo", "barrrr"), it -> "" + it.length(),
					it -> CodeBlock.of(it + " $S", it));
		}).build()).hasToString("foo \"foo\"6barrrr \"barrrr\";\n");
	}

	@Test // GH-3357
	void youShallNotPass() {

		// without expecting arguments, the second $L is superfluous.
		assertThatIllegalArgumentException().isThrownBy(() -> LordOfTheStrings.invoke("$L.run($L)", "runnable").build());
	}

	@Test // GH-3357
	void shouldRenderMethodCall() {

		CodeBlock block = LordOfTheStrings.invoke("$L.run()", "runnable").build();
		assertThat(block).hasToString("runnable.run()");
	}

	@Test // GH-3357
	void shouldRenderMethodCallWithArguments() {

		CodeBlock block = LordOfTheStrings.invoke("$L.run($L)", "runnable").argument("foo").build();
		assertThat(block).hasToString("runnable.run(foo)");

		block = LordOfTheStrings.invoke("$L.run($L)", "runnable").argument("foo").argument("bar").build();
		assertThat(block).hasToString("runnable.run(foo, bar)");

		block = LordOfTheStrings.invoke("$L.run($L)", "runnable").argument("$L", "foo").argument("bar").build();
		assertThat(block).hasToString("runnable.run(foo, bar)");

		block = LordOfTheStrings.invoke("$L.run($L)", "runnable").arguments(Arrays.asList("foo", "bar")).build();
		assertThat(block).hasToString("runnable.run(foo, bar)");

		block = LordOfTheStrings.invoke("$L.run($L)", "runnable").arguments(List.of()).build();
		assertThat(block).hasToString("runnable.run()");

		block = LordOfTheStrings.invoke("$L.run($L)", "runnable")
				.arguments(List.of("foo", "bar"), it -> CodeBlock.of("$S", it)).build();
		assertThat(block).hasToString("runnable.run(\"foo\", \"bar\")");
	}

	@Test // GH-3357
	void shouldRenderAssignTo() {

		CodeBlock block = LordOfTheStrings.invoke("$L.run()", "runnable").assignTo("$T result", String.class);
		assertThat(block).hasToString("java.lang.String result = runnable.run()");
	}

	@Test // GH-3357
	void shouldRenderSimpleReturn() {

		CodeBlock block = LordOfTheStrings.returning(Long.class).otherwise("1L").build();
		assertThat(block).hasToString("return 1L");
	}

	@Test // GH-3357
	void shouldRenderConditionalLongReturn() {

		CodeBlock block = LordOfTheStrings.returning(Long.class).whenLong("1L").whenBoxedLong("$T.valueOf(1)", Long.class)
				.otherwise(":-[").build();
		assertThat(block).hasToString("return java.lang.Long.valueOf(1)");

		block = LordOfTheStrings.returning(Long.class).whenBoxedLong("$T.valueOf(1)", Long.class).whenLong("1L")
				.otherwise(":-[").build();
		assertThat(block).hasToString("return java.lang.Long.valueOf(1)");

		block = LordOfTheStrings.returning(long.class).whenBoxedLong("$T.valueOf(1)", Long.class).whenLong("1L")
				.otherwise(":-[").build();
		assertThat(block).hasToString("return 1L");

		block = LordOfTheStrings.returning(Long.class).whenBoxed(Long.class, "$T.valueOf(1)", Long.class).otherwise(":-[")
				.build();
		assertThat(block).hasToString("return java.lang.Long.valueOf(1)");

		block = LordOfTheStrings.returning(Long.class).whenBoxed(long.class, "$T.valueOf(1)", Long.class).otherwise(":-[")
				.build();
		assertThat(block).hasToString("return java.lang.Long.valueOf(1)");
	}

	@Test // GH-3357
	void shouldRenderConditionalIntReturn() {

		CodeBlock block = LordOfTheStrings.returning(Integer.class).whenBoxed(long.class, "$T.valueOf(1)", Long.class)
				.otherwise(":-[").build();
		assertThat(block).hasToString("return :-[");

		block = LordOfTheStrings.returning(Integer.class).whenBoxedInteger("$T.valueOf(1)", Integer.class).otherwise(":-[")
				.build();
		assertThat(block).hasToString("return java.lang.Integer.valueOf(1)");

		block = LordOfTheStrings.returning(int.class).whenBoxedInteger("$T.valueOf(1)", Integer.class).whenInt("1")
				.otherwise(":-[").build();
		assertThat(block).hasToString("return 1");
	}

	@Test // GH-3357
	void shouldRenderConditionalBooleanReturn() {

		CodeBlock block = LordOfTheStrings.returning(boolean.class).whenBoolean("$L", true).otherwise(":-[").build();
		assertThat(block).hasToString("return true");

		block = LordOfTheStrings.returning(Boolean.class).whenBoolean("$L", true).otherwise(":-[").build();
		assertThat(block).hasToString("return true");
	}

	@Test // GH-3357
	void shouldRenderConditionalNumericReturn() {

		CodeBlock block = LordOfTheStrings.returning(boolean.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return :-[");

		block = LordOfTheStrings.returning(long.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.longValue() : 0L");

		block = LordOfTheStrings.returning(Long.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.longValue() : null");

		block = LordOfTheStrings.returning(int.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.intValue() : 0");

		block = LordOfTheStrings.returning(Integer.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.intValue() : null");

		block = LordOfTheStrings.returning(Short.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.shortValue() : null");

		block = LordOfTheStrings.returning(short.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.shortValue() : 0");

		block = LordOfTheStrings.returning(Byte.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.byteValue() : null");

		block = LordOfTheStrings.returning(Float.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.floatValue() : null");

		block = LordOfTheStrings.returning(float.class).number("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable != null ? someNumericVariable.floatValue() : 0f");
	}

	@Test // GH-3357
	void shouldRenderConditionalSafeNumericReturn() {

		CodeBlock block = LordOfTheStrings.returning(boolean.class).nonNullableNumber("someNumericVariable")
				.otherwise(":-[").build();
		assertThat(block).hasToString("return :-[");

		block = LordOfTheStrings.returning(long.class).nonNullableNumber("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable.longValue()");

		block = LordOfTheStrings.returning(Long.class).nonNullableNumber("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable.longValue()");

		block = LordOfTheStrings.returning(Integer.class).nonNullableNumber("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable.intValue()");

		block = LordOfTheStrings.returning(short.class).nonNullableNumber("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable.shortValue()");

		block = LordOfTheStrings.returning(byte.class).nonNullableNumber("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable.byteValue()");

		block = LordOfTheStrings.returning(Double.class).nonNullableNumber("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable.doubleValue()");

		block = LordOfTheStrings.returning(Float.class).nonNullableNumber("someNumericVariable").otherwise(":-[").build();
		assertThat(block).hasToString("return someNumericVariable.floatValue()");
	}

	@Test // GH-3357
	void shouldRenderConditionalOptional() {

		CodeBlock block = LordOfTheStrings.returning(Optional.class).optional(CodeBlock.of("$S", "foo")).build();
		assertThat(block).hasToString("return java.util.Optional.ofNullable(\"foo\")");

		block = LordOfTheStrings.returning(String.class).optional(CodeBlock.of("$S", "foo")).build();
		assertThat(block).hasToString("return \"foo\"");
	}

}
