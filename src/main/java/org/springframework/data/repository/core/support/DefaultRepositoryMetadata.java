/*
 * Copyright 2011-2018 the original author or authors.
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

import lombok.Getter;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link RepositoryMetadata}. Will inspect generic types of {@link Repository} to find out
 * about domain and id class.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@Getter
public class DefaultRepositoryMetadata extends AbstractRepositoryMetadata {

	private static final String MUST_BE_A_REPOSITORY = String.format("Given type must be assignable to %s!",
			Repository.class);

	private final Class<?> idType;
	private final Class<?> domainType;

	/**
	 * Creates a new {@link DefaultRepositoryMetadata} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public DefaultRepositoryMetadata(Class<?> repositoryInterface) {

		super(repositoryInterface);
		Assert.isTrue(Repository.class.isAssignableFrom(repositoryInterface), MUST_BE_A_REPOSITORY);

		List<TypeInformation<?>> arguments = ClassTypeInformation.from(repositoryInterface) //
				.getRequiredSuperTypeInformation(Repository.class)//
				.getTypeArguments();

		this.domainType = resolveTypeParameter(arguments, 0,
				() -> String.format("Could not resolve domain type of %s!", repositoryInterface));
		this.idType = resolveTypeParameter(arguments, 1,
				() -> String.format("Could not resolve id type of %s!", repositoryInterface));
	}

	private static Class<?> resolveTypeParameter(List<TypeInformation<?>> arguments, int index,
			Supplier<String> exceptionMessage) {

		if (arguments.size() <= index || arguments.get(index) == null) {
			throw new IllegalArgumentException(exceptionMessage.get());
		}

		return arguments.get(index).getType();
	}
}
