/*
 * Copyright 2012-present the original author or authors.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.TypeFilterUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.ConfigurationUtils;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Annotation-based {@link RepositoryConfigurationSource}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Peter Rietzler
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @author Florian Cramer
 * @author Christoph Strobl
 */
public class AnnotationRepositoryConfigurationSource extends RepositoryConfigurationSourceSupport {

	private static final String REPOSITORY_IMPLEMENTATION_POSTFIX = "repositoryImplementationPostfix";
	private static final String BASE_PACKAGES = "basePackages";
	private static final String BASE_PACKAGE_CLASSES = "basePackageClasses";
	private static final String NAMED_QUERIES_LOCATION = "namedQueriesLocation";
	private static final String QUERY_LOOKUP_STRATEGY = "queryLookupStrategy";
	private static final String REPOSITORY_FACTORY_BEAN_CLASS = "repositoryFactoryBeanClass";
	private static final String REPOSITORY_BASE_CLASS = "repositoryBaseClass";
	private static final String REPOSITORY_FRAGMENTS_CONTRIBUTOR_CLASS = "fragmentsContributor";
	private static final String CONSIDER_NESTED_REPOSITORIES = "considerNestedRepositories";
	private static final String BOOTSTRAP_MODE = "bootstrapMode";
	private static final String BEAN_NAME_GENERATOR = "nameGenerator";
	private static final String INCLUDE_FILTERS = "includeFilters";
	private static final String EXCLUDE_FILTERS = "excludeFilters";

	private final AnnotationMetadata configMetadata;
	private final AnnotationMetadata enableAnnotationMetadata;
	private final AnnotationAttributes attributes;
	private final Function<AnnotationAttributes, Stream<TypeFilter>> typeFilterFunction;
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
	 * @param importBeanNameGenerator can be {@literal null}.
	 */
	public AnnotationRepositoryConfigurationSource(AnnotationMetadata metadata, Class<? extends Annotation> annotation,
			ResourceLoader resourceLoader, Environment environment, BeanDefinitionRegistry registry,
			@Nullable BeanNameGenerator importBeanNameGenerator) {

		super(environment, ConfigurationUtils.getRequiredClassLoader(resourceLoader), registry,
				configuredOrDefaultBeanNameGenerator(metadata, annotation,
						ConfigurationUtils.getRequiredClassLoader(resourceLoader), importBeanNameGenerator));

		Assert.notNull(metadata, "Metadata must not be null");
		Assert.notNull(annotation, "Annotation must not be null");
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");

		Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(annotation.getName());

		if (annotationAttributes == null) {
			throw new IllegalStateException(String.format("Unable to obtain annotation attributes for %s", annotation));
		}

		this.attributes = new AnnotationAttributes(annotationAttributes);
		this.enableAnnotationMetadata = AnnotationMetadata.introspect(annotation);
		this.configMetadata = metadata;
		this.typeFilterFunction = it -> TypeFilterUtils.createTypeFiltersFor(it, environment, resourceLoader, registry)
				.stream();
		this.hasExplicitFilters = hasExplicitFilters(attributes);
	}

	@Override
	public Streamable<String> getBasePackages() {

		String[] value = attributes.getStringArray("value");
		String[] basePackages = attributes.getStringArray(BASE_PACKAGES);
		Class<?>[] basePackageClasses = attributes.getClassArray(BASE_PACKAGE_CLASSES);

		// Default configuration - return package of annotated class
		if (value.length == 0 && basePackages.length == 0 && basePackageClasses.length == 0) {

			String className = configMetadata.getClassName();
			return Streamable.of(ClassUtils.getPackageName(className));
		}

		Set<String> packages = new HashSet<>(value.length + basePackages.length + basePackageClasses.length);
		packages.addAll(Arrays.asList(value));
		packages.addAll(Arrays.asList(basePackages));

		for (Class<?> c : basePackageClasses) {
			packages.add(ClassUtils.getPackageName(c));
		}

		return Streamable.of(packages);
	}

