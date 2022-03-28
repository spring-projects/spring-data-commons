/*
 * Copyright 2022 the original author or authors.
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
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.generator.BeanInstantiationContribution;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.projection.EntityProjectionIntrospector.ProjectionPredicate;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * The {@link BeanInstantiationContribution} for a specific data repository.
 *
 * @author Christoph Strobl
 * @since 3.0
 */
public class RepositoryBeanContribution implements BeanInstantiationContribution {

	private static final Log logger = LogFactory.getLog(RepositoryBeanContribution.class);

	private final AotRepositoryContext context;
	private final RepositoryInformation repositoryInformation;
	private BiConsumer<AotRepositoryContext, CodeContribution> moduleContribution;

	public RepositoryBeanContribution(AotRepositoryContext context) {

		this.context = context;
		this.repositoryInformation = context.getRepositoryInformation();
	}

	@Override
	public void applyTo(CodeContribution contribution) {

		writeRepositoryInfo(contribution);

		if (moduleContribution != null) {
			moduleContribution.accept(context, contribution);
		}
	}

	private void writeRepositoryInfo(CodeContribution contribution) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Contributing data repository information for %s.",
					repositoryInformation.getRepositoryInterface()));
		}

		// TODO: is this the way?
		contribution.runtimeHints().reflection() //
				.registerType(repositoryInformation.getRepositoryInterface(), hint -> {
					hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
				}) //
				.registerType(repositoryInformation.getRepositoryBaseClass(), hint -> {
					hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS);
				}) //
				.registerType(repositoryInformation.getDomainType(), hint -> {
					hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS);
				});

		// fragments
		for (RepositoryFragment<?> fragment : getRepositoryInformation().getFragments()) {

			contribution.runtimeHints().reflection() //
					.registerType(fragment.getSignatureContributor(), hint -> {

						hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);
						if (!fragment.getSignatureContributor().isInterface()) {
							hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
						}
					});
		}

		// the surrounding proxy
		contribution.runtimeHints().proxies() // repository proxy
				.registerJdkProxy(repositoryInformation.getRepositoryInterface(), SpringProxy.class, Advised.class,
						DecoratingProxy.class);

		context.ifTransactionManagerPresent(txMgrBeanNames -> {

			contribution.runtimeHints().proxies() // transactional proxy
					.registerJdkProxy(transactionalRepositoryProxy());

			if (AnnotationUtils.findAnnotation(repositoryInformation.getRepositoryInterface(), Component.class) != null) {

				TypeReference[] source = transactionalRepositoryProxy();
				TypeReference[] txProxyForSerializableComponent = Arrays.copyOf(source, source.length + 1);
				txProxyForSerializableComponent[source.length] = TypeReference.of(Serializable.class);
				contribution.runtimeHints().proxies().registerJdkProxy(txProxyForSerializableComponent);
			}
		});

		// reactive repo
		if (repositoryInformation.isReactiveRepository()) {
			// TODO: do we still need this and how to configure it?
			// registry.initialization().add(NativeInitializationEntry.ofBuildTimeType(configuration.getRepositoryInterface()));
		}

		// Kotlin
		Optional<Class<?>> coroutineRepo = context
				.resolveType("org.springframework.data.repository.kotlin.CoroutineCrudRepository");
		if (coroutineRepo.isPresent()
				&& ClassUtils.isAssignable(coroutineRepo.get(), repositoryInformation.getRepositoryInterface())) {

			contribution.runtimeHints().reflection() //
					.registerTypes(
							Arrays.asList(TypeReference.of("org.springframework.data.repository.kotlin.CoroutineCrudRepository"),
									TypeReference.of(Repository.class), TypeReference.of(Iterable.class),
									TypeReference.of("kotlinx.coroutines.flow.Flow"), TypeReference.of("kotlin.collections.Iterable"),
									TypeReference.of("kotlin.Unit"), TypeReference.of("kotlin.Long"), TypeReference.of("kotlin.Boolean")),
							hint -> {
								hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS);
							});
		}

		// repository methods
		repositoryInformation.getQueryMethods().map(repositoryInformation::getReturnedDomainClass)
				.filter(Class::isInterface).forEach(type -> {
					if (ProjectionPredicate.typeHierarchy().test(type, repositoryInformation.getDomainType())) {
						contributeProjection(type, contribution);
					}
				});
	}

	private TypeReference[] transactionalRepositoryProxy() {

		return new TypeReference[] { TypeReference.of(repositoryInformation.getRepositoryInterface()),
				TypeReference.of(Repository.class),
				TypeReference.of("org.springframework.transaction.interceptor.TransactionalProxy"),
				TypeReference.of("org.springframework.aop.framework.Advised"), TypeReference.of(DecoratingProxy.class) };
	}

	protected void contributeProjection(Class<?> type, CodeContribution contribution) {

		contribution.runtimeHints().proxies().registerJdkProxy(type, TargetAware.class, SpringProxy.class,
				DecoratingProxy.class);
	}

	/**
	 * Callback for module specific contributions.
	 *
	 * @param moduleContribution can be {@literal null}.
	 * @return this.
	 */
	public RepositoryBeanContribution setModuleContribution(
			@Nullable BiConsumer<AotRepositoryContext, CodeContribution> moduleContribution) {

		this.moduleContribution = moduleContribution;
		return this;
	}

	public RepositoryInformation getRepositoryInformation() {
		return repositoryInformation;
	}
}
