/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.domain;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * Specification for property path matching to use in query by example (QBE). An {@link ExampleMatcher} can be created
 * for a {@link Class object type}. Instances of {@link ExampleMatcher} can be either {@link #matching()} or
 * {@link #typed(Class)} and settings can be tuned {@code with...} methods in a fluent style. {@code with...} methods
 * return a copy of the {@link ExampleMatcher} instance with the specified setting. Null-handling defaults to
 * {@link NullHandler#IGNORE} and case-sensitive {@link StringMatcher#DEFAULT} string matching.
 * <p>
 * This class is immutable.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oliver Gierke
 * @param <T>
 * @since 1.12
 */
@ToString
@EqualsAndHashCode
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExampleMatcher {

	NullHandler nullHandler;
	StringMatcher defaultStringMatcher;
	PropertySpecifiers propertySpecifiers;
	Set<String> ignoredPaths;
	boolean defaultIgnoreCase;
	@Wither(AccessLevel.PRIVATE) MatchMode mode;

	private ExampleMatcher() {
		this(NullHandler.IGNORE, StringMatcher.DEFAULT, new PropertySpecifiers(), Collections.<String>emptySet(), false,
				MatchMode.ALL);
	}

	/**
	 * Create a new {@link ExampleMatcher} including all non-null properties by default exposing that all resulting
	 * predicates are supposed to be AND-concatenated.
	 *
	 * @param type will never be {@literal null}.
	 * @return
	 * @see #matchingAll()
	 */
	public static ExampleMatcher matching() {
		return matchingAll();
	}

	/**
	 * Create a new {@link ExampleMatcher} including all non-null properties by default matching any predicate derived
	 * from the example.
	 *
	 * @param type will never be {@literal null}.
	 * @return
	 */
	public static ExampleMatcher matchingAny() {
		return new ExampleMatcher().withMode(MatchMode.ANY);
	}

	/**
	 * Create a new {@link ExampleMatcher} including all non-null properties by default matching all predicates derived
	 * from the example.
	 *
	 * @param type will never be {@literal null}.
	 * @return
	 */
	public static ExampleMatcher matchingAll() {
		return new ExampleMatcher().withMode(MatchMode.ALL);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code propertyPaths}. This instance is immutable
	 * and unaffected by this method call.
	 *
	 * @param ignoredPaths must not be {@literal null} and not empty.
	 * @return
	 */
	public ExampleMatcher withIgnorePaths(String... ignoredPaths) {

		Assert.notEmpty(ignoredPaths, "IgnoredPaths must not be empty!");
		Assert.noNullElements(ignoredPaths, "IgnoredPaths must not contain null elements!");

		Set<String> newIgnoredPaths = new LinkedHashSet<>(this.ignoredPaths);
		newIgnoredPaths.addAll(Arrays.asList(ignoredPaths));

		return new ExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, newIgnoredPaths, defaultIgnoreCase,
				mode);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified string matching of {@code defaultStringMatcher}.
	 * This instance is immutable and unaffected by this method call.
	 *
	 * @param defaultStringMatcher must not be {@literal null}.
	 * @return
	 */
	public ExampleMatcher withStringMatcher(StringMatcher defaultStringMatcher) {

		Assert.notNull(ignoredPaths, "DefaultStringMatcher must not be empty!");

		return new ExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths, defaultIgnoreCase,
				mode);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with ignoring case sensitivity by default. This instance is immutable
	 * and unaffected by this method call.
	 *
	 * @return
	 */
	public ExampleMatcher withIgnoreCase() {
		return withIgnoreCase(true);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with {@code defaultIgnoreCase}. This instance is immutable and
	 * unaffected by this method call.
	 *
	 * @param defaultIgnoreCase
	 * @return
	 */
	public ExampleMatcher withIgnoreCase(boolean defaultIgnoreCase) {
		return new ExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths, defaultIgnoreCase,
				mode);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code GenericPropertyMatcher} for the
	 * {@code propertyPath}. This instance is immutable and unaffected by this method call.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param matcherConfigurer callback to configure a {@link GenericPropertyMatcher}, must not be {@literal null}.
	 * @return
	 */
	public ExampleMatcher withMatcher(String propertyPath, MatcherConfigurer<GenericPropertyMatcher> matcherConfigurer) {

		Assert.hasText(propertyPath, "PropertyPath must not be empty!");
		Assert.notNull(matcherConfigurer, "MatcherConfigurer must not be empty!");

		GenericPropertyMatcher genericPropertyMatcher = new GenericPropertyMatcher();
		matcherConfigurer.configureMatcher(genericPropertyMatcher);

		return withMatcher(propertyPath, genericPropertyMatcher);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code GenericPropertyMatcher} for the
	 * {@code propertyPath}. This instance is immutable and unaffected by this method call.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param genericPropertyMatcher callback to configure a {@link GenericPropertyMatcher}, must not be {@literal null}.
	 * @return
	 */
	public ExampleMatcher withMatcher(String propertyPath, GenericPropertyMatcher genericPropertyMatcher) {

		Assert.hasText(propertyPath, "PropertyPath must not be empty!");
		Assert.notNull(genericPropertyMatcher, "GenericPropertyMatcher must not be empty!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);
		PropertySpecifier propertySpecifier = new PropertySpecifier(propertyPath);

		if (genericPropertyMatcher.ignoreCase != null) {
			propertySpecifier = propertySpecifier.withIgnoreCase(genericPropertyMatcher.ignoreCase);
		}

		if (genericPropertyMatcher.stringMatcher != null) {
			propertySpecifier = propertySpecifier.withStringMatcher(genericPropertyMatcher.stringMatcher);
		}

		if (genericPropertyMatcher.valueTransformer != null) {
			propertySpecifier = propertySpecifier.withValueTransformer(genericPropertyMatcher.valueTransformer);
		}

		propertySpecifiers.add(propertySpecifier);

		return new ExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths, defaultIgnoreCase,
				mode);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code PropertyValueTransformer} for the
	 * {@code propertyPath}.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param propertyValueTransformer must not be {@literal null}.
	 * @return
	 */
	public ExampleMatcher withTransformer(String propertyPath, PropertyValueTransformer propertyValueTransformer) {

		Assert.hasText(propertyPath, "PropertyPath must not be empty!");
		Assert.notNull(propertyValueTransformer, "PropertyValueTransformer must not be empty!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);
		PropertySpecifier propertySpecifier = getOrCreatePropertySpecifier(propertyPath, propertySpecifiers);

		propertySpecifiers.add(propertySpecifier.withValueTransformer(propertyValueTransformer));

		return new ExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths, defaultIgnoreCase,
				mode);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with ignore case sensitivity for the {@code propertyPaths}. This
	 * instance is immutable and unaffected by this method call.
	 *
	 * @param propertyPaths must not be {@literal null} and not empty.
	 * @return
	 */
	public ExampleMatcher withIgnoreCase(String... propertyPaths) {

		Assert.notEmpty(propertyPaths, "PropertyPaths must not be empty!");
		Assert.noNullElements(propertyPaths, "PropertyPaths must not contain null elements!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);

		for (String propertyPath : propertyPaths) {
			PropertySpecifier propertySpecifier = getOrCreatePropertySpecifier(propertyPath, propertySpecifiers);
			propertySpecifiers.add(propertySpecifier.withIgnoreCase(true));
		}

		return new ExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths, defaultIgnoreCase,
				mode);
	}

	private PropertySpecifier getOrCreatePropertySpecifier(String propertyPath, PropertySpecifiers propertySpecifiers) {

		if (propertySpecifiers.hasSpecifierForPath(propertyPath)) {
			return propertySpecifiers.getForPath(propertyPath);
		}

		return new PropertySpecifier(propertyPath);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with treatment for {@literal null} values of
	 * {@link NullHandler#INCLUDE} . This instance is immutable and unaffected by this method call.
	 *
	 * @return
	 */
	public ExampleMatcher withIncludeNullValues() {
		return withNullHandler(NullHandler.INCLUDE);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with treatment for {@literal null} values of
	 * {@link NullHandler#IGNORE}. This instance is immutable and unaffected by this method call.
	 *
	 * @return
	 */
	public ExampleMatcher withIgnoreNullValues() {
		return withNullHandler(NullHandler.IGNORE);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code nullHandler}. This instance is immutable
	 * and unaffected by this method call.
	 *
	 * @param nullHandler must not be {@literal null}.
	 * @return
	 */
	public ExampleMatcher withNullHandler(NullHandler nullHandler) {

		Assert.notNull(nullHandler, "NullHandler must not be null!");
		return new ExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths, defaultIgnoreCase,
				mode);
	}

	/**
	 * Get defined null handling.
	 *
	 * @return never {@literal null}
	 */
	public ExampleMatcher.NullHandler getNullHandler() {
		return nullHandler;
	}

	/**
	 * Get defined {@link ExampleMatcher.StringMatcher}.
	 *
	 * @return never {@literal null}.
	 */
	public ExampleMatcher.StringMatcher getDefaultStringMatcher() {
		return defaultStringMatcher;
	}

	/**
	 * @return {@literal true} if {@link String} should be matched with ignore case option.
	 */
	public boolean isIgnoreCaseEnabled() {
		return this.defaultIgnoreCase;
	}

	/**
	 * @param path
	 * @return return {@literal true} if path was set to be ignored.
	 */
	public boolean isIgnoredPath(String path) {
		return this.ignoredPaths.contains(path);
	}

	/**
	 * @return unmodifiable {@link Set} of ignored paths.
	 */
	public Set<String> getIgnoredPaths() {
		return ignoredPaths;
	}

	/**
	 * @return the {@link PropertySpecifiers} within the {@link ExampleMatcher}.
	 */
	public PropertySpecifiers getPropertySpecifiers() {
		return propertySpecifiers;
	}

	/**
	 * Returns whether all of the predicates of the {@link Example} are supposed to match. If {@literal false} is
	 * returned, it's sufficient if any of the predicates derived from the {@link Example} match.
	 * 
	 * @return whether all of the predicates of the {@link Example} are supposed to match or any of them is sufficient.
	 */
	public boolean isAllMatching() {
		return mode.equals(MatchMode.ALL);
	}

	/**
	 * Returns whether it's sufficient that any of the predicates of the {@link Example} match. If {@literal false} is
	 * returned, all predicates derived from the example need to match to produce results.
	 * 
	 * @return whether it's sufficient that any of the predicates of the {@link Example} match or all need to match.
	 */
	public boolean isAnyMatching() {
		return mode.equals(MatchMode.ANY);
	}

	/**
	 * Null handling for creating criterion out of an {@link Example}.
	 *
	 * @author Christoph Strobl
	 */
	public static enum NullHandler {

		INCLUDE, IGNORE
	}

	/**
	 * Callback to configure a matcher.
	 *
	 * @author Mark Paluch
	 * @param <T>
	 */
	public static interface MatcherConfigurer<T> {
		void configureMatcher(T matcher);
	}

	/**
	 * A generic property matcher that specifies {@link StringMatcher string matching} and case sensitivity.
	 *
	 * @author Mark Paluch
	 */
	@EqualsAndHashCode
	public static class GenericPropertyMatcher {

		StringMatcher stringMatcher = null;
		Boolean ignoreCase = null;
		PropertyValueTransformer valueTransformer = NoOpPropertyValueTransformer.INSTANCE;

		/**
		 * Creates an unconfigured {@link GenericPropertyMatcher}.
		 */
		public GenericPropertyMatcher() {}

		/**
		 * Creates a new {@link GenericPropertyMatcher} with a {@link StringMatcher} and {@code ignoreCase}.
		 *
		 * @param stringMatcher must not be {@literal null}.
		 * @param ignoreCase
		 * @return
		 */
		public static GenericPropertyMatcher of(StringMatcher stringMatcher, boolean ignoreCase) {
			return new GenericPropertyMatcher().stringMatcher(stringMatcher).ignoreCase(ignoreCase);
		}

		/**
		 * Creates a new {@link GenericPropertyMatcher} with a {@link StringMatcher} and {@code ignoreCase}.
		 *
		 * @param stringMatcher must not be {@literal null}.
		 * @return
		 */
		public static GenericPropertyMatcher of(StringMatcher stringMatcher) {
			return new GenericPropertyMatcher().stringMatcher(stringMatcher);
		}

		/**
		 * Sets ignores case to {@literal true}.
		 *
		 * @return
		 */
		public GenericPropertyMatcher ignoreCase() {

			this.ignoreCase = true;
			return this;
		}

		/**
		 * Sets ignores case to {@code ignoreCase}.
		 *
		 * @param ignoreCase
		 * @return
		 */
		public GenericPropertyMatcher ignoreCase(boolean ignoreCase) {

			this.ignoreCase = ignoreCase;
			return this;
		}

		/**
		 * Sets ignores case to {@literal false}.
		 *
		 * @return
		 */
		public GenericPropertyMatcher caseSensitive() {

			this.ignoreCase = false;
			return this;
		}

		/**
		 * Sets string matcher to {@link StringMatcher#CONTAINING}.
		 *
		 * @return
		 */
		public GenericPropertyMatcher contains() {

			this.stringMatcher = StringMatcher.CONTAINING;
			return this;
		}

		/**
		 * Sets string matcher to {@link StringMatcher#ENDING}.
		 *
		 * @return
		 */
		public GenericPropertyMatcher endsWith() {

			this.stringMatcher = StringMatcher.ENDING;
			return this;
		}

		/**
		 * Sets string matcher to {@link StringMatcher#STARTING}.
		 *
		 * @return
		 */
		public GenericPropertyMatcher startsWith() {

			this.stringMatcher = StringMatcher.STARTING;
			return this;
		}

		/**
		 * Sets string matcher to {@link StringMatcher#EXACT}.
		 *
		 * @return
		 */
		public GenericPropertyMatcher exact() {

			this.stringMatcher = StringMatcher.EXACT;
			return this;
		}

		/**
		 * Sets string matcher to {@link StringMatcher#DEFAULT}.
		 *
		 * @return
		 */
		public GenericPropertyMatcher storeDefaultMatching() {

			this.stringMatcher = StringMatcher.DEFAULT;
			return this;
		}

		/**
		 * Sets string matcher to {@link StringMatcher#REGEX}.
		 *
		 * @return
		 */
		public GenericPropertyMatcher regex() {

			this.stringMatcher = StringMatcher.REGEX;
			return this;
		}

		/**
		 * Sets string matcher to {@code stringMatcher}.
		 *
		 * @param stringMatcher must not be {@literal null}.
		 * @return
		 */
		public GenericPropertyMatcher stringMatcher(StringMatcher stringMatcher) {

			Assert.notNull(stringMatcher, "StringMatcher must not be null!");
			this.stringMatcher = stringMatcher;
			return this;
		}

		/**
		 * Sets the {@link PropertyValueTransformer} to {@code propertyValueTransformer}.
		 *
		 * @param propertyValueTransformer must not be {@literal null}.
		 * @return
		 */
		public GenericPropertyMatcher transform(PropertyValueTransformer propertyValueTransformer) {

			Assert.notNull(propertyValueTransformer, "PropertyValueTransformer must not be null!");
			this.valueTransformer = propertyValueTransformer;
			return this;
		}
	}

	/**
	 * Predefined property matchers to create a {@link GenericPropertyMatcher}.
	 *
	 * @author Mark Paluch
	 */
	public static class GenericPropertyMatchers {

		/**
		 * Creates a {@link GenericPropertyMatcher} that matches string case insensitive.
		 *
		 * @return
		 */
		public static GenericPropertyMatcher ignoreCase() {
			return new GenericPropertyMatcher().ignoreCase();
		}

		/**
		 * Creates a {@link GenericPropertyMatcher} that matches string case sensitive.
		 *
		 * @return
		 */
		public static GenericPropertyMatcher caseSensitive() {
			return new GenericPropertyMatcher().caseSensitive();
		}

		/**
		 * Creates a {@link GenericPropertyMatcher} that matches string using {@link StringMatcher#CONTAINING}.
		 *
		 * @return
		 */
		public static GenericPropertyMatcher contains() {
			return new GenericPropertyMatcher().contains();
		}

		/**
		 * Creates a {@link GenericPropertyMatcher} that matches string using {@link StringMatcher#ENDING}.
		 *
		 * @return
		 */
		public static GenericPropertyMatcher endsWith() {
			return new GenericPropertyMatcher().endsWith();

		}

		/**
		 * Creates a {@link GenericPropertyMatcher} that matches string using {@link StringMatcher#STARTING}.
		 *
		 * @return
		 */
		public static GenericPropertyMatcher startsWith() {
			return new GenericPropertyMatcher().startsWith();
		}

		/**
		 * Creates a {@link GenericPropertyMatcher} that matches string using {@link StringMatcher#EXACT}.
		 *
		 * @return
		 */
		public static GenericPropertyMatcher exact() {
			return new GenericPropertyMatcher().startsWith();
		}

		/**
		 * Creates a {@link GenericPropertyMatcher} that matches string using {@link StringMatcher#DEFAULT}.
		 *
		 * @return
		 */
		public static GenericPropertyMatcher storeDefaultMatching() {
			return new GenericPropertyMatcher().storeDefaultMatching();
		}

		/**
		 * Creates a {@link GenericPropertyMatcher} that matches string using {@link StringMatcher#REGEX}.
		 *
		 * @return
		 */
		public static GenericPropertyMatcher regex() {
			return new GenericPropertyMatcher().regex();
		}
	}

	/**
	 * Match modes for treatment of {@link String} values.
	 *
	 * @author Christoph Strobl
	 */
	public static enum StringMatcher {

		/**
		 * Store specific default.
		 */
		DEFAULT,
		/**
		 * Matches the exact string
		 */
		EXACT,
		/**
		 * Matches string starting with pattern
		 */
		STARTING,
		/**
		 * Matches string ending with pattern
		 */
		ENDING,
		/**
		 * Matches string containing pattern
		 */
		CONTAINING,
		/**
		 * Treats strings as regular expression patterns
		 */
		REGEX;

	}

	/**
	 * Allows to transform the property value before it is used in the query.
	 */
	public static interface PropertyValueTransformer extends Converter<Object, Object> {}

	/**
	 * @author Christoph Strobl
	 * @since 1.12
	 */
	public static enum NoOpPropertyValueTransformer implements ExampleMatcher.PropertyValueTransformer {

		INSTANCE;

		@Override
		public Object convert(Object source) {
			return source;
		}
	}

	/**
	 * Define specific property handling for a Dot-Path.
	 *
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 * @since 1.12
	 */
	@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	@EqualsAndHashCode
	public static class PropertySpecifier {

		String path;
		StringMatcher stringMatcher;
		Boolean ignoreCase;
		PropertyValueTransformer valueTransformer;

		/**
		 * Creates new {@link PropertySpecifier} for given path.
		 *
		 * @param path Dot-Path to the property. Must not be {@literal null}.
		 */
		PropertySpecifier(String path) {

			Assert.hasText(path, "Path must not be null/empty!");
			this.path = path;

			this.stringMatcher = null;
			this.ignoreCase = null;
			this.valueTransformer = NoOpPropertyValueTransformer.INSTANCE;
		}

		/**
		 * Creates a new {@link PropertySpecifier} containing all values from the current instance and sets
		 * {@link StringMatcher} in the returned instance.
		 *
		 * @param stringMatcher must not be {@literal null}.
		 * @return
		 */
		public PropertySpecifier withStringMatcher(StringMatcher stringMatcher) {

			Assert.notNull(stringMatcher, "StringMatcher must not be null!");
			return new PropertySpecifier(this.path, stringMatcher, this.ignoreCase, this.valueTransformer);
		}

		/**
		 * Creates a new {@link PropertySpecifier} containing all values from the current instance and sets
		 * {@code ignoreCase}.
		 *
		 * @param ignoreCase must not be {@literal null}.
		 * @return
		 */
		public PropertySpecifier withIgnoreCase(boolean ignoreCase) {
			return new PropertySpecifier(this.path, this.stringMatcher, ignoreCase, this.valueTransformer);
		}

		/**
		 * Creates a new {@link PropertySpecifier} containing all values from the current instance and sets
		 * {@link PropertyValueTransformer} in the returned instance.
		 *
		 * @param valueTransformer must not be {@literal null}.
		 * @return
		 */
		public PropertySpecifier withValueTransformer(PropertyValueTransformer valueTransformer) {

			Assert.notNull(valueTransformer, "PropertyValueTransformer must not be null!");
			return new PropertySpecifier(this.path, this.stringMatcher, this.ignoreCase, valueTransformer);
		}

		/**
		 * Get the properties Dot-Path.
		 *
		 * @return never {@literal null}.
		 */
		public String getPath() {
			return path;
		}

		/**
		 * Get the {@link StringMatcher}.
		 *
		 * @return can be {@literal null}.
		 */
		public StringMatcher getStringMatcher() {
			return stringMatcher;
		}

		/**
		 * @return {@literal null} if not set.
		 */
		public Boolean getIgnoreCase() {
			return ignoreCase;
		}

		/**
		 * Get the property transformer to be applied.
		 *
		 * @return never {@literal null}.
		 */
		public PropertyValueTransformer getPropertyValueTransformer() {
			return valueTransformer == null ? NoOpPropertyValueTransformer.INSTANCE : valueTransformer;
		}

		/**
		 * Transforms a given source using the {@link PropertyValueTransformer}.
		 *
		 * @param source
		 * @return
		 */
		public Object transformValue(Object source) {
			return getPropertyValueTransformer().convert(source);
		}
	}

	/**
	 * Define specific property handling for Dot-Paths.
	 *
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 * @since 1.12
	 */
	@EqualsAndHashCode
	public static class PropertySpecifiers {

		private final Map<String, PropertySpecifier> propertySpecifiers = new LinkedHashMap<String, PropertySpecifier>();

		PropertySpecifiers() {}

		PropertySpecifiers(PropertySpecifiers propertySpecifiers) {
			this.propertySpecifiers.putAll(propertySpecifiers.propertySpecifiers);
		}

		public void add(PropertySpecifier specifier) {

			Assert.notNull(specifier, "PropertySpecifier must not be null!");
			propertySpecifiers.put(specifier.getPath(), specifier);
		}

		public boolean hasSpecifierForPath(String path) {
			return propertySpecifiers.containsKey(path);
		}

		public PropertySpecifier getForPath(String path) {
			return propertySpecifiers.get(path);
		}

		public boolean hasValues() {
			return !propertySpecifiers.isEmpty();
		}

		public Collection<PropertySpecifier> getSpecifiers() {
			return propertySpecifiers.values();
		}
	}

	/**
	 * The match modes to expose so that clients can find about how to concatenate the predicates.
	 *
	 * @author Oliver Gierke
	 * @since 1.13
	 * @see ExampleMatcher#isAllMatching()
	 */
	private static enum MatchMode {
		ALL, ANY;
	}
}
