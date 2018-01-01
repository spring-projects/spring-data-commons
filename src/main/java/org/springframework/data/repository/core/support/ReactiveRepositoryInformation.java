/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.data.repository.core.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * This {@link RepositoryInformation} uses a {@link ConversionService} to check whether method arguments can be
 * converted for invocation of implementation methods.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 2.0
 * @deprecated as of 2.0, can be deleted...
 */
@Deprecated
public class ReactiveRepositoryInformation extends DefaultRepositoryInformation {

	/**
	 * Creates a new {@link ReactiveRepositoryInformation} for the given {@link RepositoryMetadata}, repository base
	 * class, custom implementation and {@link ConversionService}.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param repositoryBaseClass must not be {@literal null}.
	 * @param composition can be {@literal null}.
	 */
	public ReactiveRepositoryInformation(RepositoryMetadata metadata, Class<?> repositoryBaseClass,
			RepositoryComposition composition) {
		super(metadata, repositoryBaseClass, composition.withMethodLookup(MethodLookups.forReactiveTypes(metadata)));
	}
}
