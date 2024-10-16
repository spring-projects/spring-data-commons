/*
 * Copyright 2008-2024 the original author or authors.
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
package org.springframework.data.repository.core.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.query.ExtensionAwareQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.QueryMethodValueEvaluationContextAccessor;
import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.util.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Adapter for Spring's {@link FactoryBean} interface to allow easy setup of repository factories via Spring
 * configuration.
 * <p>
 * Subclasses may pass-thru generics, provide a fixed domain, provide a fixed identifier type, or provide additional
 * generic type parameters. Type parameters must appear in the same order the ones from this class (repository type,
 * entity type, identifier type, additional type parameters). Using a different ordering will result in invalid type
 * definitions.
 *
 * @param <T> the type of the repository.
 * @param <S> the entity type.
 * @param <ID> the entity identifier type.
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Johannes Englmeier
 */
public abstract class RepositoryFactoryBeanSupport<T extends Repository<S, ID>, S, ID>
		implements InitializingBean, RepositoryFactoryInformation<S, ID>, FactoryBean<T>, ApplicationEventPublisherAware,
		BeanClassLoaderAware, BeanFactoryAware, EnvironmentAware {

	private final Class<? extends T> repositoryInterface;

	private RepositoryFactorySupport factory;
	private boolean exposeMetadata;
	private Key queryLookupStrategyKey;
	private Optional<Class<?>> repositoryBaseClass = Optional.empty();
	private Optional<Object> customImplementation = Optional.empty();
	private Optional<RepositoryFragments> repositoryFragments = Optional.empty();
	private NamedQueries namedQueries = PropertiesBasedNamedQueries.EMPTY;
	private Optional<MappingContext<?, ?>> mappingContext = Optional.empty();
	private ClassLoader classLoader;
	private ApplicationEventPublisher publisher;
	private BeanFactory beanFactory;
	private Environment environment;
	private boolean lazyInit = false;
	private Optional<EvaluationContextProvider> evaluationContextProvider = Optional.empty();
	private final List<RepositoryFactoryCustomizer> repositoryFactoryCustomizers = new ArrayList<>();

	private Lazy<T> repository;

	private RepositoryMetadata repositoryMetadata;

	/**
	 * Creates a new {@link RepositoryFactoryBeanSupport} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	protected RepositoryFactoryBeanSupport(Class<? extends T> repositoryInterface) {

		Assert.notNull(repositoryInterface, "Repository interface must not be null");
		this.repositoryInterface = repositoryInterface;
	}

	/**
	 * Configures the repository base class to be used.
	 *
	 * @param repositoryBaseClass the repositoryBaseClass to set, can be {@literal null}.
	 * @since 1.11
	 */
	public void setRepositoryBaseClass(Class<?> repositoryBaseClass) {
		this.repositoryBaseClass = Optional.ofNullable(repositoryBaseClass);
	}

	/**
	 * Set whether the repository method metadata should be exposed by the repository factory as a ThreadLocal for
	 * retrieval via the {@code RepositoryMethodContext} class. This is useful if an advised object needs to obtain
	 * repository information.
	 * <p>
	 * Default is "false", in order to avoid unnecessary extra interception. This means that no guarantees are provided
	 * that {@code RepositoryMethodContext} access will work consistently within any method of the advised object.
	 *
	 * @since 3.4
	 */
	public void setExposeMetadata(boolean exposeMetadata) {
		this.exposeMetadata = exposeMetadata;
	}

	/**
	 * Set the {@link QueryLookupStrategy.Key} to be used.
	 *
	 * @param queryLookupStrategyKey
	 */
	public void setQueryLookupStrategyKey(Key queryLookupStrategyKey) {
		this.queryLookupStrategyKey = queryLookupStrategyKey;
	}

	/**
	 * Setter to inject a custom repository implementation.
	 *
	 * @param customImplementation
	 */
	public void setCustomImplementation(Object customImplementation) {
		this.customImplementation = Optional.of(customImplementation);
	}

	/**
	 * Setter to inject repository fragments.
	 *
	 * @param repositoryFragments
	 */
	public void setRepositoryFragments(RepositoryFragments repositoryFragments) {
		this.repositoryFragments = Optional.of(repositoryFragments);
	}

	/**
	 * Setter to inject a {@link NamedQueries} instance.
	 *
	 * @param namedQueries the namedQueries to set
	 */
	public void setNamedQueries(NamedQueries namedQueries) {
		this.namedQueries = namedQueries;
	}

	/**
	 * Configures the {@link MappingContext} to be used to lookup {@link PersistentEntity} instances for
	 * {@link #getPersistentEntity()}.
	 *
	 * @param mappingContext
	 */
	protected void setMappingContext(MappingContext<?, ?> mappingContext) {
		this.mappingContext = Optional.of(mappingContext);
	}

	/**
	 * Sets the {@link EvaluationContextProvider} to be used to evaluate SpEL expressions in manually defined queries.
	 *
	 * @param evaluationContextProvider must not be {@literal null}.
	 * @since 3.4
	 */
	public void setEvaluationContextProvider(EvaluationContextProvider evaluationContextProvider) {
		this.evaluationContextProvider = Optional.of(evaluationContextProvider);
	}

	/**
	 * Sets the {@link QueryMethodEvaluationContextProvider} to be used to evaluate SpEL expressions in manually defined
	 * queries.
	 *
	 * @param evaluationContextProvider must not be {@literal null}.
	 * @deprecated since 3.4, use {@link #setEvaluationContextProvider(EvaluationContextProvider)} instead.
	 */
	@Deprecated(since = "3.4")
	public void setEvaluationContextProvider(QueryMethodEvaluationContextProvider evaluationContextProvider) {
		setEvaluationContextProvider(evaluationContextProvider.getEvaluationContextProvider());
	}

	/**
	 * Register a {@link RepositoryFactoryCustomizer} to customize the {@link RepositoryFactorySupport repository factor}
	 * before creating the repository.
	 *
	 * @param customizer must not be {@literal null}.
	 * @since 2.4
	 */
	public void addRepositoryFactoryCustomizer(RepositoryFactoryCustomizer customizer) {

		Assert.notNull(customizer, "RepositoryFactoryCustomizer must not be null");
		this.repositoryFactoryCustomizers.add(customizer);
	}

	/**
	 * Configures whether to initialize the repository proxy lazily. This defaults to {@literal false}.
	 *
	 * @param lazy whether to initialize the repository proxy lazily. This defaults to {@literal false}.
	 */
	public void setLazyInit(boolean lazy) {
		this.lazyInit = lazy;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		this.beanFactory = beanFactory;

		if (this.evaluationContextProvider.isEmpty() && beanFactory instanceof ListableBeanFactory lbf) {
			this.evaluationContextProvider = createDefaultEvaluationContextProvider(lbf);
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Create a default {@link EvaluationContextProvider} (or subclass) from {@link ListableBeanFactory}.
	 *
	 * @param beanFactory the bean factory to use.
	 * @return the default instance. May be {@link Optional#empty()}.
	 * @since 3.4
	 */
	protected Optional<EvaluationContextProvider> createDefaultEvaluationContextProvider(
			ListableBeanFactory beanFactory) {
		return createDefaultQueryMethodEvaluationContextProvider(beanFactory)
				.map(QueryMethodEvaluationContextProvider::getEvaluationContextProvider);
	}

	/**
	 * Create a default {@link QueryMethodEvaluationContextProvider} (or subclass) from {@link ListableBeanFactory}.
	 *
	 * @param beanFactory the bean factory to use.
	 * @return the default instance. May be {@link Optional#empty()}.
	 * @since 2.4
	 * @deprecated since 3.4, use {@link #createDefaultEvaluationContextProvider(ListableBeanFactory)} instead.
	 */
	@Deprecated(since = "3.4")
	protected Optional<QueryMethodEvaluationContextProvider> createDefaultQueryMethodEvaluationContextProvider(
			ListableBeanFactory beanFactory) {
		return Optional.of(new ExtensionAwareQueryMethodEvaluationContextProvider(beanFactory));
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EntityInformation<S, ID> getEntityInformation() {
		return (EntityInformation<S, ID>) factory.getEntityInformation(repositoryMetadata.getDomainType());
	}

	@Override
	public RepositoryInformation getRepositoryInformation() {

		RepositoryFragments fragments = customImplementation.map(RepositoryFragments::just)//
				.orElse(RepositoryFragments.empty());

		return factory.getRepositoryInformation(repositoryMetadata, fragments);
	}

	@Override
	public PersistentEntity<?, ?> getPersistentEntity() {

		return mappingContext.orElseThrow(() -> new IllegalStateException("No MappingContext available"))
				.getRequiredPersistentEntity(repositoryMetadata.getDomainType());
	}

	@Override
	public List<QueryMethod> getQueryMethods() {
		return factory.getQueryMethods();
	}

	@Override
	@NonNull
	public T getObject() {
		return this.repository.get();
	}

	@Override
	@NonNull
	public Class<? extends T> getObjectType() {
		return repositoryInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() {

		this.factory = createRepositoryFactory();
		this.factory.setExposeMetadata(exposeMetadata);
		this.factory.setQueryLookupStrategyKey(queryLookupStrategyKey);
		this.factory.setNamedQueries(namedQueries);
		this.factory.setEvaluationContextProvider(
				evaluationContextProvider.orElse(QueryMethodValueEvaluationContextAccessor.DEFAULT_CONTEXT_PROVIDER));
		this.factory.setBeanClassLoader(classLoader);
		this.factory.setBeanFactory(beanFactory);

		if (this.publisher != null) {
			this.factory.addRepositoryProxyPostProcessor(new EventPublishingRepositoryProxyPostProcessor(publisher));
		}

		if (this.environment != null) {
			this.factory.setEnvironment(this.environment);
		}

		repositoryBaseClass.ifPresent(this.factory::setRepositoryBaseClass);

		this.repositoryFactoryCustomizers.forEach(customizer -> customizer.customize(this.factory));

		RepositoryFragments customImplementationFragment = customImplementation //
				.map(RepositoryFragments::just) //
				.orElseGet(RepositoryFragments::empty);

		RepositoryFragments repositoryFragmentsToUse = this.repositoryFragments //
				.orElseGet(RepositoryFragments::empty) //
				.append(customImplementationFragment);

		this.repositoryMetadata = this.factory.getRepositoryMetadata(repositoryInterface);

		this.repository = Lazy.of(() -> this.factory.getRepository(repositoryInterface, repositoryFragmentsToUse));

		// Make sure the aggregate root type is present in the MappingContext (e.g. for auditing)
		this.mappingContext.ifPresent(it -> it.getPersistentEntity(repositoryMetadata.getDomainType()));

		if (!lazyInit) {
			this.repository.get();
		}
	}

	/**
	 * Create the actual {@link RepositoryFactorySupport} instance.
	 *
	 * @return
	 */
	protected abstract RepositoryFactorySupport createRepositoryFactory();
}
