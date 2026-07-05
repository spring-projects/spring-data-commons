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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.javapoet.CodeBlock;
import org.springframework.lang.CheckReturnValue;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class for creating Java code blocks using a fluent API. This class provides a structured and extensible
 * programming model to simplify the creation of method calls, return statements, and complex code structures on top of
 * JavaPoet. It is designed to reduce conditional nesting and improve readability in code generation scenarios.
 * <p>
 * This class introduces additional abstractions such as {@link CodeBlockBuilder}, {@link InvocationBuilder}, and
 * {@link TypedReturnBuilder} to facilitate the construction of dynamic code blocks. These abstractions enable
 * developers to create code with conditional logic, argument concatenation, and control flow in a declarative and
 * intuitive manner.
 * <p>
 * This class is intended for internal use within the framework and is not meant to be used directly by application
 * developers.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public abstract class LordOfTheStrings {

	/**
	 * Create a new {@code CodeBlockBuilder} instance.
	 *
	 * @return a new {@code CodeBlockBuilder}.
	 */
	public static CodeBlockBuilder builder() {
		return new CodeBlockBuilder(CodeBlock.builder());
	}

	/**
	 * Create a new {@code CodeBlockBuilder} instance using the given {@link CodeBlock.Builder}.
	 *
	 * @param builder the {@link CodeBlock.Builder} to use.
	 * @return a new {@code CodeBlockBuilder}.
	 */
	public static CodeBlockBuilder builder(CodeBlock.Builder builder) {
		return new CodeBlockBuilder(builder);
	}

	/**
	 * Create a new {@code CodeBlockBuilder} instance with an initial format and arguments.
	 *
	 * @param format the format string.
	 * @param args the arguments for the format string.
	 * @return a new initialized {@code CodeBlockBuilder}.
	 */
	public static CodeBlockBuilder builder(String format, @Nullable Object... args) {
		return new CodeBlockBuilder(CodeBlock.builder().add(format, args));
	}

	/**
	 * Create a {@link InvocationBuilder} for building method invocation code.
	 * <p>
	 * The given {@code methodCall} may contain format specifiers as defined in Java Poet. It must additionally contain a
	 * format specifier (last position) that is used to expand the method arguments when intending to provide arguments,
	 * for example:
	 *
	 * <pre class="code">
	 * Sort sort = …;
	 * MethodCallBuilder method = PoetrySlam.method("$T.by($L)", Sort.class);
	 *
	 * method.arguments(sort, (order, builder) -> {
	 * 	builder.add("$T.asc($S)", Sort.Order.class, order.getProperty());
	 * });
	 *
	 * method.build();
	 * </pre>
	 *
	 * The presence of arguments is detected when calling any {@link InvocationBuilder#argument} or
	 * {@link InvocationBuilder#arguments} method. Providing an empty {@link CodeBlock} or {@link Iterable} activates
	 * argument processing for easier handling when calling this static method. Note that the argument placeholder in
	 * {@code methodCall} must be omitted if no arguments are added.
	 *
	 * @param methodCall the invocation (or method call) format string.
	 * @param arguments the arguments for the method call.
	 * @return a new {@code MethodCallBuilder}.
	 */
	public static InvocationBuilder invoke(String methodCall, Object... arguments) {
		return new InvocationBuilder(methodCall, arguments);
	}

	/**
	 * Create a builder for a return statement targeting the given return type. Any formats provided to
	 * {@link ReturnBuilderSupport} must not contain the {@code return} keyword as this will be included when building the
	 * resulting {@link CodeBlock}, for example:
	 *
	 * <pre class="code">
	 * Method method = …;
	 * CodeBlock block = LordOfTheStrings.returning(ResolvableType.forMethodReturnType(method))
	 *     .whenBoxedLong("$T.valueOf(1)", Long.class)
	 *     .whenLong("1L")
	 *     .otherwise("0L")
	 *     .build();
	 * </pre>
	 *
	 * @param returnType the method return type.
	 * @return a new {@code ReturnStatementBuilder}.
	 */
	public static TypedReturnBuilder returning(ResolvableType returnType) {
		return new TypedReturnBuilder(returnType);
	}

	/**
	 * Create a builder for a return statement targeting the given return type. Any formats provided to
	 * {@link ReturnBuilderSupport} must not contain the {@code return} keyword as this will be included when building the
	 * resulting {@link CodeBlock}, for example:
	 *
	 * <pre class="code">
	 * Method method = …;
	 * CodeBlock block = LordOfTheStrings.returning(method.getReturnType())
	 *     .whenBoxedLong("$T.valueOf(1)", Long.class)
	 *     .whenLong("1L")
	 *     .otherwise("0L")
	 *     .build();
	 * </pre>
	 *
	 * @param returnType the method return type.
	 * @return a new {@code ReturnStatementBuilder}.
	 */
	public static TypedReturnBuilder returning(Class<?> returnType) {
		return new TypedReturnBuilder(ResolvableType.forType(returnType));
	}

	private LordOfTheStrings() {
		// you shall not instantiate
	}

	/**
	 * Builder to create method invocation code supporting argument concatenation.
	 */
	public static class InvocationBuilder {

		private final String name;
		private final List<Object> nameArguments;
		private final List<CodeTuple> arguments = new ArrayList<>();
		private boolean hasArguments = false;

		InvocationBuilder(String name, Object... arguments) {
			this.name = name;
			this.nameArguments = List.of(arguments);
		}

		/**
		 * Add a single argument as literal to the method call.
		 *
		 * @param argument the argument to add.
		 * @return {@code this} builder.
		 */
		@Contract("null ->fail; _ -> this")
		public InvocationBuilder argument(String argument) {

			Assert.hasText(argument, "Argument must not be null or empty");
			return argument("$L", argument);
		}

		/**
		 * Add multiple arguments to the method call creating a literal for each argument.
		 *
		 * @param arguments the collection of arguments to add.
		 * @return {@code this} builder.
		 */
		@Contract("_ -> this")
		public InvocationBuilder arguments(Iterable<?> arguments) {

			this.hasArguments = true;

			for (Object argument : arguments) {
				argument("$L", argument);
			}

			return this;
		}

		/**
		 * Add multiple arguments to the method call, applying a builder customizer for each argument.
		 *
		 * @param arguments the iterable of arguments to add.
		 * @param consumer the consumer to apply to each argument.
		 * @param <T> the type of the arguments.
		 * @return {@code this} builder.
		 */
		@Contract("_, _ -> this")
		public <T> InvocationBuilder arguments(Iterable<? extends T> arguments, Function<? super T, CodeBlock> consumer) {

			this.hasArguments = true;

			for (T argument : arguments) {
				argument(consumer.apply(argument));
			}

			return this;
		}

		/**
		 * Add a {@link CodeBlock} as an argument to the method call.
		 *
		 * @param argument the {@link CodeBlock} to add.
		 * @return {@code this} builder.
		 */
		@Contract("null -> fail; _ -> this")
		public InvocationBuilder argument(CodeBlock argument) {

			Assert.notNull(argument, "CodeBlock must not be null");

			this.hasArguments = true;

			if (argument.isEmpty()) {
				return this;
			}

			return argument("$L", argument);
		}

		/**
		 * Add a formatted argument to the method call.
		 *
		 * @param format the format string.
		 * @param args the arguments for the format string.
		 * @return {@code this} builder.
		 */
		@Contract("null, _ -> fail; _, _ -> this")
		public InvocationBuilder argument(String format, @Nullable Object... args) {

			Assert.hasText(format, "Format must not be null or empty");

			this.hasArguments = true;
			this.arguments.add(new CodeTuple(format, args));
			return this;
		}

		/**
		 * Build the {@link CodeBlock} representing the method call. The resulting CodeBlock can be used inline or as a
		 * {@link CodeBlock.Builder#addStatement(CodeBlock) statement}.
		 *
		 * @return the constructed {@link CodeBlock}.
		 */
		@CheckReturnValue
		public CodeBlock build() {

			CodeBlock.Builder builder = CodeBlock.builder();
			buildCall(builder);
			return builder.build();
		}

		/**
		 * Build the {@link CodeBlock} representing the method call and assign it to the given variable, for example:
		 *
		 * <pre class="code">
		 * CodeBlock.Builder builder = …;
		 * InvocationBuilder invoke = LordOfTheStrings.invoke("getJdbcOperations().update($L)", …);
		 * builder.addStatement(invoke.assignTo("int $L", result));
		 * </pre>
		 *
		 * The resulting CodeBlock should be used as {@link CodeBlock.Builder#addStatement(CodeBlock) statement}.
		 *
		 * @param format the format string for the assignment.
		 * @param args the arguments for the format string.
		 * @return the constructed {@link CodeBlock}.
		 */
		@CheckReturnValue
		public CodeBlock assignTo(String format, @Nullable Object... args) {

			CodeBlock.Builder builder = CodeBlock.builder();

			builder.add(format.trim() + " = ", args);
			buildCall(builder);

			return builder.build();
		}

		private void buildCall(CodeBlock.Builder builder) {

			boolean first = true;

			CodeBlock.Builder argsBuilder = CodeBlock.builder();
			for (CodeTuple argument : arguments) {

				if (first) {
					first = false;
				} else {
					argsBuilder.add(", ");
				}

				argsBuilder.add(argument.format(), argument.args());
			}

			List<Object> allArguments = new ArrayList<>(nameArguments);

			if (hasArguments) {
				allArguments.add(argsBuilder.build());
			}

			builder.add(name, allArguments.toArray());
		}
	}

	/**
	 * An extended variant of {@link CodeBlock.Builder} that supports building statements in a fluent way and extended for
	 * functional {@link #addStatement(Consumer) statement creation}.
	 * <p>
	 * This builder provides additional methods for creating and managing code blocks, including support for control flow,
	 * named arguments, and conditional statements. It is designed to enhance the readability and flexibility of code
	 * block construction.
	 * <p>
	 * Use this builder to create complex code structures in a fluent and intuitive manner.
	 *
	 * @see CodeBlock.Builder
	 */
	public static class CodeBlockBuilder {

		private final CodeBlock.Builder builder;

		CodeBlockBuilder(CodeBlock.Builder builder) {
			this.builder = builder;
		}

		/**
		 * Determine whether this builder is empty.
		 *
		 * @return {@code true} if the builder is empty; {@code false} otherwise.
		 * @see CodeBlock.Builder#isEmpty()
		 */
		public boolean isEmpty() {
			return builder.isEmpty();
		}

		/**
		 * Add a formatted statement to the code block.
		 *
		 * @param format the format string.
		 * @param args the arguments for the format string.
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#add(String, Object...)
		 */
		@Contract("_, _ -> this")
		public CodeBlockBuilder add(String format, @Nullable Object... args) {

			builder.add(format, args);
			return this;
		}

		/**
		 * Add a {@link CodeBlock} as a statement to the code block.
		 *
		 * @param codeBlock the {@link CodeBlock} to add.
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#addStatement(CodeBlock)
		 */
		@Contract("_ -> this")
		public CodeBlockBuilder addStatement(CodeBlock codeBlock) {

			builder.addStatement(codeBlock);
			return this;
		}

		/**
		 * Add a statement to the code block using a {@link Consumer} to configure it.
		 *
		 * @param consumer the {@link Consumer} to configure the statement.
		 * @return {@code this} builder.
		 */
		@Contract("null -> fail; _ -> this")
		public CodeBlockBuilder addStatement(Consumer<StatementBuilder> consumer) {

			Assert.notNull(consumer, "Consumer must not be null");

			StatementBuilder statementBuilder = new StatementBuilder();
			consumer.accept(statementBuilder);

			if (!statementBuilder.isEmpty()) {

				this.add("$[");

				for (CodeBlock block : statementBuilder.blocks) {
					builder.add(block);
				}

				this.add(";\n$]");

			}
			return this;
		}

		/**
		 * Add a {@link CodeBlock} to the code block.
		 *
		 * @param codeBlock the {@link CodeBlock} to add.
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#addStatement(CodeBlock)
		 */
		@Contract("_ -> this")
		public CodeBlockBuilder add(CodeBlock codeBlock) {

			builder.add(codeBlock);
			return this;
		}

		/**
		 * Add a formatted statement to the code block.
		 *
		 * @param format the format string.
		 * @param args the arguments for the format string.
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#addStatement(String, Object...)
		 */
		@Contract("_, _ -> this")
		public CodeBlockBuilder addStatement(String format, @Nullable Object... args) {

			builder.addStatement(format, args);
			return this;
		}

		/**
		 * Add named arguments to the code block.
		 *
		 * @param format the format string.
		 * @param arguments the named arguments.
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#addNamed(String, Map)
		 */
		@Contract("_, _ -> this")
		public CodeBlockBuilder addNamed(String format, Map<String, ?> arguments) {

			builder.addNamed(format, arguments);
			return this;
		}

		/**
		 * Begin a control flow block with the specified format and arguments.
		 *
		 * @param controlFlow the control flow format string.
		 * @param args the arguments for the control flow format string.
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#beginControlFlow(String, Object...)
		 */
		@Contract("_, _ -> this")
		public CodeBlockBuilder beginControlFlow(String controlFlow, @Nullable Object... args) {

			builder.beginControlFlow(controlFlow, args);
			return this;
		}

		/**
		 * End the current control flow block with the specified format and arguments.
		 *
		 * @param controlFlow the control flow format string.
		 * @param args the arguments for the control flow format string.
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#endControlFlow(String, Object...)
		 */
		@Contract("_, _ -> this")
		public CodeBlockBuilder endControlFlow(String controlFlow, @Nullable Object... args) {

			builder.endControlFlow(controlFlow, args);
			return this;
		}

		/**
		 * End the current control flow block.
		 *
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#endControlFlow()
		 */
		@Contract("-> this")
		public CodeBlockBuilder endControlFlow() {

			builder.endControlFlow();
			return this;
		}

		/**
		 * Begin the next control flow block with the specified format and arguments.
		 *
		 * @param controlFlow the control flow format string.
		 * @param args the arguments for the control flow format string.
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#nextControlFlow(String, Object...)
		 */
		@Contract("_, _ -> this")
		public CodeBlockBuilder nextControlFlow(String controlFlow, @Nullable Object... args) {

			builder.nextControlFlow(controlFlow, args);
			return this;
		}

		/**
		 * Indent the current code block.
		 *
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#indent()
		 */
		@Contract("-> this")
		public CodeBlockBuilder indent() {

			builder.indent();
			return this;
		}

		/**
		 * Unindent the current code block.
		 *
		 * @return {@code this} builder.
		 * @see CodeBlock.Builder#unindent()
		 */
		@Contract("-> this")
		public CodeBlockBuilder unindent() {

			builder.unindent();
			return this;
		}

		/**
		 * Build the {@link CodeBlock} from the current state of the builder.
		 *
		 * @return the constructed {@link CodeBlock}.
		 */
		@CheckReturnValue
		public CodeBlock build() {
			return builder.build();
		}

		/**
		 * Clear the current state of the builder.
		 *
		 * @return {@code this} builder.
		 */
		@Contract("-> this")
		public CodeBlockBuilder clear() {

			builder.clear();
			return this;
		}

	}

	/**
	 * Builder for creating statements including conditional and concatenated variants.
	 * <p>
	 * This builder allows for the creation of complex statements with conditional logic and concatenated elements. It is
	 * designed to simplify the construction of dynamic code blocks.
	 * <p>
	 * Use this builder to handle conditional inclusion in a structured and fluent manner instead of excessive conditional
	 * nesting that would be required otherwise in the calling code.
	 */
	public static class StatementBuilder {

		private final List<CodeBlock> blocks = new ArrayList<>();

		StatementBuilder() {}

		/**
		 * Determine whether this builder is empty.
		 *
		 * @return {@code true} if the builder is empty; {@code false} otherwise.
		 */
		public boolean isEmpty() {
			return blocks.isEmpty();
		}

		/**
		 * Add a conditional statement to the builder if the condition <b>is</b> met.
		 *
		 * @param state the condition to evaluate.
		 * @return a {@link ConditionalStatementStep} for further configuration.
		 */
		public ConditionalStatementStep when(boolean state) {
			return whenNot(!state);
		}

		/**
		 * Add a conditional statement to the builder if the condition is <b>not</b> met.
		 *
		 * @param state the condition to evaluate.
		 * @return a {@link ConditionalStatementStep} for further configuration.
		 */
		public ConditionalStatementStep whenNot(boolean state) {

			return (format, args) -> {

				if (!state) {
					add(format, args);
				}
				return this;
			};
		}

		/**
		 * Add a formatted statement to the builder.
		 *
		 * @param format the format string.
		 * @param args the arguments for the format string.
		 * @return {@code this} builder.
		 */
		@Contract("_, _ -> this")
		public StatementBuilder add(String format, @Nullable Object... args) {
			return add(CodeBlock.of(format, args));
		}

		/**
		 * Add a {@link CodeBlock} to the statement builder.
		 *
		 * @param codeBlock the code block to add.
		 * @return {@code this} builder.
		 */
		@Contract("null -> fail; _ -> this")
		public StatementBuilder add(CodeBlock codeBlock) {

			Assert.notNull(codeBlock, "CodeBlock must not be null");
			blocks.add(codeBlock);
			return this;
		}

		/**
		 * Concatenate elements into the builder with a delimiter.
		 *
		 * @param elements the elements to concatenate.
		 * @param delim the delimiter to use between elements.
		 * @param mapper the mapping function to apply to each element returning a {@link CodeBlock} to add.
		 * @param <T> the type of the elements.
		 * @return {@code this} builder.
		 */
		@Contract("null, _ -> fail; _, _ -> this")
		public <T> StatementBuilder addAll(Iterable<? extends T> elements, String delim,
				Function<? super T, CodeBlock> mapper) {
			return addAll(elements, t -> delim, mapper);
		}

		/**
		 * Concatenate elements into the builder with a custom delimiter function.
		 *
		 * @param elements the elements to concatenate.
		 * @param delim the function to determine the delimiter for each element. Delimiters are applied beginning with the
		 *          second iteration element and obtain from the current element.
		 * @param mapper the mapping function to apply to each element returning a {@link CodeBlock} to add.
		 * @param <T> the type of the elements.
		 * @return {@code this} builder.
		 */
		@Contract("null, _, _ -> fail; _, _, _ -> this")
		public <T> StatementBuilder addAll(Iterable<? extends T> elements, Function<? super T, String> delim,
				Function<? super T, CodeBlock> mapper) {

			Assert.notNull(elements, "Elements must not be null");

			boolean first = true;
			for (T element : elements) {

				if (first) {
					first = false;
				} else {
					blocks.add(CodeBlock.of(delim.apply(element)));
				}

				add(mapper.apply(element));
			}

			return this;
		}

		/**
		 * Functional interface for conditional statement steps.
		 */
		public interface ConditionalStatementStep {

			/**
			 * Add a statement to the builder if the condition is met.
			 *
			 * @param format the format string.
			 * @param args the arguments for the format string.
			 * @return the {@link StatementBuilder}.
			 */
			StatementBuilder then(String format, @Nullable Object... args);

		}

	}

	/**
	 * Builder for constructing return statements based on the target return type. The resulting {@link #build()
	 * CodeBlock} must be added as a {@link CodeBlock.Builder#addStatement(CodeBlock)}.
	 */
	public abstract static class ReturnBuilderSupport {

		private final List<ReturnRule> rules = new ArrayList<>();
		private final List<ReturnRule> fallback = new ArrayList<>();

		/**
		 * Create a new builder.
		 */
		ReturnBuilderSupport() {}

		/**
		 * Add a return statement if the given condition is {@code true}.
		 *
		 * @param condition the condition to evaluate.
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("_, _, _ -> this")
		public ReturnBuilderSupport when(boolean condition, String format, @Nullable Object... args) {
			this.rules.add(ruleOf(condition, format, args));
			return this;
		}

		/**
		 * Add a fallback return statement if no previous return statement was added.
		 *
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("_, _ -> this")
		public ReturnBuilderSupport otherwise(String format, @Nullable Object... args) {
			this.fallback.add(ruleOf(true, format, args));
			return this;
		}

		/**
		 * Add a fallback return statement if no previous return statement was added.
		 *
		 * @param builderConsumer the code block builder consumer to apply.
		 * @return {@code this} builder.
		 */
		ReturnBuilderSupport otherwise(Consumer<CodeBlock.Builder> builderConsumer) {
			this.fallback.add(new ReturnRule(true, "", new Object[] {}, builderConsumer));
			return this;
		}

		/**
		 * Build the code block representing the return statement.
		 *
		 * @return the resulting {@code CodeBlock}
		 */
		@CheckReturnValue
		public CodeBlock build() {

			CodeBlock.Builder builder = CodeBlock.builder();

			for (ReturnRule rule : (Iterable<? extends ReturnRule>) () -> Stream
					.concat(this.rules.stream(), this.fallback.stream()).iterator()) {
				if (rule.condition()) {
					builder.add("return");
					rule.accept(builder);
					return builder.build();
				}
			}

			return builder.build();
		}

		/**
		 * Add a return statement if the given condition is {@code true}.
		 *
		 * @param condition the condition to evaluate.
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		static ReturnRule ruleOf(boolean condition, String format, @Nullable Object... args) {

			Assert.notNull(format, "Format must not be null");

			if (format.startsWith("return")) {
				throw new IllegalArgumentException("Return value format '%s' must not contain 'return'".formatted(format));
			}

			return new ReturnRule(condition, format, args, null);
		}

	}

	record ReturnRule(boolean condition, String format, @Nullable Object[] args,
			@Nullable Consumer<CodeBlock.Builder> builderCustomizer) {

		public void accept(CodeBlock.Builder builder) {

			if (StringUtils.hasText(format()) || builderCustomizer() != null) {

				builder.add(" ");

				if (StringUtils.hasText(format())) {
					builder.add(format(), args());
				}

				if (builderCustomizer() != null) {
					builderCustomizer().accept(builder);
				}
			}
		}

	}

	/**
	 * Builder for constructing return statements based conditionally on the target return type. The resulting
	 * {@link #build() CodeBlock} must be added as a {@link CodeBlock.Builder#addStatement(CodeBlock)}.
	 */
	public static class TypedReturnBuilder extends ReturnBuilderSupport {

		private final ResolvableType returnType;

		/**
		 * Create a new builder for the given return type.
		 *
		 * @param returnType the method return type
		 */
		TypedReturnBuilder(ResolvableType returnType) {

			Assert.notNull(returnType, "Return type must not be null");

			this.returnType = returnType;

			// consider early return cases for Void and void.
			whenBoxed(Void.class, "null");
			when(ReflectionUtils.isVoid(returnType.toClass()), "");
		}

		/**
		 * Add return statements for numeric types if the given {@code resultToReturn} points to a {@link Number}. Considers
		 * primitive and boxed {@code int} and {@code long} type return paths and that {@code resultToReturn} can be
		 * {@literal null}.
		 *
		 * @param resultToReturn the argument or variable name holding the result.
		 * @return {@code this} builder.
		 */
		@Contract("_ -> this")
		public TypedReturnBuilder number(String resultToReturn) {

			return whenBoxedLong("$1L != null ? $1L.longValue() : null", resultToReturn)
					.whenLong("$1L != null ? $1L.longValue() : 0L", resultToReturn)
					.whenBoxedInteger("$1L != null ? $1L.intValue() : null", resultToReturn)
					.whenInt("$1L != null ? $1L.intValue() : 0", resultToReturn)
					.whenBoxed(Byte.class, "$1L != null ? $1L.byteValue() : null", resultToReturn)
					.when(byte.class, "$1L != null ? $1L.byteValue() : 0", resultToReturn)
					.whenBoxed(Short.class, "$1L != null ? $1L.shortValue() : null", resultToReturn)
					.when(short.class, "$1L != null ? $1L.shortValue() : 0", resultToReturn)
					.whenBoxed(Double.class, "$1L != null ? $1L.doubleValue() : null", resultToReturn)
					.when(double.class, "$1L != null ? $1L.doubleValue() : 0", resultToReturn)
					.whenBoxed(Float.class, "$1L != null ? $1L.floatValue() : null", resultToReturn)
					.when(float.class, "$1L != null ? $1L.floatValue() : 0f", resultToReturn);
		}

		/**
		 * Add return statements for numeric types if the given {@code resultToReturn} points to a non-nullable
		 * {@link Number}. Considers all primitive numeric types assuming that {@code resultToReturn} is never
		 * {@literal null}.
		 *
		 * @param resultToReturn the argument or variable name holding the result.
		 * @return {@code this} builder.
		 */
		@Contract("_ -> this")
		public TypedReturnBuilder nonNullableNumber(String resultToReturn) {
			return whenPrimitiveOrBoxed(long.class, "$1L.longValue()", resultToReturn)
					.whenPrimitiveOrBoxed(int.class, "$1L.intValue()", resultToReturn)
					.whenPrimitiveOrBoxed(short.class, "$1L.shortValue()", resultToReturn)
					.whenPrimitiveOrBoxed(byte.class, "$1L.byteValue()", resultToReturn)
					.whenPrimitiveOrBoxed(float.class, "$1L.floatValue()", resultToReturn)
					.whenPrimitiveOrBoxed(double.class, "$1L.doubleValue()", resultToReturn);
		}

		/**
		 * Add a return statement if the return type is boolean (primitive or box type).
		 *
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("_, _ -> this")
		public TypedReturnBuilder whenBoolean(String format, @Nullable Object... args) {
			return when(returnType.isAssignableFrom(boolean.class) || returnType.isAssignableFrom(Boolean.class), format,
					args);
		}

		/**
		 * Add a return statement if the return type is {@link Long} (boxed {@code long} type).
		 *
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("_, _ -> this")
		public TypedReturnBuilder whenBoxedLong(String format, @Nullable Object... args) {
			return whenBoxed(long.class, format, args);
		}

		/**
		 * Add a return statement if the return type is a primitive {@code long} type.
		 *
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("_, _ -> this")
		public TypedReturnBuilder whenLong(String format, @Nullable Object... args) {
			return when(returnType.toClass() == long.class, format, args);
		}

		/**
		 * Add a return statement if the return type is {@link Integer} (boxed {@code int} type).
		 *
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("_, _ -> this")
		public TypedReturnBuilder whenBoxedInteger(String format, @Nullable Object... args) {
			return whenBoxed(int.class, format, args);
		}

		/**
		 * Add a return statement if the return type is a primitive {@code int} type.
		 *
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("_, _ -> this")
		public TypedReturnBuilder whenInt(String format, @Nullable Object... args) {
			return when(returnType.toClass() == int.class, format, args);
		}

		/**
		 * Add a return statement if the return type matches the given boxed wrapper type.
		 *
		 * @param primitiveOrWrapper the primitive or wrapper type.
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("null, _, _ -> fail; _, _, _ -> this")
		public TypedReturnBuilder whenBoxed(Class<?> primitiveOrWrapper, String format, @Nullable Object... args) {

			Class<?> primitiveWrapper = ClassUtils.resolvePrimitiveIfNecessary(primitiveOrWrapper);
			return when(returnType.toClass() == primitiveWrapper, format, args);
		}

		/**
		 * Add a return statement if the return type matches the given primitive or boxed wrapper type.
		 *
		 * @param primitiveType the primitive or wrapper type.
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("null, _, _ -> fail; _, _, _ -> this")
		public TypedReturnBuilder whenPrimitiveOrBoxed(Class<?> primitiveType, String format, @Nullable Object... args) {

			Class<?> primitiveWrapper = ClassUtils.resolvePrimitiveIfNecessary(primitiveType);
			return when(
					ClassUtils.isAssignable(ClassUtils.resolvePrimitiveIfNecessary(returnType.toClass()), primitiveWrapper),
					format, args);
		}

		/**
		 * Add a return statement if the declared return type is assignable from the given {@code returnType}.
		 *
		 * @param returnType the candidate return type.
		 * @param format the code format string.
		 * @param args the format arguments
		 * @return {@code this} builder.
		 */
		@Contract("null, _, _ -> fail; _, _, _ -> this")
		public TypedReturnBuilder when(Class<?> returnType, String format, @Nullable Object... args) {

			Assert.notNull(returnType, "Return type must not be null");
			return when(this.returnType.isAssignableFrom(returnType), format, args);
		}

		/**
		 * Add a return statement if the given condition is {@code true}.
		 *
		 * @param condition the condition to evaluate.
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Override
		@Contract("_, _, _ -> this")
		public TypedReturnBuilder when(boolean condition, String format, @Nullable Object... args) {
			super.when(condition, format, args);
			return this;
		}

		/**
		 * Add a fallback return statement considering that the returned value might be nullable and apply conditionally
		 * {@link Optional#ofNullable(Object)} wrapping if the return type is {@code Optional}.
		 *
		 * @param codeBlock the code block result to be returned.
		 * @return {@code this} builder.
		 */
		@Contract("_ -> this")
		public TypedReturnBuilder optional(CodeBlock codeBlock) {
			return optional("$L", codeBlock);
		}

		/**
		 * Add a fallback return statement considering that the returned value might be nullable and apply conditionally
		 * {@link Optional#ofNullable(Object)} wrapping if the return type is {@code Optional}.
		 *
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Contract("null, _ -> fail; _, _ -> this")
		public TypedReturnBuilder optional(String format, @Nullable Object... args) {

			if (Optional.class.isAssignableFrom(returnType.toClass())) {

				Assert.hasText(format, "Format must not be null or empty");
				if (format.startsWith("return")) {
					throw new IllegalArgumentException("Return value format '%s' must not contain 'return'".formatted(format));
				}

				otherwise(builder -> {

					builder.add("$T.ofNullable(", Optional.class);
					builder.add(format, args);
					builder.add(")");
				});

				return this;
			}

			return otherwise(format, args);
		}

		/**
		 * Add a fallback return statement if no previous return statement was added.
		 *
		 * @param codeBlock the code block result to be returned.
		 * @return {@code this} builder.
		 */
		@Contract("_ -> this")
		public TypedReturnBuilder otherwise(CodeBlock codeBlock) {
			return otherwise("$L", codeBlock);
		}

		/**
		 * Add a fallback return statement if no previous return statement was added.
		 *
		 * @param format the code format string.
		 * @param args the format arguments.
		 * @return {@code this} builder.
		 */
		@Override
		@Contract("_, _ -> this")
		public TypedReturnBuilder otherwise(String format, @Nullable Object... args) {
			super.otherwise(format, args);
			return this;
		}

	}

	record CodeTuple(String format, @Nullable Object... args) {

	}

}
