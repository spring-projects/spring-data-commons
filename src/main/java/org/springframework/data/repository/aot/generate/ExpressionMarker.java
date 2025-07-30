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
package org.springframework.data.repository.aot.generate;

import org.springframework.javapoet.CodeBlock;

/**
 * ExpressionMarker is used to add a dedicated type to AOT generated methods that can be used to determine the current
 * method by calling {@link Class#getEnclosingMethod()} on it. This can be useful when working with expressions (eg.
 * SpEL) that need to be evaluated in a given context.
 * <p>
 * {@link ExpressionMarker} is intended to be used via {@link AotQueryMethodGenerationContext} to maintain usage info,
 * making sure the code is only added ({@link #isInUse()}) when {@link #enclosingMethod()} was called for generating
 * code.
 * 
 * <pre class="code">
 * ExpressionMarker marker = context.getExpressionMarker();
 * CodeBlock.builder().add("evaluate($L, $S, $L)", marker.enclosingMethod(), queryString, parameters);
 * </pre>
 * 
 * @author Christoph Strobl
 * @since 4.0
 */
public class ExpressionMarker {

	private final String typeName;
	private boolean inUse = false;

	ExpressionMarker() {
		this("ExpressionMarker");
	}

	ExpressionMarker(String typeName) {
		this.typeName = typeName;
	}

	/**
	 * @return {@code class ExpressionMarker}.
	 */
	CodeBlock declaration() {
		return CodeBlock.of("class $L{};\n", typeName);
	}

	/**
	 * Calling this method sets the {@link ExpressionMarker} as {@link #isInUse() in-use}.
	 *
	 * @return {@code ExpressionMarker.class}.
	 */
	public CodeBlock marker() {

		if (!inUse) {
			inUse = true;
		}
		return CodeBlock.of("$L.class", typeName);
	}

	/**
	 * Calling this method sets the {@link ExpressionMarker} as {@link #isInUse() in-use}.
	 *
	 * @return {@code ExpressionMarker.class.getEnclosingMethod()}
	 */
	public CodeBlock enclosingMethod() {
		return CodeBlock.of("$L.getEnclosingMethod()", marker());
	}

	/**
	 * @return if the marker is in use.
	 */
	public boolean isInUse() {
		return inUse;
	}
}
