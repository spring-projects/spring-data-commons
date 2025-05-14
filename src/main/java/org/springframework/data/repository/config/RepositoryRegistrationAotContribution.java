/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.data.repository.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragmentsDecorator;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.data.aot.AotContext;
import org.springframework.data.projection.EntityProjectionIntrospector;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.Predicates;
import org.springframework.data.util.QTypeContributor;
import org.springframework.data.util.TypeContributor;
import org.springframework.data.util.TypeUtils;
import org.springframework.javapoet.CodeBlock;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanRegistrationAotContribution} used to contribute repository registrations.
 *
 * @author John Blum
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.0
 */
public class RepositoryRegistrationAotContribution implements BeanRegistrationAotContribution {

	private static final Log logger = LogFactory.getLog(RepositoryRegistrationAotContribution.class);

	private static final String KOTLIN_COROUTINE_REPOSITORY_TYPE_NAME = "org.springframework.data.repository.kotlin.CoroutineCrudRepository";

	private final RepositoryRegistrationAotProcessor aotProcessor;

	private final AotRepositoryContext repositoryContext;

	private @Nullable RepositoryContributor repositoryContributor;

	private @Nullable BiFunction<AotRepositoryContext, GenerationContext, @Nullable RepositoryContributor> moduleContribution;

	/**
	 * Constructs a new instance of the {@link RepositoryRegistrationAotContribution} initialized with the given, required
	 * {@link RepositoryRegistrationAotProcessor} from which this contribution was created.
	 *
	 * @param processor reference back to the {@link RepositoryRegistrationAotProcessor} from which this contribution was
	 *          created.
	 * @param context reference back to the {@link AotRepositoryContext} from which this contribution was created.
	 * @throws IllegalArgumentException if the {@link RepositoryRegistrationAotProcessor} is {@literal null}.
	 * @see RepositoryRegistrationAotProcessor
	 */
	protected RepositoryRegistrationAotContribution(RepositoryRegistrationAotProcessor processor,
			AotRepositoryContext context) {

		Assert.notNull(processor, "RepositoryRegistrationAotProcessor must not be null");
		Assert.notNull(context, "AotRepositoryContext must not be null");

		this.aotProcessor = processor;
		this.repositoryContext = context;
	}

	/**
	 * Factory method used to construct a new instance of {@link RepositoryRegistrationAotContribution} initialized with
	 * the given, required {@link RepositoryRegistrationAotProcessor} from which this contribution was created.
	 *
	 * @param processor reference back to the {@link RepositoryRegistrationAotProcessor} from which this contribution was
	 *          created.
	 * @return a new instance of {@link RepositoryRegistrationAotContribution} if a contribution can be made;
	 *         {@literal null} if no contribution can be made.
	 * @see RepositoryRegistrationAotProcessor
	 */
	public static @Nullable RepositoryRegistrationAotContribution load(RepositoryRegistrationAotProcessor processor,
			RegisteredBean repositoryBean) {

		RepositoryConfiguration<?> repositoryMetadata = processor.getRepositoryMetadata(repositoryBean);

		if (repositoryMetadata == null) {
			return null;
		}

		AotRepositoryContext repositoryContext = buildAotRepositoryContext(processor.getEnvironment(), repositoryBean,
				repositoryMetadata);

		if (repositoryContext == null) {
			return null;
		}

		return new RepositoryRegistrationAotContribution(processor, repositoryContext);
	}

	/**
	 * Builds a {@link RepositoryRegistrationAotContribution} for given, required {@link RegisteredBean} representing the
	 * {@link Repository} registered in the bean registry.
	 *
	 * @param repositoryBean {@link RegisteredBean} for the {@link Repository}; must not be {@literal null}.
	 * @return a {@link RepositoryRegistrationAotContribution} to contribute AOT metadata and code for the
	 *         {@link Repository} {@link RegisteredBean}.
	 * @throws IllegalArgumentException if the {@link RegisteredBean} is {@literal null}.
	 * @deprecated since 4.0.
	 */
	@Deprecated(since = "4.0", forRemoval = true)
	public @Nullable RepositoryRegistrationAotContribution forBean(RegisteredBean repositoryBean) {

		RepositoryConfiguration<?> repositoryMetadata = getRepositoryRegistrationAotProcessor()
				.getRepositoryMetadata(repositoryBean);

		if (repositoryMetadata == null) {
			return null;
		}

		AotRepositoryContext repositoryContext = buildAotRepositoryContext(aotProcessor.getEnvironment(), repositoryBean,
				repositoryMetadata);

		if (repositoryContext == null) {
			return null;
		}

		return new RepositoryRegistrationAotContribution(getRepositoryRegistrationAotProcessor(), repositoryContext);
	}

