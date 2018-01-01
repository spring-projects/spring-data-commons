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
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.data.repository.config.DefaultRepositoryConfiguration;
import org.springframework.data.repository.config.RepositoryBeanNameGenerator;
import org.springframework.data.repository.config.SpringDataAnnotationBeanNameGenerator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link Bean} wrappers.
 *
 * @author Dirk Mahler
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Peter Rietzler
 * @author Jens Schauder
 * @author Christoph Strobl
 */
public abstract class CdiRepositoryBean<T> implements Bean<T>, PassivationCapable {

	private static final Logger LOGGER = LoggerFactory.getLogger(CdiRepositoryBean.class);
	private static final CdiRepositoryConfiguration DEFAULT_CONFIGURATION = DefaultCdiRepositoryConfiguration.INSTANCE;

	private final Set<Annotation> qualifiers;
	private final Class<T> repositoryType;
	private final Optional<CustomRepositoryImplementationDetector> detector;
	private final BeanManager beanManager;
	private final String passivationId;

	private transient @Nullable T repoInstance;

	private final SpringDataAnnotationBeanNameGenerator annotationBeanNameGenerator = new SpringDataAnnotationBeanNameGenerator();
	private final RepositoryBeanNameGenerator beanNameGenerator = new RepositoryBeanNameGenerator(
			getClass().getClassLoader());

	/**
	 * Creates a new {@link CdiRepositoryBean}.
	 *
	 * @param qualifiers must not be {@literal null}.
	 * @param repositoryType has to be an interface must not be {@literal null}.
	 * @param beanManager the CDI {@link BeanManager}, must not be {@literal null}.
	 */
	public CdiRepositoryBean(Set<Annotation> qualifiers, Class<T> repositoryType, BeanManager beanManager) {
		this(qualifiers, repositoryType, beanManager, Optional.empty());
	}

	/**
	 * Creates a new {@link CdiRepositoryBean}.
	 *
	 * @param qualifiers must not be {@literal null}.
	 * @param repositoryType has to be an interface must not be {@literal null}.
	 * @param beanManager the CDI {@link BeanManager}, must not be {@literal null}.
	 * @param detector detector for the custom repository implementations {@link CustomRepositoryImplementationDetector},
	 *          can be {@literal null}.
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
		this.detector = detector;
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
			LOGGER.debug("Returning eagerly created CDI repository instance for {}.", repositoryType.getName());
			return repoInstance;
		}

		LOGGER.debug("Creating CDI repository bean instance for {}.", repositoryType.getName());
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

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Destroying bean instance %s for repository type '%s'.", instance.toString(),
					repositoryType.getName()));
		}

		creationalContext.release();
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
	 * @param beanManager
	 * @param qualifiers
	 * @return the custom implementation instance or null
	 */
	private Optional<Bean<?>> getCustomImplementationBean(Class<?> repositoryType, BeanManager beanManager,
			Set<Annotation> qualifiers) {

		return detector.flatMap(it -> {

			CdiRepositoryConfiguration cdiRepositoryConfiguration = lookupConfiguration(beanManager, qualifiers);

			return getCustomImplementationClass(repositoryType, cdiRepositoryConfiguration, it)//
					.flatMap(type -> beanManager.getBeans(type, getQualifiersArray(qualifiers)).stream().findFirst());
		});
	}

	/**
	 * Retrieves a custom repository interfaces from a repository type. This works for the whole class hierarchy and can
	 * find also a custom repository which is inherited over many levels.
	 *
	 * @param repositoryType The class representing the repository.
	 * @param cdiRepositoryConfiguration The configuration for CDI usage.
	 * @return the interface class or {@literal null}.
	 */
	private Optional<Class<?>> getCustomImplementationClass(Class<?> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration, CustomRepositoryImplementationDetector detector) {

		String className = getCustomImplementationClassName(repositoryType, cdiRepositoryConfiguration);
		Optional<AbstractBeanDefinition> beanDefinition = detector.detectCustomImplementation( //
				className, //
				getCustomImplementationBeanName(repositoryType), //
				Collections.singleton(repositoryType.getPackage().getName()), //
				Collections.emptySet(), //
				beanNameGenerator::generateBeanName //
		);

		return beanDefinition.map(it -> {

			try {
				return Class.forName(it.getBeanClassName());
			} catch (ClassNotFoundException e) {
				throw new UnsatisfiedResolutionException(
						String.format("Unable to resolve class for '%s'", it.getBeanClassName()), e);
			}
		});
	}

	private String getCustomImplementationBeanName(Class<?> repositoryType) {
		return annotationBeanNameGenerator.generateBeanName(new AnnotatedGenericBeanDefinition(repositoryType))
				+ DEFAULT_CONFIGURATION.getRepositoryImplementationPostfix();
	}

	private String getCustomImplementationClassName(Class<?> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		String configuredPostfix = cdiRepositoryConfiguration.getRepositoryImplementationPostfix();
		Assert.hasText(configuredPostfix, "Configured repository postfix must not be null or empty!");

		return ClassUtils.getShortName(repositoryType) + configuredPostfix;
	}

	private Annotation[] getQualifiersArray(Set<Annotation> qualifiers) {
		return qualifiers.toArray(new Annotation[qualifiers.size()]);
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
	 * @deprecated override {@link #create(CreationalContext, Class, Object)} instead.
	 */
	@Deprecated
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType) {

		Optional<Bean<?>> customImplementationBean = getCustomImplementationBean(repositoryType, beanManager, qualifiers);
		Optional<Object> customImplementation = customImplementationBean
				.map(it -> beanManager.getReference(it, it.getBeanClass(), beanManager.createCreationalContext(it)));

		return create(creationalContext, repositoryType, customImplementation);
	}

	/**
	 * Creates the actual component instance.
	 *
	 * @param creationalContext will never be {@literal null}.
	 * @param repositoryType will never be {@literal null}.
	 * @param customImplementation can be {@literal null}.
	 * @return
	 */
	protected T create(CreationalContext<T> creationalContext, Class<T> repositoryType,
			Optional<Object> customImplementation) {
		throw new UnsupportedOperationException(
				"You have to implement create(CreationalContext<T>, Class<T>, Optional<Object>) "
						+ "in order to use custom repository implementations");
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

	static enum DefaultCdiRepositoryConfiguration implements CdiRepositoryConfiguration {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.cdi.CdiRepositoryConfiguration#getRepositoryImplementationPostfix()
		 */
		@Override
		public String getRepositoryImplementationPostfix() {
			return DefaultRepositoryConfiguration.DEFAULT_REPOSITORY_IMPLEMENTATION_POSTFIX;
		}
	}
}
