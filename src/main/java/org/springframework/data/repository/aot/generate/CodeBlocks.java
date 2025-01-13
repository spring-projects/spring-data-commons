/*
 * Copyright 2025. the original author or authors.
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

/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.repository.aot.generate;

import org.apache.commons.logging.Log;
import org.springframework.data.repository.aot.generate.AotRepositoryBuilder.GenerationMetadata;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @since 2025/01
 */
public class CodeBlocks {

	private final GenerationMetadata metadata;

	public CodeBlocks(GenerationMetadata metadata) {
		this.metadata = metadata;
	}

	private CodeBlock log(String level, String message) {

		CodeBlock.Builder builder = CodeBlock.builder();
		builder.beginControlFlow("if($L.is$LEnabled())", metadata.fieldNameOf(Log.class), StringUtils.capitalize(level));
		builder.addStatement("$L.$L($S)", metadata.fieldNameOf(Log.class), level, message);
		builder.endControlFlow();
		return builder.build();
	}

	public CodeBlock logDebug(String message) {
		return log("debug", message);
	}

}