	protected @Nullable BiFunction<AotRepositoryContext, GenerationContext, @Nullable RepositoryContributor> getModuleContribution() {
		return this.moduleContribution;
	}

	protected AotRepositoryContext getRepositoryContext() {
		return this.repositoryContext;
	}

	protected RepositoryRegistrationAotProcessor getRepositoryRegistrationAotProcessor() {
		return this.aotProcessor;
	}

	public RepositoryInformation getRepositoryInformation() {
		return getRepositoryContext().getRepositoryInformation();
	}

	private void logTrace(String message, Object... arguments) {
		getRepositoryRegistrationAotProcessor().logTrace(message, arguments);
	}

	private static @Nullable AotRepositoryContext buildAotRepositoryContext(Environment environment, RegisteredBean bean,
			RepositoryConfiguration<?> repositoryConfiguration) {

		RepositoryBeanDefinitionReader reader = new RepositoryBeanDefinitionReader(bean);
		RepositoryConfiguration<?> configuration = reader.getConfiguration();
		RepositoryConfigurationExtensionSupport extension = reader.getConfigurationExtension();

		if (configuration == null || extension == null) {
			logger.warn(
					"Cannot create AotRepositoryContext for bean [%s]. No RepositoryConfiguration/RepositoryConfigurationExtension. Please make sure to register the repository bean through @Enable…Repositories."
							.formatted(bean.getBeanName()));
			return null;
		}
		RepositoryInformation repositoryInformation = reader.getRepositoryInformation();
		DefaultAotRepositoryContext repositoryContext = new DefaultAotRepositoryContext(bean, repositoryInformation,
				extension.getModuleName(), AotContext.from(bean.getBeanFactory(), environment),
				configuration.getConfigurationSource());

		repositoryContext.setIdentifyingAnnotations(extension.getIdentifyingAnnotations());

		return repositoryContext;
	}

	/**
	 * {@link BiConsumer Callback} for data module specific contributions.
	 *
	 * @param moduleContribution {@link BiConsumer} used by data modules to submit contributions; can be {@literal null}.
	 * @return this.
	 */
	public RepositoryRegistrationAotContribution withModuleContribution(
			@Nullable BiFunction<AotRepositoryContext, GenerationContext, @Nullable RepositoryContributor> moduleContribution) {
		this.moduleContribution = moduleContribution;
		return this;
	}

	@Override
	public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {

		contributeRepositoryInfo(this.repositoryContext, generationContext);

		var moduleContribution = getModuleContribution();
		if (moduleContribution != null && this.repositoryContributor == null) {

			this.repositoryContributor = moduleContribution.apply(getRepositoryContext(), generationContext);

			if (this.repositoryContributor != null) {
				this.repositoryContributor.contribute(generationContext);
			}
		}
	}

