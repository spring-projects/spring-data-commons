/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.repository.config;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Annotation based {@link RepositoryConfigurationSource}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationRepositoryConfigurationSource extends RepositoryConfigurationSourceSupport {

	private static final String REPOSITORY_IMPLEMENTATION_POSTFIX = "repositoryImplementationPostfix";
	private static final String BASE_PACKAGES = "basePackages";
	private static final String BASE_PACKAGE_CLASSES = "basePackageClasses";
	private static final String NAMED_QUERIES_LOCATION = "namedQueriesLocation";
	private static final String QUERY_LOOKUP_STRATEGY = "queryLookupStrategy";
	private static final String REPOSITORY_FACTORY_BEAN_CLASS = "repositoryFactoryBeanClass";

	private final AnnotationMetadata metadata;
	private final AnnotationAttributes attributes;

	/**
	 * Creates a new {@link AnnotationRepositoryConfigurationSource} from the given {@link AnnotationMetadata} and
	 * annotation.
	 * 
	 * @param metadata must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 */
	public AnnotationRepositoryConfigurationSource(AnnotationMetadata metadata, Class<? extends Annotation> annotation) {

		Assert.notNull(metadata);
		Assert.notNull(annotation);

		this.attributes = new AnnotationAttributes(metadata.getAnnotationAttributes(annotation.getName()));
		this.metadata = metadata;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getBasePackages()
	 */
	public Iterable<String> getBasePackages() {

		String[] value = attributes.getStringArray("value");
		String[] basePackages = attributes.getStringArray(BASE_PACKAGES);
		Class<?>[] basePackageClasses = attributes.getClassArray(BASE_PACKAGE_CLASSES);

		// Default configuration - return package of annotated class
		if (value.length == 0 && basePackages.length == 0 && basePackageClasses.length == 0) {
			String className = metadata.getClassName();
			return Collections.singleton(className.substring(0, className.lastIndexOf('.')));
		}

		Set<String> packages = new HashSet<String>();
		packages.addAll(Arrays.asList(value));
		packages.addAll(Arrays.asList(basePackages));

		for (Class<?> typeName : basePackageClasses) {
			packages.add(ClassUtils.getPackageName(typeName));
		}

		return packages;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getQueryLookupStrategyKey()
	 */
	public Object getQueryLookupStrategyKey() {
		return attributes.get(QUERY_LOOKUP_STRATEGY);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getNamedQueryLocation()
	 */
	public String getNamedQueryLocation() {
		return getNullDefaultedAttribute(NAMED_QUERIES_LOCATION);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryImplementationPostfix()
	 */
	public String getRepositoryImplementationPostfix() {
		return getNullDefaultedAttribute(REPOSITORY_IMPLEMENTATION_POSTFIX);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getSource()
	 */
	public Object getSource() {
		return metadata;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSourceSupport#getIncludeFilters()
	 */
	@Override
	protected Iterable<TypeFilter> getIncludeFilters() {
		return parseFilters("includeFilters");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSourceSupport#getExcludeFilters()
	 */
	@Override
	protected Iterable<TypeFilter> getExcludeFilters() {
		return parseFilters("excludeFilters");
	}

	private Set<TypeFilter> parseFilters(String attributeName) {

		Set<TypeFilter> result = new HashSet<TypeFilter>();
		AnnotationAttributes[] filters = attributes.getAnnotationArray(attributeName);

		for (AnnotationAttributes filter : filters) {
			result.addAll(typeFiltersFor(filter));
		}

		return result;
	}

	/**
	 * Returns the {@link String} attribute with the given name and defaults it to {@literal null} in case it's empty.
	 * 
	 * @param attributeName
	 * @return
	 */
	private String getNullDefaultedAttribute(String attributeName) {
		String attribute = attributes.getString(attributeName);
		return StringUtils.hasText(attribute) ? attribute : null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryFactoryBeanName()
	 */
	public String getRepositoryFactoryBeanName() {
		return attributes.getClass(REPOSITORY_FACTORY_BEAN_CLASS).getName();
	}

	/**
	 * Returns the {@link AnnotationAttributes} of the annotation configured.
	 * 
	 * @return the attributes will never be {@literal null}.
	 */
	public AnnotationAttributes getAttributes() {
		return attributes;
	}

	/**
	 * Copy of {@code ComponentScanAnnotationParser#typeFiltersFor}.
	 * 
	 * @param filterAttributes
	 * @return
	 */
	private List<TypeFilter> typeFiltersFor(AnnotationAttributes filterAttributes) {
		List<TypeFilter> typeFilters = new ArrayList<TypeFilter>();
		FilterType filterType = filterAttributes.getEnum("type");

		for (Class<?> filterClass : filterAttributes.getClassArray("value")) {
			switch (filterType) {
			case ANNOTATION:
				Assert.isAssignable(Annotation.class, filterClass, "An error occured when processing a @ComponentScan "
						+ "ANNOTATION type filter: ");
				@SuppressWarnings("unchecked")
				Class<Annotation> annoClass = (Class<Annotation>) filterClass;
				typeFilters.add(new AnnotationTypeFilter(annoClass));
				break;
			case ASSIGNABLE_TYPE:
				typeFilters.add(new AssignableTypeFilter(filterClass));
				break;
			case CUSTOM:
				Assert.isAssignable(TypeFilter.class, filterClass, "An error occured when processing a @ComponentScan "
						+ "CUSTOM type filter: ");
				typeFilters.add(BeanUtils.instantiateClass(filterClass, TypeFilter.class));
				break;
			default:
				throw new IllegalArgumentException("unknown filter type " + filterType);
			}
		}
		return typeFilters;
	}
}
