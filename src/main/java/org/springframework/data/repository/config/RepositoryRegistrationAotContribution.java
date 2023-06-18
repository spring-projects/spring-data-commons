/*
 * Copyright 2022-2023 the original author or authors.
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
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.aot.AotContext;
import org.springframework.data.projection.EntityProjectionIntrospector;
import org.springframework.data.projection.TargetAware;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.QTypeContributor;
import org.springframework.data.util.TypeContributor;
import org.springframework.data.util.TypeUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanRegistrationAotContribution} used to contribute repository registrations.
 *
 * @author John Blum
 * @since 3.0
 */
public class RepositoryRegistrationAotContribution implements BeanRegistrationAotContribution {

	private static final String KOTLIN_COROUTINE_REPOSITORY_TYPE_NAME = "org.springframework.data.repository.kotlin.CoroutineCrudRepository";

	/**
	 * Factory method used to construct a new instance of {@link RepositoryRegistrationAotContribution} initialized with
	 * the given, required {@link RepositoryRegistrationAotProcessor} from which this contribution was created.
	 *
	 * @param repositoryRegistrationAotProcessor reference back to the {@link RepositoryRegistrationAotProcessor} from
	 *          which this contribution was created.
	 * @return a new instance of {@link RepositoryRegistrationAotContribution}.
	 * @throws IllegalArgumentException if the {@link RepositoryRegistrationAotProcessor} is {@literal null}.
	 * @see RepositoryRegistrationAotProcessor
	 */
	public static RepositoryRegistrationAotContribution fromProcessor(
			RepositoryRegistrationAotProcessor repositoryRegistrationAotProcessor) {

		return new RepositoryRegistrationAotContribution(repositoryRegistrationAotProcessor);
	}

	private AotRepositoryContext repositoryContext;

	private BiConsumer<AotRepositoryContext, GenerationContext> moduleContribution;

	private final RepositoryRegistrationAotProcessor repositoryRegistrationAotProcessor;

	/**
	 * Constructs a new instance of the {@link RepositoryRegistrationAotContribution} initialized with the given, required
	 * {@link RepositoryRegistrationAotProcessor} from which this contribution was created.
	 *
	 * @param repositoryRegistrationAotProcessor reference back to the {@link RepositoryRegistrationAotProcessor} from
	 *          which this contribution was created.
	 * @throws IllegalArgumentException if the {@link RepositoryRegistrationAotProcessor} is {@literal null}.
	 * @see RepositoryRegistrationAotProcessor
	 */
	protected RepositoryRegistrationAotContribution(
			RepositoryRegistrationAotProcessor repositoryRegistrationAotProcessor) {

		Assert.notNull(repositoryRegistrationAotProcessor, "RepositoryRegistrationAotProcessor must not be null");

		this.repositoryRegistrationAotProcessor = repositoryRegistrationAotProcessor;
	}

	protected ConfigurableListableBeanFactory getBeanFactory() {
		return getRepositoryRegistrationAotProcessor().getBeanFactory();
	}

	protected Optional<BiConsumer<AotRepositoryContext, GenerationContext>> getModuleContribution() {
		return Optional.ofNullable(this.moduleContribution);
	}

	protected AotRepositoryContext getRepositoryContext() {

		Assert.state(this.repositoryContext != null,
				"The AOT RepositoryContext was not properly initialized; did you call the forBean(:RegisteredBean) method");

		return this.repositoryContext;
	}

	protected RepositoryRegistrationAotProcessor getRepositoryRegistrationAotProcessor() {
		return this.repositoryRegistrationAotProcessor;
	}

	public RepositoryInformation getRepositoryInformation() {
		return getRepositoryContext().getRepositoryInformation();
	}

	private void logTrace(String message, Object... arguments) {
		getRepositoryRegistrationAotProcessor().logTrace(message, arguments);
	}

	/**
	 * Builds a {@link RepositoryRegistrationAotContribution} for given, required {@link RegisteredBean} representing the
	 * {@link Repository} registered in the bean registry.
	 *
	 * @param repositoryBean {@link RegisteredBean} for the {@link Repository}; must not be {@literal null}.
	 * @return a {@link RepositoryRegistrationAotContribution} to contribute AOT metadata and code for the
	 *         {@link Repository} {@link RegisteredBean}.
	 * @throws IllegalArgumentException if the {@link RegisteredBean} is {@literal null}.
	 * @see org.springframework.beans.factory.support.RegisteredBean
	 */
	public RepositoryRegistrationAotContribution forBean(RegisteredBean repositoryBean) {

		Assert.notNull(repositoryBean, "The RegisteredBean for the repository must not be null");

		RepositoryConfiguration<?> repositoryMetadata = getRepositoryRegistrationAotProcessor()
				.getRepositoryMetadata(repositoryBean);

		this.repositoryContext = buildAotRepositoryContext(repositoryBean, repositoryMetadata);

		enhanceRepositoryBeanDefinition(repositoryBean, repositoryMetadata, this.repositoryContext);

		return this;
	}

	protected DefaultAotRepositoryContext buildAotRepositoryContext(RegisteredBean bean,
			RepositoryConfiguration<?> repositoryMetadata) {

		RepositoryInformation repositoryInformation = resolveRepositoryInformation(repositoryMetadata);

		DefaultAotRepositoryContext repositoryContext = new DefaultAotRepositoryContext(
				AotContext.from(this.getBeanFactory()));

		repositoryContext.setBeanName(bean.getBeanName());
		repositoryContext.setBasePackages(repositoryMetadata.getBasePackages().toSet());
		repositoryContext.setIdentifyingAnnotations(resolveIdentifyingAnnotations());
		repositoryContext.setRepositoryInformation(repositoryInformation);

		return repositoryContext;
	}