	@Override
	public Optional<Object> getQueryLookupStrategyKey() {
		return Optional.ofNullable(attributes.get(QUERY_LOOKUP_STRATEGY));
	}

	@Override
	public Optional<String> getNamedQueryLocation() {
		return getNullDefaultedAttribute(NAMED_QUERIES_LOCATION);
	}

	@Override
	public Optional<String> getRepositoryImplementationPostfix() {
		return getNullDefaultedAttribute(REPOSITORY_IMPLEMENTATION_POSTFIX);
	}

	@Override
	@NonNull
	public Object getSource() {
		return configMetadata;
	}

	@Override
	protected Iterable<TypeFilter> getIncludeFilters() {
		return parseFilters(INCLUDE_FILTERS);
	}

	@Override
	public Streamable<TypeFilter> getExcludeFilters() {
		return parseFilters(EXCLUDE_FILTERS);
	}

	@Override
	public Optional<String> getRepositoryFactoryBeanClassName() {
		return Optional.of(attributes.getClass(REPOSITORY_FACTORY_BEAN_CLASS).getName());
	}

	@Override
	public Optional<String> getRepositoryBaseClassName() {

		if (!attributes.containsKey(REPOSITORY_BASE_CLASS)) {
			return Optional.empty();
		}

		Class<?> repositoryBaseClass = attributes.getClass(REPOSITORY_BASE_CLASS);
		return DefaultRepositoryBaseClass.class.equals(repositoryBaseClass) ? Optional.empty()
				: Optional.of(repositoryBaseClass.getName());
	}

