/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.web.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration.PageModule;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} providing hints for web usage.
 *
 * @author Christoph Strobl
 * @since 3.2.3
 */
class WebRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		hints.reflection().registerType(org.springframework.data.web.config.SpringDataWebSettings.class, hint -> hint
				.withMembers(MemberCategory.INVOKE_DECLARED_METHODS).onReachableType(EnableSpringDataWebSupport.class));

		if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader)) {

			// Page Model for Jackson Rendering
			hints.reflection().registerType(org.springframework.data.web.PagedModel.class,
					MemberCategory.INVOKE_PUBLIC_METHODS);

			hints.reflection().registerType(PagedModel.PageMetadata.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
					MemberCategory.INVOKE_PUBLIC_METHODS);

			// Type that might not be seen otherwise
			hints.reflection().registerType(TypeReference.of("org.springframework.data.domain.Unpaged"),
					hint -> hint.onReachableType(PageModule.class));

			// Jackson Converters used via @JsonSerialize in SpringDataJacksonConfiguration
			hints.reflection().registerType(
					TypeReference
							.of("org.springframework.data.web.config.SpringDataJacksonConfiguration$PageModule$PageModelConverter"),
					hint -> {
						hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
						hint.onReachableType(PageModule.class);
					});
			hints.reflection().registerType(TypeReference.of(
					"org.springframework.data.web.config.SpringDataJacksonConfiguration$PageModule$PlainPageSerializationWarning"),
					hint -> {
						hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
						hint.onReachableType(PageModule.class);
					});
		}
	}
}
