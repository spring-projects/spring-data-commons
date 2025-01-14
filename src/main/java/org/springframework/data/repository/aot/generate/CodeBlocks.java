/*
 * Copyright 2025 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.springframework.data.repository.aot.generate.AotRepositoryBuilder.TargetAotRepositoryImplementationMetadata;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Helper to write contextual pieces of code during code generation.
 *
 * @author Christoph Strobl
 */
public class CodeBlocks {

	private final TargetAotRepositoryImplementationMetadata metadata;

	public CodeBlocks(TargetAotRepositoryImplementationMetadata metadata) {
		this.metadata = metadata;
	}

	/**
	 * @param level the log level eg. `debug`.
	 * @param message the message to print/
	 * @param args optional args to be applied to the message.
	 * @return a {@link CodeBlock} containing a level guarded logging statement.
	 */
	private CodeBlock log(String level, String message, Object... args) {

		CodeBlock.Builder builder = CodeBlock.builder();
		builder.beginControlFlow("if($L.is$LEnabled())", metadata.fieldNameOf(Log.class), StringUtils.capitalize(level));
		if (ObjectUtils.isEmpty(args)) {
			builder.addStatement("$L.$L($S)", metadata.fieldNameOf(Log.class), level, message);
		} else {
			builder.addStatement("$L.$L($S.formatted($L))", metadata.fieldNameOf(Log.class), level, message,
					StringUtils.arrayToCommaDelimitedString(args));
		}
		builder.endControlFlow();
		return builder.build();
	}

	/**
	 * @param message the logging message.
	 * @param args optional args to apply to the message.
	 * @return a {@link CodeBlock} containing a debug level guarded logging statement.
	 */
	public CodeBlock logDebug(String message, Object... args) {
		return log("debug", message, args);
	}

}
