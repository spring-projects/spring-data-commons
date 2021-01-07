/*
 * Copyright 2012-2021 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Annotation based {@link RepositoryConfigurationSource}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Peter Rietzler
 * @author Jens Schauder
 * @author Mark Paluch
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
	private static final String BOOTSTRAP_MODE = "bootstrapMode";

	private final AnnotationMetadata configMetadata;
	private final AnnotationMetadata enableAnnotationMetadata;
	private final AnnotationAttributes attributes;
	private final ResourceLoader resourceLoader;
	private final boolean hasExplicitFilters;

	/**
	 * Creates a new {@link AnnotationRepositoryConfigurationSource} from the given {@link AnnotationMetadata} and
	 * annotation.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 * @deprecated since 2.2. Prefer to use overload taking a {@link BeanNameGenerator} additionally.
	 */
	@Deprecated
	public AnnotationRepositoryConfigurationSource(AnnotationMetadata metadata, Class<? extends Annotation> annotation,
			ResourceLoader resourceLoader, Environment environment, BeanDefinitionRegistry registry) {
		this(metadata, annotation, resourceLoader, environment, registry, null);
	}

	/**
	 * Creates a new {@link AnnotationRepositoryConfigurationSource} from the given {@link AnnotationMetadata} and
	 * annotation.
	 *
	 * @param metadata must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 * @param resourceLoader must not be {@literal null}.
	 * @param environment must not be {@literal null}.
	 * @param registry must not be {@literal null}.
	 * @param generator can be {@literal null}.
	 */
	public AnnotationRepositoryConfigurationSource(AnnotationMetadata metadata, Class<? extends Annotation> annotation,
			ResourceLoader resourceLoader, Environment environment, BeanDefinitionRegistry registry,
			@Nullable BeanNameGenerator generator) {

		super(environment, ConfigurationUtils.getRequiredClassLoader(resourceLoader), registry,
				defaultBeanNameGenerator(generator));

		Assert.notNull(metadata, "Metadata must not be null!");
		Assert.notNull(annotation, "Annotation must not be null!");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null!");

		Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(annotation.getName());

		if (annotationAttributes == null) {
			throw new IllegalStateException(String.format("Unable to obtain annotation attributes for %s!", annotation));
		}

		this.attributes = new AnnotationAttributes(annotationAttributes);
		this.enableAnnotationMetadata = AnnotationMetadata.introspect(annotation);
		this.configMetadata = metadata;
		this.resourceLoader = resourceLoader;
		this.hasExplicitFilters = hasExplicitFilters(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getBasePackages()
	 */
	public Streamable<String> getBasePackages() {

		String[] value = attributes.getStringArray("value");
		String[] basePackages = attributes.getStringArray(BASE_PACKAGES);
		Class<?>[] basePackageClasses = attributes.getClassArray(BASE_PACKAGE_CLASSES);

		// Default configuration - return package of annotated class
		if (value.length == 0 && basePackages.length == 0 && basePackageClasses.length == 0) {

			String className = configMetadata.getClassName();
			return Streamable.of(ClassUtils.getPackageName(className));
		}

		Set<String> packages = new HashSet<>();
		packages.addAll(Arrays.asList(value));
		packages.addAll(Arrays.asList(basePackages));

		Arrays.stream(basePackageClasses)//
				.map(ClassUtils::getPackageName)//
				.forEach(it -> packages.add(it));

		return Streamable.of(packages);
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
	@Nonnull
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
	public Streamable<TypeFilter> getExcludeFilters() {
		return parseFilters("excludeFilters");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getRepositoryFactoryBeanClassName()
	 */
	@Override
	public Optional<String> getRepositoryFactoryBeanClassName() {
		return Optional.of(attributes.getClass(REPOSITORY_FACTORY_BEAN_CLASS).getName());
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
		return getAttribute(name, String.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getAttribute(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> Optional<T> getAttribute(String name, Class<T> type) {

		if (!attributes.containsKey(name)) {
			throw new IllegalArgumentException(String.format("No attribute named %s found!", name));
		}

		Object value = attributes.get(name);

		if (value == null) {
			return Optional.empty();
		}

		Assert.isInstanceOf(type, value,
				() -> String.format("Attribute value for %s is of type %s but was expected to be of type %s!", name,
						value.getClass(), type));

		Object result = String.class.isInstance(value) //
				? StringUtils.hasText((String) value) ? value : null //
				: value;

		return Optional.ofNullable(type.cast(result));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#usesExplicitFilters()
	 */
	@Override
	public boolean usesExplicitFilters() {
		return hasExplicitFilters;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getBootstrapMode()
	 */
	@Override
	public BootstrapMode getBootstrapMode() {

		try {
			return attributes.getEnum(BOOTSTRAP_MODE);
		} catch (IllegalArgumentException o_O) {
			return BootstrapMode.DEFAULT;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.config.RepositoryConfigurationSource#getResourceDescription()
	 */
	@Override
	public String getResourceDescription() {

		String simpleClassName = ClassUtils.getShortName(configMetadata.getClassName());
		String annoationClassName = ClassUtils.getShortName(enableAnnotationMetadata.getClassName());

		return String.format("@%s declared on %s", annoationClassName, simpleClassName);
	}

	private Streamable<TypeFilter> parseFilters(String attributeName) {

		AnnotationAttributes[] filters = attributes.getAnnotationArray(attributeName);

		return Streamable.of(() -> Arrays.stream(filters).flatMap(it -> typeFiltersFor(it).stream()));
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

	/**
	 * Returns whether there's explicit configuration of include- or exclude filters.
	 *
	 * @param attributes must not be {@literal null}.
	 * @return
	 */
	private static boolean hasExplicitFilters(AnnotationAttributes attributes) {

		return Stream.of("includeFilters", "excludeFilters") //
				.anyMatch(it -> attributes.getAnnotationArray(it).length > 0);
	}

	/**
	 * Returns the {@link BeanNameGenerator} to use falling back to an {@link AnnotationBeanNameGenerator} if either the
	 * given generator is {@literal null} or it's the one locally declared in {@link ConfigurationClassPostProcessor}'s
	 * {@code importBeanNameGenerator}. This is to make sure we only use the given {@link BeanNameGenerator} if it was
	 * customized.
	 *
	 * @param generator can be {@literal null}.
	 * @return
	 * @since 2.2
	 */
	private static BeanNameGenerator defaultBeanNameGenerator(@Nullable BeanNameGenerator generator) {

		return generator == null || ConfigurationClassPostProcessor.IMPORT_BEAN_NAME_GENERATOR.equals(generator) //
				? new AnnotationBeanNameGenerator() //
				: generator;
	}
}
