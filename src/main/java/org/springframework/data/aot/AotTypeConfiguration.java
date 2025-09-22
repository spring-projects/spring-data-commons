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

package org.springframework.data.aot;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.DecoratingProxy;
import org.springframework.data.projection.TargetAware;

/**
 * Configuration object that captures various AOT configuration aspects of types within the data context by offering
 * predefined methods to register native configuration necessary for data binding, projection proxy definitions, AOT
 * cglib bytecode generation and other common tasks.
 *
 * @author Christoph Strobl
 * @since 4.0
 */
public interface AotTypeConfiguration {

	/**
	 * Configure the referenced type for data binding. In case of {@link java.lang.annotation.Annotation} only data ones
	 * are considered. For more fine-grained control use {@link #forReflectiveAccess(MemberCategory...)}.
	 *
	 * @return this.
	 */
	AotTypeConfiguration forDataBinding();

	/**
	 * Configure the referenced type for reflective access by providing at least one {@link MemberCategory}.
	 *
	 * @param categories must not contain {@literal null}.
	 * @return this.
	 */
	AotTypeConfiguration forReflectiveAccess(MemberCategory... categories);

	/**
	 * Contribute generated cglib accessors for the referenced type.
	 * <p>
	 * Can be disabled by user configuration ({@code spring.aot.data.accessors.enabled}). Honors in/exclusions set by user
	 * configuration {@code spring.aot.data.accessors.include} / {@code spring.aot.data.accessors.exclude}
	 *
	 * @return this.
	 */
	AotTypeConfiguration contributeAccessors();

	/**
	 * Configure the referenced type as a projection interface returned by eg. a query method.
	 * <p>
	 * Shortcut for {@link #proxyInterface(Class[]) proxyInterface(TargetAware, SpringProxy, DecoratingProxy)}
	 *
	 * @return this.
	 */
	default AotTypeConfiguration usedAsProjectionInterface() {
		return proxyInterface(TargetAware.class, SpringProxy.class, DecoratingProxy.class);
	}

	/**
	 * Configure the referenced type as a spring proxy interface.
	 * <p>
	 * Shortcut for {@link #proxyInterface(Class[]) proxyInterface(SpringProxy, Advised, DecoratingProxy)}
	 *
	 * @return this.
	 */
	default AotTypeConfiguration springProxy() {
		return proxyInterface(SpringProxy.class, Advised.class, DecoratingProxy.class);
	}

	/**
	 * Configure the referenced type as a repository proxy.
	 *
	 * @return this.
	 */
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

	/**
	 * Register a proxy for the referenced type that also implements the given proxyInterfaces.
	 *
	 * @param proxyInterfaces additional interfaces the proxy implements. Order matters!
	 * @return this.
	 */
	AotTypeConfiguration proxyInterface(List<TypeReference> proxyInterfaces);

	/**
	 * Register a proxy for the referenced type that also implements the given proxyInterfaces.
	 *
	 * @param proxyInterfaces additional interfaces the proxy implements. Order matters!
	 * @return this.
	 */
	default AotTypeConfiguration proxyInterface(Class<?>... proxyInterfaces) {
		return proxyInterface(Stream.of(proxyInterfaces).map(TypeReference::of).toList());
	}

	/**
	 * Configure the referenced type for usage with Querydsl by registering hints for potential {@code Q} types.
	 *
	 * @return this.
	 */
	AotTypeConfiguration forQuerydsl();

}
