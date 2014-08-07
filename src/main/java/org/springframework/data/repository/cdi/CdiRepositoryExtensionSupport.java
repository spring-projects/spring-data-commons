/*
 * Copyright 2011-2013 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;
import org.springframework.data.repository.config.DefaultRepositoryConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base class for {@link Extension} implementations that create instances for Spring Data repositories.
 * 
 * @author Dirk Mahler
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public abstract class CdiRepositoryExtensionSupport implements Extension {

	private static final Logger LOGGER = LoggerFactory.getLogger(CdiRepositoryExtensionSupport.class);
	private static final CdiRepositoryConfiguration DEFAULT_CONFIGURATION = DefaultCdiRepositoryConfiguration.INSTANCE;

	private final Map<Class<?>, Set<Annotation>> repositoryTypes = new HashMap<Class<?>, Set<Annotation>>();
	private final Set<CdiRepositoryBean<?>> eagerRepositories = new HashSet<CdiRepositoryBean<?>>();
	private final CustomRepositoryImplementationDetector customImplementationDetector;

	protected CdiRepositoryExtensionSupport() {

		Environment environment = new StandardEnvironment();
		ResourceLoader resourceLoader = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);

		this.customImplementationDetector = new CustomRepositoryImplementationDetector(metadataReaderFactory, environment,
				resourceLoader);
	}

	/**
	 * Implementation of a an observer which checks for Spring Data repository types and stores them in
	 * {@link #repositoryTypes} for later registration as bean type.
	 * 
	 * @param <X> The type.
	 * @param processAnnotatedType The annotated type as defined by CDI.
	 */
	protected <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> processAnnotatedType) {

		AnnotatedType<X> annotatedType = processAnnotatedType.getAnnotatedType();
		Class<X> repositoryType = annotatedType.getJavaClass();

		if (isRepository(repositoryType)) {

			// Determine the qualifiers of the repository type.
			Set<Annotation> qualifiers = getQualifiers(repositoryType);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(String.format("Discovered repository type '%s' with qualifiers %s.", repositoryType.getName(),
						qualifiers));
			}
			// Store the repository type using its qualifiers.
			repositoryTypes.put(repositoryType, qualifiers);
		}
	}

	/**
	 * Returns whether the given type is a repository type.
	 * 
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private boolean isRepository(Class<?> type) {

		boolean isInterface = type.isInterface();
		boolean extendsRepository = Repository.class.isAssignableFrom(type);
		boolean isAnnotated = type.isAnnotationPresent(RepositoryDefinition.class);
		boolean excludedByAnnotation = type.isAnnotationPresent(NoRepositoryBean.class);

		return isInterface && (extendsRepository || isAnnotated) && !excludedByAnnotation;
	}

	/**
	 * Determines the qualifiers of the given type.
	 */
	private Set<Annotation> getQualifiers(final Class<?> type) {

		Set<Annotation> qualifiers = new HashSet<Annotation>();
		Annotation[] annotations = type.getAnnotations();
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> annotationType = annotation.annotationType();
			if (annotationType.isAnnotationPresent(Qualifier.class)) {
				qualifiers.add(annotation);
			}
		}

		// Add @Default qualifier if no qualifier is specified.
		if (qualifiers.isEmpty()) {
			qualifiers.add(DefaultAnnotationLiteral.INSTANCE);
		}

		// Add @Any qualifier.
		qualifiers.add(AnyAnnotationLiteral.INSTANCE);
		return qualifiers;
	}

	/**
	 * Triggers the eager initialization of beans registered for that behavior.
	 * 
	 * @param event must not be {@literal null}.
	 * @param manager must not be {@literal null}.
	 * @see #registerBean(CdiRepositoryBean)
	 */
	void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager manager) {

		for (CdiRepositoryBean<?> bean : eagerRepositories) {

			LOGGER.debug("Eagerly instantiating CDI repository bean for {}.", bean.getBeanClass());
			bean.initialize();
		}
	}

	/**
	 * Provides access to all repository types as well as their qualifiers.
	 * 
	 * @return
	 */
	protected Iterable<Entry<Class<?>, Set<Annotation>>> getRepositoryTypes() {
		return repositoryTypes.entrySet();
	}

	/**
	 * Registers the given {@link CdiRepositoryBean} for further general treatment by the infrastructure. In particular,
	 * this will cause repositories to be instantiated eagerly if marked as such.
	 * 
	 * @param bean must not be {@literal null}.
	 * @see #afterDeploymentValidation(AfterDeploymentValidation, BeanManager)
	 */
	protected void registerBean(CdiRepositoryBean<?> bean) {

		Class<?> repositoryInterface = bean.getBeanClass();

		if (AnnotationUtils.findAnnotation(repositoryInterface, Eager.class) != null) {
			this.eagerRepositories.add(bean);
		}
	}

	/**
	 * Looks up an instance of a {@link CdiRepositoryConfiguration}. In case the instance cannot be found within the CDI
	 * scope, a default configuration is used.
	 * 
	 * @return an available CdiRepositoryConfiguration instance or a default configuration.
	 */
	protected CdiRepositoryConfiguration lookupConfiguration(BeanManager beanManager, Set<Annotation> qualifiers) {

		Set<Bean<?>> beans = beanManager.getBeans(CdiRepositoryConfiguration.class, getQualifiersArray(qualifiers));

		if (beans.isEmpty()) {
			return DEFAULT_CONFIGURATION;
		}

		Bean<?> bean = beans.iterator().next();
		CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);

		return (CdiRepositoryConfiguration) beanManager.getReference(bean, CdiRepositoryConfiguration.class,
				creationalContext);
	}

	private Annotation[] getQualifiersArray(Set<Annotation> qualifiers) {
		return qualifiers.toArray(new Annotation[qualifiers.size()]);
	}

	/**
	 * Try to lookup a custom implementation for a {@link org.springframework.data.repository.Repository}.
	 * 
	 * @param repositoryType
	 * @param beanManager
	 * @param qualifiers
	 * @return the custom implementation instance or null
	 */
	protected Bean<?> getCustomImplementationBean(Class<?> repositoryType, BeanManager beanManager,
			Set<Annotation> qualifiers) {

		CdiRepositoryConfiguration cdiRepositoryConfiguration = lookupConfiguration(beanManager, qualifiers);
		Class<?> customImplementationClass = getCustomImplementationClass(repositoryType, cdiRepositoryConfiguration);

		if (customImplementationClass == null) {
			return null;
		}

		Set<Bean<?>> beans = beanManager.getBeans(customImplementationClass, getQualifiersArray(qualifiers));
		return beans.isEmpty() ? null : beans.iterator().next();
	}

	/**
	 * Retrieves a custom repository interfaces from a repository type. This works for the whole class hierarchy and can
	 * find also a custom repo which is inherieted over many levels.
	 * 
	 * @param repositoryType The class representing the repository.
	 * @param cdiRepositoryConfiguration The configuration for CDI usage.
	 * @return the interface class or {@literal null}.
	 */
	private Class<?> getCustomImplementationClass(Class<?> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		String className = getCustomImplementationClassName(repositoryType, cdiRepositoryConfiguration);
		AbstractBeanDefinition beanDefinition = customImplementationDetector.detectCustomImplementation(className,
				Collections.singleton(repositoryType.getPackage().getName()));

		if (beanDefinition == null) {
			return null;
		}

		try {
			return Class.forName(beanDefinition.getBeanClassName());
		} catch (ClassNotFoundException e) {
			throw new UnsatisfiedResolutionException(String.format("Unable to resolve class for '%s'",
					beanDefinition.getBeanClassName()), e);
		}
	}

	private String getCustomImplementationClassName(Class<?> repositoryType,
			CdiRepositoryConfiguration cdiRepositoryConfiguration) {

		String configuredPostfix = cdiRepositoryConfiguration.getRepositoryImplementationPostfix();
		Assert.hasText(configuredPostfix, "Configured repository postfix must not be null or empty!");

		return ClassUtils.getShortName(repositoryType) + configuredPostfix;
	}

	@SuppressWarnings("all")
	static class DefaultAnnotationLiteral extends AnnotationLiteral<Default> implements Default {

		private static final long serialVersionUID = 511359421048623933L;
		private static final DefaultAnnotationLiteral INSTANCE = new DefaultAnnotationLiteral();
	}

	@SuppressWarnings("all")
	static class AnyAnnotationLiteral extends AnnotationLiteral<Any> implements Any {

		private static final long serialVersionUID = 7261821376671361463L;
		private static final AnyAnnotationLiteral INSTANCE = new AnyAnnotationLiteral();
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