	@Override
	public Optional<String> getRepositoryFragmentsContributorClassName() {

		if (!attributes.containsKey(REPOSITORY_FRAGMENTS_CONTRIBUTOR_CLASS)) {
			return Optional.empty();
		}

		Class<?> fragmentsContributorClass = attributes.getClass(REPOSITORY_FRAGMENTS_CONTRIBUTOR_CLASS);

		return Optional.of(fragmentsContributorClass) //
				.filter(Predicate.not(Class::isInterface)) // avoid default values that are typically interfaces
				.map(Class::getName);
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

	@Override
	public boolean shouldConsiderNestedRepositories() {
		return attributes.containsKey(CONSIDER_NESTED_REPOSITORIES) && attributes.getBoolean(CONSIDER_NESTED_REPOSITORIES);
	}

	@Override
	public Optional<String> getAttribute(String name) {
		return getAttribute(name, String.class);
	}

	@Override
	public <T> Optional<T> getAttribute(String name, Class<T> type) {

		if (!attributes.containsKey(name)) {
			throw new IllegalArgumentException(String.format("No attribute named %s found", name));
		}

		Object value = attributes.get(name);

		if (value == null) {
			return Optional.empty();
		}

		Assert.isInstanceOf(type, value,
				() -> String.format("Attribute value for %s is of type %s but was expected to be of type %s", name,
						value.getClass(), type));

		Object result = value instanceof String //
				? StringUtils.hasText((String) value) ? value : null //
				: value;

		return Optional.ofNullable(type.cast(result));
	}

	@Override
	public boolean usesExplicitFilters() {
		return hasExplicitFilters;
	}

	@Override
	public BootstrapMode getBootstrapMode() {

		try {
			return attributes.getEnum(BOOTSTRAP_MODE);
		} catch (IllegalArgumentException o_O) {
			return BootstrapMode.DEFAULT;
		}
	}

	@Override
	public String getResourceDescription() {

		String simpleClassName = ClassUtils.getShortName(configMetadata.getClassName());
		String annotationClassName = ClassUtils.getShortName(enableAnnotationMetadata.getClassName());

		return String.format("@%s declared on %s", annotationClassName, simpleClassName);
	}

	private Streamable<TypeFilter> parseFilters(String attributeName) {

		AnnotationAttributes[] filters = attributes.getAnnotationArray(attributeName);
		return Streamable.of(() -> Arrays.stream(filters).flatMap(typeFilterFunction));
	}

	/**
	 * Returns the {@link String} attribute with the given name and defaults it to {@literal Optional#empty()} in case
	 * it's empty.
	 *
	 * @param attributeName must not be {@literal null}.
	 * @return
	 */
	private Optional<String> getNullDefaultedAttribute(String attributeName) {

		String attribute = attributes.getString(attributeName);

		return StringUtils.hasText(attribute) ? Optional.of(attribute) : Optional.empty();
	}

	/**
	 * Returns whether there's explicit configuration of include- or exclude filters.
	 *
	 * @param attributes must not be {@literal null}.
	 * @return
	 */
	private static boolean hasExplicitFilters(AnnotationAttributes attributes) {

		return Stream.of(INCLUDE_FILTERS, EXCLUDE_FILTERS) //
				.anyMatch(it -> attributes.getAnnotationArray(it).length > 0);
	}

	private static BeanNameGenerator configuredOrDefaultBeanNameGenerator(AnnotationMetadata metadata,
			Class<? extends Annotation> annotation, ClassLoader beanClassLoader,
			@Nullable BeanNameGenerator importBeanNameGenerator) {

		BeanNameGenerator beanNameGenerator = getConfiguredBeanNameGenerator(metadata, annotation, beanClassLoader);

		if (beanNameGenerator != null) {
			return beanNameGenerator;
		}

		return defaultBeanNameGenerator(importBeanNameGenerator);
	}

	/**
	 * Returns the {@link BeanNameGenerator} to use falling back to an {@link AnnotationBeanNameGenerator} if either the
	 * given generator is {@literal null} or it's the one locally declared in {@link ConfigurationClassPostProcessor}'s
	 * {@code importBeanNameGenerator}. This is to make sure we only use the given {@link BeanNameGenerator} if it was
	 * customized.
	 *
	 * @param generator can be {@literal null}.
	 * @return the configured {@link BeanNameGenerator} if it is not
	 *         {@link ConfigurationClassPostProcessor#IMPORT_BEAN_NAME_GENERATOR} or {@link AnnotationBeanNameGenerator}
	 *         otherwise.
	 * @since 2.2
	 */
	private static BeanNameGenerator defaultBeanNameGenerator(@Nullable BeanNameGenerator generator) {

		return generator == null || ConfigurationClassPostProcessor.IMPORT_BEAN_NAME_GENERATOR.equals(generator) //
				? AnnotationBeanNameGenerator.INSTANCE //
				: generator;
	}

	/**
	 * Obtain a configured {@link BeanNameGenerator} if present.
	 *
	 * @param beanClassLoader a class loader to load the configured {@link BeanNameGenerator} class in case it was
	 *          configured as String instead of a Class instance.
	 * @return the bean name generator or {@literal null} if not configured.
	 */
	@SuppressWarnings("unchecked")
	private static @Nullable BeanNameGenerator getConfiguredBeanNameGenerator(AnnotationMetadata metadata,
		Class<? extends Annotation> annotation, ClassLoader beanClassLoader) {

		Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(annotation.getName());
		if(annotationAttributes == null || !annotationAttributes.containsKey(BEAN_NAME_GENERATOR)) {
			return null;
		}

		Object configuredBeanNameGenerator = annotationAttributes.get(BEAN_NAME_GENERATOR);
		if (configuredBeanNameGenerator == null) {
			return null;
		}

		if (configuredBeanNameGenerator instanceof String beanNameGeneratorTypeName) {
			try {
				configuredBeanNameGenerator = ClassUtils.forName(beanNameGeneratorTypeName, beanClassLoader);
			} catch (Exception o_O) {
				throw new RuntimeException(o_O);
			}
		}

		if (configuredBeanNameGenerator != BeanNameGenerator.class) {
			return BeanUtils.instantiateClass((Class<? extends BeanNameGenerator>) configuredBeanNameGenerator);
		}

		return null;
	}
}
