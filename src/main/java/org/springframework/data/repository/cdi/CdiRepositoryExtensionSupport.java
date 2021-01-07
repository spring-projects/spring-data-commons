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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.log.LogMessage;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;

/**
 * Base class for {@link Extension} implementations that create instances for Spring Data repositories.
 *
 * @author Dirk Mahler
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 */
public abstract class CdiRepositoryExtensionSupport implements Extension {

	private static final Log logger = LogFactory.getLog(CdiRepositoryExtensionSupport.class);

	private final Map<Class<?>, Set<Annotation>> repositoryTypes = new HashMap<>();
	private final Set<CdiRepositoryBean<?>> eagerRepositories = new HashSet<>();
	private final CdiRepositoryContext context;

	protected CdiRepositoryExtensionSupport() {
		context = new CdiRepositoryContext(getClass().getClassLoader());
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

			if (logger.isDebugEnabled()) {
				logger.debug(
						String.format("Discovered repository type '%s' with qualifiers %s.", repositoryType.getName(), qualifiers));
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

		Set<Annotation> qualifiers = new HashSet<>();
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

			logger.debug(LogMessage.format("Eagerly instantiating CDI repository bean for %s.", bean.getBeanClass()));
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
	 * @return the {@link CustomRepositoryImplementationDetector} to scan for the custom implementation
	 */
	protected CustomRepositoryImplementationDetector getCustomImplementationDetector() {
		return context.getCustomRepositoryImplementationDetector();
	}

	/**
	 * @return the {@link CdiRepositoryContext} encapsulating the CDI-specific class loaders and fragment scanning.
	 * @since 2.1
	 */
	protected CdiRepositoryContext getRepositoryContext() {
		return context;
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

}