	@Override
	public BeanRegistrationCodeFragments customizeBeanRegistrationCodeFragments(GenerationContext generationContext,
			BeanRegistrationCodeFragments codeFragments) {

		return new BeanRegistrationCodeFragmentsDecorator(codeFragments) {

			@Override
			public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
					BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
					Predicate<String> attributeFilter) {

				if (repositoryContributor == null) { // no aot implementation -> go on as

					return super.generateSetBeanDefinitionPropertiesCode(generationContext, beanRegistrationCode, beanDefinition,
							attributeFilter);
				}

				AotRepositoryBeanDefinitionPropertiesDecorator decorator = new AotRepositoryBeanDefinitionPropertiesDecorator(
						() -> super.generateSetBeanDefinitionPropertiesCode(generationContext, beanRegistrationCode, beanDefinition,
								attributeFilter),
						repositoryContributor);

				return decorator.decorate();
			}
		};
	}

	public Predicate<Class<?>> typeFilter() { // like only document ones. // TODO: As in MongoDB?
		return Predicates.isTrue();
	}

	private void contributeRepositoryInfo(AotRepositoryContext repositoryContext, GenerationContext contribution) {

		RepositoryInformation repositoryInformation = getRepositoryInformation();

		logTrace("Contributing repository information for [%s]", repositoryInformation.getRepositoryInterface());

		contribution.getRuntimeHints().reflection()
				.registerType(repositoryInformation.getRepositoryInterface(),
						hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS))
				.registerType(repositoryInformation.getRepositoryBaseClass(), hint -> hint
						.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));

		TypeContributor.contribute(repositoryInformation.getDomainType(), contribution);
		QTypeContributor.contributeEntityPath(repositoryInformation.getDomainType(), contribution,
				repositoryContext.getClassLoader());

		// Repository Fragments
		for (RepositoryFragment<?> fragment : getRepositoryInformation().getFragments()) {

			Class<?> repositoryFragmentType = fragment.getSignatureContributor();
			Optional<Class<?>> implementation = fragment.getImplementationClass();

			contribution.getRuntimeHints().reflection().registerType(repositoryFragmentType, hint -> {

				hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);

				if (!repositoryFragmentType.isInterface()) {
					hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
				}
			});

			implementation.ifPresent(typeToRegister -> {
				contribution.getRuntimeHints().reflection().registerType(typeToRegister, hint -> {

					hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);

					if (!typeToRegister.isInterface()) {
						hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
					}
				});
			});
		}

		// Repository Proxy
		contribution.getRuntimeHints().proxies().registerJdkProxy(repositoryInformation.getRepositoryInterface(),
				SpringProxy.class, Advised.class, DecoratingProxy.class);

		// Transactional Repository Proxy
		// repositoryContext.ifTransactionManagerPresent(transactionManagerBeanNames -> {

		// TODO: Is the following double JDK Proxy registration above necessary or would a single JDK Proxy
		// registration suffice?
		// In other words, simply having a single JDK Proxy registration either with or without
		// the additional Serializable TypeReference?
		// NOTE: Using a single JDK Proxy registration causes the
		// simpleRepositoryWithTxManagerNoKotlinNoReactiveButComponent() test case method to fail.
		List<TypeReference> transactionalRepositoryProxyTypeReferences = transactionalRepositoryProxyTypeReferences(
				repositoryInformation);

		contribution.getRuntimeHints().proxies()
				.registerJdkProxy(transactionalRepositoryProxyTypeReferences.toArray(new TypeReference[0]));

		if (isComponentAnnotatedRepository(repositoryInformation)) {
			transactionalRepositoryProxyTypeReferences.add(TypeReference.of(Serializable.class));
			contribution.getRuntimeHints().proxies()
					.registerJdkProxy(transactionalRepositoryProxyTypeReferences.toArray(new TypeReference[0]));
		}
		// });

		// Kotlin
		if (isKotlinCoroutineRepository(repositoryContext, repositoryInformation)) {
			contribution.getRuntimeHints().reflection().registerTypes(kotlinRepositoryReflectionTypeReferences(),
					hint -> hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		// Repository query methods
		repositoryInformation.getQueryMethods().stream().map(repositoryInformation::getReturnedDomainClass)
				.filter(Class::isInterface).forEach(type -> {
					if (EntityProjectionIntrospector.ProjectionPredicate.typeHierarchy().test(type,
							repositoryInformation.getDomainType())) {
						contributeProjection(type, contribution);
					}
				});
	}

	private boolean isComponentAnnotatedRepository(RepositoryInformation repositoryInformation) {
		return AnnotationUtils.findAnnotation(repositoryInformation.getRepositoryInterface(), Component.class) != null;
	}

	private boolean isKotlinCoroutineRepository(AotRepositoryContext repositoryContext,
			RepositoryInformation repositoryInformation) {

		return repositoryContext.introspectType(KOTLIN_COROUTINE_REPOSITORY_TYPE_NAME).resolveType()
				.filter(it -> ClassUtils.isAssignable(it, repositoryInformation.getRepositoryInterface())).isPresent();
	}

	private List<TypeReference> kotlinRepositoryReflectionTypeReferences() {

		return new ArrayList<>(
				Arrays.asList(TypeReference.of("org.springframework.data.repository.kotlin.CoroutineCrudRepository"),
						TypeReference.of(Repository.class), //
						TypeReference.of(Iterable.class), //
						TypeReference.of("kotlinx.coroutines.flow.Flow"), //
						TypeReference.of("kotlin.collections.Iterable"), //
						TypeReference.of("kotlin.Unit"), //
						TypeReference.of("kotlin.Long"), //
						TypeReference.of("kotlin.Boolean")));
	}

	private List<TypeReference> transactionalRepositoryProxyTypeReferences(RepositoryInformation repositoryInformation) {

		return new ArrayList<>(Arrays.asList(TypeReference.of(repositoryInformation.getRepositoryInterface()),
				TypeReference.of(Repository.class), //
				TypeReference.of("org.springframework.transaction.interceptor.TransactionalProxy"), //
				TypeReference.of("org.springframework.aop.framework.Advised"), //
				TypeReference.of(DecoratingProxy.class)));
	}

	private void contributeProjection(Class<?> type, GenerationContext generationContext) {

		generationContext.getRuntimeHints().proxies().registerJdkProxy(type, TargetAware.class, SpringProxy.class,
				DecoratingProxy.class);
	}

	static boolean isJavaOrPrimitiveType(Class<?> type) {
		return TypeUtils.type(type).isPartOf("java") //
				|| ClassUtils.isPrimitiveOrWrapper(type) //
				|| ClassUtils.isPrimitiveArray(type); //
	}


}
