/*
 * Copyright 2012-2015 the original author or authors.
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Annotation based {@link RepositoryConfigurationSource}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class AnnotationRepositoryConfigurationSource extends RepositoryConfigurationSourceSupport {

	private static final String REPOSITORY_IMPLEMENTATION_POSTFIX = "repositoryImplementationPostfix";
	private static final String BASE_PACKAGES = "basePackages";
	private static final String BASE_PACKAGE_CLASSES = "basePackageClasses";
	private static final String NAMED_QUERIES_LOCATION = "namedQueriesLocation";
	private static final String QUERY_LOOKUP_STRATEGY = "queryLookupStrategy";
	private static final String REPOSITORY_FACTORY_BEAN_CLASS = "repositoryFactoryBeanClass";
	private static final String REPOSITORY_BASE_CLASS = "repositoryBaseClass";
	private static final String CONSIDER_NESTED_REPOSITORIES = "considerNestedRepositories";

	private final AnnotationMetadata configMetadata;
	private final AnnotationMetadata enableAnnotationMetadata;
	private final AnnotationAttributes attributes;
	private final ResourceLoader resourceLoader;
	private final boolean hasExplicitFilters;

	/**
	 * Creates a new {@link AnnotationRepositoryConfigurationSource} from the given {@link AnnotationMetadata} and
	 * annotation.
	 * 
	 * @param configMetadata must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @param environment
	 */
	public AnnotationRepositoryConfigurationSource(AnnotationMetadata metadata, Class<? extends Annotation> annotation,
			ResourceLoader resourceLoader, Environment environment) {

		super(environment);

		Assert.notNull(metadata);
		Assert.notNull(annotation);
		Assert.notNull(resourceLoader);

		this.attributes = new AnnotationAttributes(metadata.getAnnotationAttributes(annotation.getName()));
		this.enableAnnotationMetadata = new StandardAnnotationMetadata(annotation);
		this.configMetadata = metadata;
		this.resourceLoader = resourceLoader;
		this.hasExplicitFilters = hasExplicitFilters(attributes);
	}

	/**
	 * Returns whether there's explicit configuration of include- or exclude filters.
	 * 
	 * @param attributes must not be {@literal null}.
	 * @return
	 */
	private static boolean hasExplicitFilters(AnnotationAttributes attributes) {

		for (String attribute : Arrays.asList("includeFilters", "excludeFilters")) {

			if (attributes.getAnnotationArray(attribute).length > 0) {
				return true;
			}
		}

		return false;
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
			String className = configMetadata.getClassName();
			return Collections.singleton(ClassUtils.getPackageName(className));
		}

		Set<String> packages = new HashSet<>();
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
	public Optional<Object> getQueryLookupStrategyKey() {
		return Optional.ofNullable(attributes.get(QUERY_LOOKUP_STRATEGY));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getNamedQueryLocation()
	 */
	public Optional<String> getNamedQueryLocation() {
		return getNullDefaultedAttribute(NAMED_QUERIES_LOCATION);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryImplementationPostfix()
	 */
	public Optional<String> getRepositoryImplementationPostfix() {
		return getNullDefaultedAttribute(REPOSITORY_IMPLEMENTATION_POSTFIX);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getSource()
	 */
	public Object getSource() {
		return configMetadata;
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

		Set<TypeFilter> result = new HashSet<>();
		AnnotationAttributes[] filters = attributes.getAnnotationArray(attributeName);

		for (AnnotationAttributes filter : filters) {
			result.addAll(typeFiltersFor(filter));
		}

		return result;
	}

	/**
	 * Returns the {@link String} attribute with the given name and defaults it to {@literal Optional#empty()} in case
	 * it's empty.
	 * 
	 * @param attributeName
	 * @return
	 */
	private Optional<String> getNullDefaultedAttribute(String attributeName) {
		String attribute = attributes.getString(attributeName);
		return StringUtils.hasText(attribute) ? Optional.of(attribute) : Optional.empty();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryFactoryBeanName()
	 */
	public String getRepositoryFactoryBeanName() {
		return attributes.getClass(REPOSITORY_FACTORY_BEAN_CLASS).getName();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryBaseClassName()
	 */
	@Override
	public Optional<String> getRepositoryBaseClassName() {

		if (!attributes.containsKey(REPOSITORY_BASE_CLASS)) {
			return Optional.empty();
		}

		Class<? extends Object> repositoryBaseClass = attributes.getClass(REPOSITORY_BASE_CLASS);
		return DefaultRepositoryBaseClass.class.equals(repositoryBaseClass) ? Optional.empty()
				: Optional.of(repositoryBaseClass.getName());
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
	 * Returns the {@link AnnotationMetadata} for the {@code @Enable} annotation that triggered the configuration.
	 * 
	 * @return the enableAnnotationMetadata
	 */
	public AnnotationMetadata getEnableAnnotationMetadata() {
		return enableAnnotationMetadata;
	}

	/**
	 * Copy of {@code ComponentScanAnnotationParser#typeFiltersFor}.
	 * 
	 * @param filterAttributes
	 * @return
	 */
	private List<TypeFilter> typeFiltersFor(AnnotationAttributes filterAttributes) {

		List<TypeFilter> typeFilters = new ArrayList<>();
		FilterType filterType = filterAttributes.getEnum("type");

		for (Class<?> filterClass : filterAttributes.getClassArray("value")) {
			switch (filterType) {
				case ANNOTATION:
					Assert.isAssignable(Annotation.class, filterClass,
							"An error occured when processing a @ComponentScan " + "ANNOTATION type filter: ");
					@SuppressWarnings("unchecked")
					Class<Annotation> annoClass = (Class<Annotation>) filterClass;
					typeFilters.add(new AnnotationTypeFilter(annoClass));
					break;
				case ASSIGNABLE_TYPE:
					typeFilters.add(new AssignableTypeFilter(filterClass));
					break;
				case CUSTOM:
					Assert.isAssignable(TypeFilter.class, filterClass,
							"An error occured when processing a @ComponentScan " + "CUSTOM type filter: ");
					typeFilters.add(BeanUtils.instantiateClass(filterClass, TypeFilter.class));
					break;
				default:
					throw new IllegalArgumentException("Unknown filter type " + filterType);
			}
		}

		for (String expression : getPatterns(filterAttributes)) {

			String rawName = filterType.toString();

			if ("REGEX".equals(rawName)) {
				typeFilters.add(new RegexPatternTypeFilter(Pattern.compile(expression)));
			} else if ("ASPECTJ".equals(rawName)) {
				typeFilters.add(new AspectJTypeFilter(expression, this.resourceLoader.getClassLoader()));
			} else {
				throw new IllegalArgumentException("Unknown filter type " + filterType);
			}
		}

		return typeFilters;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSourceSupport#shouldConsiderNestedRepositories()
	 */
	@Override
	public boolean shouldConsiderNestedRepositories() {
		return attributes.containsKey(CONSIDER_NESTED_REPOSITORIES) && attributes.getBoolean(CONSIDER_NESTED_REPOSITORIES);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getAttribute(java.lang.String)
	 */
	@Override
	public Optional<String> getAttribute(String name) {

		String attribute = attributes.getString(name);
		return StringUtils.hasText(attribute) ? Optional.of(attribute) : Optional.empty();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#usesExplicitFilters()
	 */
	@Override
	public boolean usesExplicitFilters() {
		return hasExplicitFilters;
	}

	/**
	 * Safely reads the {@code pattern} attribute from the given {@link AnnotationAttributes} and returns an empty list if
	 * the attribute is not present.
	 * 
	 * @param filterAttributes must not be {@literal null}.
	 * @return
	 */
	private String[] getPatterns(AnnotationAttributes filterAttributes) {

		try {
			return filterAttributes.getStringArray("pattern");
		} catch (IllegalArgumentException o_O) {
			return new String[0];
		}
	}
}
