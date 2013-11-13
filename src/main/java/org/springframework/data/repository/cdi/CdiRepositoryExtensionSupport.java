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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.RepositoryDefinition;

/**
 * Base class for {@link Extension} implementations that create instances for Spring Data repositories.
 * 
 * @author Dirk Mahler
 * @author Oliver Gierke
 */
public abstract class CdiRepositoryExtensionSupport implements Extension {

	private static final Logger LOGGER = LoggerFactory.getLogger(CdiRepositoryExtensionSupport.class);

	private final Map<Class<?>, Set<Annotation>> repositoryTypes = new HashMap<Class<?>, Set<Annotation>>();

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
	 * Provides access to all repository types as well as their qualifiers.
	 * 
	 * @return
	 */
	protected Iterable<Entry<Class<?>, Set<Annotation>>> getRepositoryTypes() {
		return repositoryTypes.entrySet();
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
