/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.repository.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.data.repository.config.RepositoryFragmentConfiguration;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.Optionals;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link Bean} wrappers.
 *
 * @author Dirk Mahler
 * @author Oliver Gierke
 * @author Mark Paluchs
 * @author Peter Rietzler
 * @author Jens Schauder
 * @author Christoph Strobl
 * @author Ariel Carrera
 */
public abstract class CdiRepositoryBean<T> implements Bean<T>, PassivationCapable {

	private static final Log logger = LogFactory.getLog(CdiRepositoryBean.class);
	private static final CdiRepositoryConfiguration DEFAULT_CONFIGURATION = DefaultCdiRepositoryConfiguration.INSTANCE;

	private final Set<Annotation> qualifiers;
	private final Class<T> repositoryType;
	private final CdiRepositoryContext context;
	private final BeanManager beanManager;
	private final String passivationId;

	private transient @Nullable T repoInstance;

	/**
	 * Creates a new {@link CdiRepositoryBean}.
	 *
	 * @param qualifiers must not be {@literal null}.
	 * @param repositoryType has to be an interface must not be {@literal null}.
	 * @param beanManager the CDI {@link BeanManager}, must not be {@literal null}.
	 */
	public CdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager) {
		this(qualifiers, repositoryType, beanManager, new CdiRepositoryContext(CdiRepositoryBean.class.getClassLoader()));
	}

	/**
	 * Creates a new {@link CdiRepositoryBean}.
	 *
	 * @param qualifiers must not be {@literal null}.
	 * @param repositoryType has to be an interface must not be {@literal null}.
	 * @param beanManager the CDI {@link BeanManager}, must not be {@literal null}.
	 * @param detector detector for the custom repository implementations {@link CustomRepositoryImplementationDetector}.
	 */
	public CdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager,
			Optional<CustomRepositoryImplementationDetector> detector) {

		Assert.notNull(qualifiers, "Qualifiers must not be null!");
		Assert.notNull(beanManager, "BeanManager must not be null!");
		Assert.notNull(repositoryType, "Repoitory type must not be null!");
		Assert.isTrue(repositoryType.isInterface(), "RepositoryType must be an interface!");

		this.qualifiers = qualifiers;
		this.repositoryType = repositoryType;
		this.beanManager = beanManager;
		this.context = new CdiRepositoryContext(getClass().getClassLoader(), detector
				.orElseThrow(() -> new IllegalArgumentException("CustomRepositoryImplementationDetector must be present!")));
		this.passivationId = createPassivationId(qualifiers, repositoryType);
	}

	/**
	 * Creates a new {@link CdiRepositoryBean}.
	 *
	 * @param qualifiers must not be {@literal null}.
	 * @param repositoryType has to be an interface must not be {@literal null}.
	 * @param beanManager the CDI {@link BeanManager}, must not be {@literal null}.
	 * @param context CDI context encapsulating class loader, metadata scanning and fragment detection.
	 * @since 2.1
	 */
	public CdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager,
			CdiRepositoryContext context) {

		Assert.notNull(qualifiers, "Qualifiers must not be null!");
		Assert.notNull(beanManager, "BeanManager must not be null!");
		Assert.notNull(repositoryType, "Repoitory type must not be null!");
		Assert.isTrue(repositoryType.isInterface(), "RepositoryType must be an interface!");

		this.qualifiers = qualifiers;
		this.repositoryType = repositoryType;
		this.beanManager = beanManager;
		this.context = context;
		this.passivationId = createPassivationId(qualifiers, repositoryType);
	}

	/**
	 * Creates a unique identifier for the given repository type and the given annotations.
	 *
	 * @param qualifiers must not be {@literal null} or contain {@literal null} values.
	 * @param repositoryType must not be {@literal null}.
	 * @return
	 */
	private String createPassivationId(Set<Annotation> qualifiers, Class<?> repositoryType) {

		List<String> qualifierNames = new ArrayList<>(qualifiers.size());

		for (Annotation qualifier : qualifiers) {
			qualifierNames.add(qualifier.annotationType().getName());
		}

		Collections.sort(qualifierNames);
		return StringUtils.collectionToDelimitedString(qualifierNames, ":") + ":" + repositoryType.getName();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getTypes()
	 */
	@SuppressWarnings("rawtypes")
	public Set<Type> getTypes() {

		Set<Class> interfaces = new HashSet<>();
		interfaces.add(repositoryType);
		interfaces.addAll(Arrays.asList(repositoryType.getInterfaces()));

		return new HashSet<>(interfaces);
	}

	/**
	 * Returns an instance of an the given {@link Bean}.
	 *
	 * @param bean the {@link Bean} about to create an instance for.
	 * @return the actual component instance.
	 * @see Bean#getTypes()
	 */
	protected <S> S getDependencyInstance(Bean<S> bean) {
		return getDependencyInstance(bean, bean.getBeanClass());
	}

	/**
	 * Returns an instance of an the given {@link Bean} and allows to be specific about the type that is about to be
	 * created.
	 *
	 * @param bean the {@link Bean} about to create an instance for.
	 * @param type the expected type of the component instance created for that {@link Bean}. We need to hand this
	 *          parameter explicitly as the {@link Bean} might carry multiple types but the primary one might not be the
	 *          first, i.e. the one returned by {@link Bean#getBeanClass()}.
	 * @return the actual component instance.
	 * @see Bean#getTypes()
	 */
	@SuppressWarnings("unchecked")
	protected <S> S getDependencyInstance(Bean<S> bean, Class<?> type) {
		CreationalContext<S> creationalContext = beanManager.createCreationalContext(bean);
		return (S) beanManager.getReference(bean, type, creationalContext);
	}

	/**
	 * Forces the initialization of bean target.
	 */
	public final void initialize() {
		create(beanManager.createCreationalContext(this));
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.context.spi.Contextual#create(javax.enterprise.context.spi.CreationalContext)
	 */
	public final T create(@SuppressWarnings("null") CreationalContext<T> creationalContext) {

		T repoInstance = this.repoInstance;

		if (repoInstance != null) {
			logger.debug(LogMessage.format("Returning eagerly created CDI repository instance for %s.", repositoryType.getName()));
			return repoInstance;
		}

		logger.debug(LogMessage.format("Creating CDI repository bean instance for %s.", repositoryType.getName()));
		repoInstance = create(creationalContext, repositoryType);
		this.repoInstance = repoInstance;

		return repoInstance;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.context.spi.Contextual#destroy(java.lang.Object, javax.enterprise.context.spi.CreationalContext)
	 */
	public void destroy(@SuppressWarnings("null") T instance,
			@SuppressWarnings("null") CreationalContext<T> creationalContext) {

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Destroying bean instance %s for repository type '%s'.", instance.toString(),
					repositoryType.getName()));
		}

		creationalContext.release();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getQualifiers()
	 */
	public Set<Annotation> getQualifiers() {
		return qualifiers;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getName()
	 */
	public String getName() {
		return repositoryType.getName();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getStereotypes()
	 */
	public Set<Class<? extends Annotation>> getStereotypes() {

		return Arrays.stream(repositoryType.getAnnotations())//
				.map(Annotation::annotationType)//
				.filter(it -> it.isAnnotationPresent(Stereotype.class))//
				.collect(Collectors.toSet());
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getBeanClass()
	 */
	public Class<?> getBeanClass() {
		return repositoryType;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#isAlternative()
	 */
	public boolean isAlternative() {
		return repositoryType.isAnnotationPresent(Alternative.class);
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#isNullable()
	 */
	public boolean isNullable() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getInjectionPoints()
	 */
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.Bean#getScope()
	 */
	public Class<? extends Annotation> getScope() {
		return ApplicationScoped.class;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.enterprise.inject.spi.PassivationCapable#getId()
	 */
	public String getId() {
		return passivationId;
	}

	/**
	 * Creates the actual component instance.
	 *
	 * @param creationalContext will never be {@literal null}.
	 * @param repositoryType will never be {@literal null}.
	 * @return
	 */
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {

		CdiRepositoryConfiguration cdiRepositoryConfiguration = lookupConfiguration(beanManager, qualifiers);

		Optional<Bean<?>> customImplementationBean = getCustomImplementationBean(repositoryType,
				cdiRepositoryConfiguration);
		Optional<Object> customImplementation = customImplementationBean.map(this::getDependencyInstance);

		return create(creationalContext, repositoryType, customImplementation);
	}

	/**
	 * Creates the actual component instance given a {@link RepositoryFactorySupport repository factory supplier} and the
	 * repository {@link Class type}. This method is an utility for to create a repository. This method will obtain a
	 * {@link RepositoryFactorySupport repository factory} and configure it with {@link CdiRepositoryConfiguration}.
	 *
	 * @param factorySupplier must not be {@literal null}.
	 * @param repositoryType must not be {@literal null}.
	 * @return
	 * @since 2.1
	 */
	protected T create(Supplier<? extends RepositoryFactorySupport> factorySupplier, Class<T> repositoryType) {

		CdiRepositoryConfiguration configuration = lookupConfiguration(beanManager, qualifiers);
		RepositoryFragments repositoryFragments = getRepositoryFragments(repositoryType, configuration);

		RepositoryFactorySupport factory = factorySupplier.get();

		applyConfiguration(factory, configuration);

		return create(factory, repositoryType, repositoryFragments);
	}

	/**
	 * Lookup repository fragments for a {@link Class repository interface}.
	 *
	 * @param repositoryType must not be {@literal null}.
	 * @return the {@link RepositoryFragments}.
	 * @since 2.1
	 */
	protected RepositoryFragments getRepositoryFragments(Class<T> repositoryType) {

		Assert.notNull(repositoryType, "Repository type must not be null!");

		CdiRepositoryConfiguration cdiRepositoryConfiguration = lookupConfiguration(beanManager, qualifiers);

		return getRepositoryFragments(repositoryType, cdiRepositoryConfiguration);
	}

	private RepositoryFragments getRepositoryFragments(Class<T> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		Optional<Bean<?>> customImplementationBean = getCustomImplementationBean(repositoryType,
				cdiRepositoryConfiguration);
		Optional<Object> customImplementation = customImplementationBean.map(this::getDependencyInstance);

		List<RepositoryFragment<?>> repositoryFragments = findRepositoryFragments(repositoryType,
				cdiRepositoryConfiguration);

		RepositoryFragments customImplementationFragment = customImplementation //
				.map(RepositoryFragments::just) //
				.orElseGet(RepositoryFragments::empty);

		return RepositoryFragments.from(repositoryFragments) //
				.append(customImplementationFragment);
	}

	@SuppressWarnings("unchecked")
	private List<RepositoryFragment<?>> findRepositoryFragments(Class<T> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		Stream<RepositoryFragmentConfiguration> fragmentConfigurations = context
				.getRepositoryFragments(cdiRepositoryConfiguration, repositoryType);

		return fragmentConfigurations.flatMap(it -> {

			Class<Object> interfaceClass = (Class<Object>) lookupFragmentInterface(repositoryType, it.getInterfaceName());
			Class<?> implementationClass = context.loadClass(it.getClassName());
			Optional<Bean<?>> bean = getBean(implementationClass, beanManager, qualifiers);

			return Optionals.toStream(bean.map(this::getDependencyInstance) //
					.map(implementation -> RepositoryFragment.implemented(interfaceClass, implementation))); //

		}).collect(Collectors.toList());
	}

	private static Class<?> lookupFragmentInterface(Class<?> repositoryType, String interfaceName) {

		return Arrays.stream(repositoryType.getInterfaces()) //
				.filter(it -> it.getName().equals(interfaceName)) //
				.findFirst() //
				.orElseThrow(() -> new IllegalArgumentException(String.format("Did not find type %s in %s!", interfaceName,
						Arrays.asList(repositoryType.getInterfaces()))));
	}

	/**
	 * Creates the actual component instance.
	 *
	 * @param creationalContext will never be {@literal null}.
	 * @param repositoryType will never be {@literal null}.
	 * @param customImplementation can be {@literal null}.
	 * @return
	 * @deprecated since 2.1, override {@link #create(CreationalContext, Class)} in which you create a repository factory
	 *             and call {@link #create(RepositoryFactorySupport, Class, RepositoryFragments)}.
	 */
	@Deprecated
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType,
			Optional<Object> customImplementation) {
		throw new UnsupportedOperationException(
				"You have to implement create(CreationalContext<T>, Class<T>, Optional<Object>) "
						+ "in order to use custom repository implementations");
	}

	/**
	 * Looks up an instance of a {@link CdiRepositoryConfiguration}. In case the instance cannot be found within the CDI
	 * scope, a default configuration is used.
	 *
	 * @return an available CdiRepositoryConfiguration instance or a default configuration.
	 */
	protected CdiRepositoryConfiguration lookupConfiguration(BeanManager beanManager, Set<Annotation> qualifiers) {

		return beanManager.getBeans(CdiRepositoryConfiguration.class, getQualifiersArray(qualifiers)).stream().findFirst()//
				.map(it -> (CdiRepositoryConfiguration) getDependencyInstance(it)) //
				.orElse(DEFAULT_CONFIGURATION);
	}

	/**
	 * Try to lookup a custom implementation for a {@link org.springframework.data.repository.Repository}. Can only be
	 * used when a {@code CustomRepositoryImplementationDetector} is provided.
	 *
	 * @param repositoryType
	 * @param cdiRepositoryConfiguration
	 * @return the custom implementation instance or null
	 */
	private Optional<Bean<?>> getCustomImplementationBean(Class<?> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		return context.getCustomImplementationClass(repositoryType, cdiRepositoryConfiguration)//
				.flatMap(type -> getBean(type, beanManager, qualifiers));
	}

	/**
	 * Applies the configuration from {@link CdiRepositoryConfiguration} to {@link RepositoryFactorySupport} by looking up
	 * the actual configuration.
	 *
	 * @param repositoryFactory will never be {@literal null}.
	 * @since 2.1
	 */
	protected void applyConfiguration(RepositoryFactorySupport repositoryFactory) {
		applyConfiguration(repositoryFactory, lookupConfiguration(beanManager, qualifiers));
	}

	/**
	 * Applies the configuration from {@link CdiRepositoryConfiguration} to {@link RepositoryFactorySupport} by looking up
	 * the actual configuration.
	 *
	 * @param repositoryFactory will never be {@literal null}.
	 * @param configuration will never be {@literal null}.
	 * @since 2.1
	 */
	protected static void applyConfiguration(RepositoryFactorySupport repositoryFactory,
			CdiRepositoryConfiguration configuration) {

		configuration.getEvaluationContextProvider().ifPresent(repositoryFactory::setEvaluationContextProvider);
		configuration.getNamedQueries().ifPresent(repositoryFactory::setNamedQueries);
		configuration.getQueryLookupStrategy().ifPresent(repositoryFactory::setQueryLookupStrategyKey);
		configuration.getRepositoryBeanClass().ifPresent(repositoryFactory::setRepositoryBaseClass);
		configuration.getRepositoryProxyPostProcessors().forEach(repositoryFactory::addRepositoryProxyPostProcessor);
		configuration.getQueryCreationListeners().forEach(repositoryFactory::addQueryCreationListener);
	}

	/**
	 * Creates the actual repository instance.
	 *
	 * @param repositoryType will never be {@literal null}.
	 * @param repositoryFragments will never be {@literal null}.
	 * @return
	 */
	protected static <T> T create(RepositoryFactorySupport repositoryFactory, Class<T> repositoryType,
			RepositoryFragments repositoryFragments) {
		return repositoryFactory.getRepository(repositoryType, repositoryFragments);
	}

	private static Optional<Bean<?>> getBean(Class<?> beanType, BeanManager beanManager, Set<Annotation> qualifiers) {
		return beanManager.getBeans(beanType, getQualifiersArray(qualifiers)).stream().findFirst();
	}

	private static Annotation[] getQualifiersArray(Set<Annotation> qualifiers) {
		return qualifiers.toArray(new Annotation[qualifiers.size()]);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("CdiRepositoryBean: type='%s', qualifiers=%s", repositoryType.getName(),
				qualifiers.toString());
	}

	enum DefaultCdiRepositoryConfiguration implements CdiRepositoryConfiguration {
		INSTANCE;
	}
}