	private Set<Class<? extends Annotation>> resolveIdentifyingAnnotations() {

		Set<Class<? extends Annotation>> identifyingAnnotations = Collections.emptySet();

		try {
			// TODO: Getting all beans of type RepositoryConfigurationExtensionSupport will have the effect that
			// if the user is currently operating in multi-store mode, then all identifying annotations from
			// all stores will be included in the resulting Set.
			// When using AOT, is multi-store mode allowed? I don't see why not, but does this work correctly
			// with AOT, ATM?
			Map<String, RepositoryConfigurationExtensionSupport> repositoryConfigurationExtensionBeans = getBeanFactory()
					.getBeansOfType(RepositoryConfigurationExtensionSupport.class);

			// repositoryConfigurationExtensionBeans.values().stream()
			// .map(RepositoryConfigurationExtensionSupport::getIdentifyingAnnotations)
			// .flatMap(Collection::stream)
			// .collect(Collectors.toCollection(() -> identifyingAnnotations));

		} catch (Throwable ignore) {
			// Possible BeansException because no bean exists of type RepositoryConfigurationExtension,
			// which included non-Singletons and occurred during eager initialization.
		}

		return identifyingAnnotations;
	}

	private RepositoryInformation resolveRepositoryInformation(RepositoryConfiguration<?> repositoryMetadata) {
		return RepositoryBeanDefinitionReader.readRepositoryInformation(repositoryMetadata, getBeanFactory());
	}

	/**
	 * Helps the AOT processing render the {@link FactoryBean} type correctly that is used to tell the outcome of the
	 * {@link FactoryBean}. We just need to set the target {@link Repository} {@link Class type} of the
	 * {@link RepositoryFactoryBeanSupport} while keeping the actual ID and DomainType set to {@link Object}. If the
	 * generic type signature does not match, then we do not try to resolve and remap the types, but rather set the
	 * {@literal factoryBeanObjectType} attribute on the {@link RootBeanDefinition}.
	 */
	protected void enhanceRepositoryBeanDefinition(RegisteredBean repositoryBean,
			RepositoryConfiguration<?> repositoryMetadata, AotRepositoryContext repositoryContext) {

		logTrace(String.format("Enhancing repository factory bean definition [%s]", repositoryBean.getBeanName()));

		Class<?> repositoryFactoryBeanType = repositoryContext
				.introspectType(repositoryMetadata.getRepositoryFactoryBeanClassName()).resolveType()
				.orElse(RepositoryFactoryBeanSupport.class);

		ResolvableType resolvedRepositoryFactoryBeanType = ResolvableType.forClass(repositoryFactoryBeanType);

		RootBeanDefinition repositoryBeanDefinition = repositoryBean.getMergedBeanDefinition();

		if (isRepositoryWithTypeParameters(resolvedRepositoryFactoryBeanType)) {
			repositoryBeanDefinition.setTargetType(ResolvableType.forClassWithGenerics(repositoryFactoryBeanType,
					repositoryContext.getRepositoryInformation().getRepositoryInterface(), Object.class, Object.class));
		} else {
			repositoryBeanDefinition.setTargetType(resolvedRepositoryFactoryBeanType);
			repositoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE,
					repositoryContext.getRepositoryInformation().getRepositoryInterface());
		}
	}

	/**
	 * {@link BiConsumer Callback} for data module specific contributions.
	 *
	 * @param moduleContribution {@link BiConsumer} used by data modules to submit contributions; can be {@literal null}.
	 * @return this.
	 */

	@SuppressWarnings("unused")
	public RepositoryRegistrationAotContribution withModuleContribution(
			@Nullable BiConsumer<AotRepositoryContext, GenerationContext> moduleContribution) {
		this.moduleContribution = moduleContribution;
		return this;
	}

	@Override
	public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {

		contributeRepositoryInfo(this.repositoryContext, generationContext);
		getModuleContribution().ifPresent(it -> it.accept(getRepositoryContext(), generationContext));
	}

	private void contributeRepositoryInfo(AotRepositoryContext repositoryContext, GenerationContext contribution) {

		RepositoryInformation repositoryInformation = getRepositoryInformation();

		logTrace("Contributing repository information for [%s]", repositoryInformation.getRepositoryInterface());

		// TODO: is this the way?
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

			contribution.getRuntimeHints().reflection().registerType(repositoryFragmentType, hint -> {

				hint.withMembers(MemberCategory.INVOKE_PUBLIC_METHODS);

				if (!repositoryFragmentType.isInterface()) {
					hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
				}
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

		// Reactive Repositories
		if (repositoryInformation.isReactiveRepository()) {
			// TODO: do we still need this and how to configure it?
			// registry.initialization().add(NativeInitializationEntry.ofBuildTimeType(configuration.getRepositoryInterface()));
		}

		// Kotlin
		if (isKotlinCoroutineRepository(repositoryContext, repositoryInformation)) {
			contribution.getRuntimeHints().reflection().registerTypes(kotlinRepositoryReflectionTypeReferences(),
					hint -> hint.withMembers(MemberCategory.INTROSPECT_PUBLIC_METHODS));
		}

		// Repository query methods
		repositoryInformation.getQueryMethods().map(repositoryInformation::getReturnedDomainClass)
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

	// TODO What was this meant to be used for? Was this type filter maybe meant to be used in
	// the TypeContributor.contribute(:Class, :Predicate :GenerationContext) method
	// used in the contributeType(..) method above?
	public Predicate<Class<?>> typeFilter() { // like only document ones. // TODO: As in MongoDB?
		return it -> true;
	}

	private static boolean isRepositoryWithTypeParameters(ResolvableType type) {
		return type.getGenerics().length == 3;
	}
}
