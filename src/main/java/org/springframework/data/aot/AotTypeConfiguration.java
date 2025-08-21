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

package org.springframework.data.aot;

import java.io.Serializable;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.env.Environment;
import org.springframework.data.projection.TargetAware;

/**
 * @author Christoph Strobl
 */
public interface AotTypeConfiguration {

	AotTypeConfiguration forDataBinding();

	AotTypeConfiguration forReflectiveAccess(MemberCategory... categories);

	AotTypeConfiguration generateEntityInstantiator();

	// TODO: ? should this be a global condition for the entire configuration or do we need it for certain aspects ?
	AotTypeConfiguration conditional(Predicate<TypeReference> filter);

	default AotTypeConfiguration usedAsProjectionInterface() {
		return proxyInterface(TargetAware.class, SpringProxy.class, DecoratingProxy.class);
	}

	default AotTypeConfiguration springProxy() {
		return proxyInterface(SpringProxy.class, Advised.class, DecoratingProxy.class);
	}

	default AotTypeConfiguration repositoryProxy() {

		springProxy();

		List<TypeReference> transactionalProxy = List.of(TypeReference.of("org.springframework.data.repository.Repository"),
				TypeReference.of("org.springframework.transaction.interceptor.TransactionalProxy"),
				TypeReference.of("org.springframework.aop.framework.Advised"), TypeReference.of(DecoratingProxy.class));
		proxyInterface(transactionalProxy);

		proxyInterface(
				Stream.concat(transactionalProxy.stream(), Stream.of(TypeReference.of(Serializable.class))).toList());

		return this;
	}

	AotTypeConfiguration proxyInterface(List<TypeReference> proxyInterfaces);

	default AotTypeConfiguration proxyInterface(TypeReference... proxyInterfaces) {
		return proxyInterface(List.of(proxyInterfaces));
	}

	default AotTypeConfiguration proxyInterface(Class<?>... proxyInterfaces) {
		return proxyInterface(Stream.of(proxyInterfaces).map(TypeReference::of).toList());
	}

	AotTypeConfiguration forQuerydsl();

	void contribute(GenerationContext generationContext);

	static Predicate<TypeReference> userConfiguredCondition(Environment environment) {

		return new Predicate<TypeReference>() {

			private final List<String> allowedAccessorTypes = environment.getProperty("spring.data.aot.generate.accessor",
					List.class, List.of());

			@Override
			@SuppressWarnings("unchecked")
			public boolean test(TypeReference typeReference) {

				if (!allowedAccessorTypes.isEmpty()) {
					if (allowedAccessorTypes.contains("none") || allowedAccessorTypes.contains("false")
							|| allowedAccessorTypes.contains("off")) {
						return false;
					}
					if (!allowedAccessorTypes.contains(typeReference.getName())) {
						return false;
					}
				}

				return true;
			}
		};
	}

}
