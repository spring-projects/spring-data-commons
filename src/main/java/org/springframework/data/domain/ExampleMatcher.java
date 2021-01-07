/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Specification for property path matching to use in query by example (QBE). An {@link ExampleMatcher} can be created
 * for a {@link Class object type}. Instances of {@link ExampleMatcher} can be either {@link #matchingAll()} or
 * {@link #matchingAny()} and settings can be tuned {@code with...} methods in a fluent style. {@code with...} methods
 * return a copy of the {@link ExampleMatcher} instance with the specified setting. Null-handling defaults to
 * {@link NullHandler#IGNORE} and case-sensitive {@link StringMatcher#DEFAULT} string matching.
 * <p>
 * This class is immutable.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Jens Schauder
 * @since 1.12
 */
public interface ExampleMatcher {

	/**
	 * Create a new {@link ExampleMatcher} including all non-null properties by default matching <strong>all</strong>
	 * predicates derived from the example.
	 *
	 * @return new instance of {@link ExampleMatcher}.
	 * @see #matchingAll()
	 */
	static ExampleMatcher matching() {
		return matchingAll();
	}

	/**
	 * Create a new {@link ExampleMatcher} including all non-null properties by default matching <strong>any</strong>
	 * predicate derived from the example.
	 *
	 * @return new instance of {@link ExampleMatcher}.
	 */
	static ExampleMatcher matchingAny() {
		return new TypedExampleMatcher().withMode(MatchMode.ANY);
	}

	/**
	 * Create a new {@link ExampleMatcher} including all non-null properties by default matching <strong>all</strong>
	 * predicates derived from the example.
	 *
	 * @return new instance of {@link ExampleMatcher}.
	 */
	static ExampleMatcher matchingAll() {
		return new TypedExampleMatcher().withMode(MatchMode.ALL);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code propertyPaths}. This instance is immutable
	 * and unaffected by this method call.
	 *
	 * @param ignoredPaths must not be {@literal null} and not empty.
	 * @return new instance of {@link ExampleMatcher}.
	 */
	ExampleMatcher withIgnorePaths(String... ignoredPaths);

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified string matching of {@code defaultStringMatcher}.
	 * This instance is immutable and unaffected by this method call.
	 *
	 * @param defaultStringMatcher must not be {@literal null}.
	 * @return new instance of {@link ExampleMatcher}.
	 */
	ExampleMatcher withStringMatcher(StringMatcher defaultStringMatcher);

	/**
	 * Returns a copy of this {@link ExampleMatcher} with ignoring case sensitivity by default. This instance is immutable
	 * and unaffected by this method call.
	 *
	 * @return new instance of {@link ExampleMatcher}.
	 */
	default ExampleMatcher withIgnoreCase() {
		return withIgnoreCase(true);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with {@code defaultIgnoreCase}. This instance is immutable and
	 * unaffected by this method call.
	 *
	 * @param defaultIgnoreCase
	 * @return new instance of {@link ExampleMatcher}.
	 */
	ExampleMatcher withIgnoreCase(boolean defaultIgnoreCase);

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code GenericPropertyMatcher} for the
	 * {@code propertyPath}. This instance is immutable and unaffected by this method call.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param matcherConfigurer callback to configure a {@link GenericPropertyMatcher}, must not be {@literal null}.
	 * @return new instance of {@link ExampleMatcher}.
	 */
	default ExampleMatcher withMatcher(String propertyPath, MatcherConfigurer<GenericPropertyMatcher> matcherConfigurer) {

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
	 * @return new instance of {@link ExampleMatcher}.
	 */
	ExampleMatcher withMatcher(String propertyPath, GenericPropertyMatcher genericPropertyMatcher);

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code PropertyValueTransformer} for the
	 * {@code propertyPath}.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param propertyValueTransformer must not be {@literal null}.
	 * @return new instance of {@link ExampleMatcher}.
	 */
	ExampleMatcher withTransformer(String propertyPath, PropertyValueTransformer propertyValueTransformer);

	/**
	 * Returns a copy of this {@link ExampleMatcher} with ignore case sensitivity for the {@code propertyPaths}. This
	 * instance is immutable and unaffected by this method call.
	 *
	 * @param propertyPaths must not be {@literal null} and not empty.
	 * @return new instance of {@link ExampleMatcher}.
	 */
	ExampleMatcher withIgnoreCase(String... propertyPaths);

	/**
	 * Returns a copy of this {@link ExampleMatcher} with treatment for {@literal null} values of
	 * {@link NullHandler#INCLUDE} . This instance is immutable and unaffected by this method call.
	 *
	 * @return new instance of {@link ExampleMatcher}.
	 */
	default ExampleMatcher withIncludeNullValues() {
		return withNullHandler(NullHandler.INCLUDE);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with treatment for {@literal null} values of
	 * {@link NullHandler#IGNORE}. This instance is immutable and unaffected by this method call.
	 *
	 * @return new instance of {@link ExampleMatcher}.
	 */
	default ExampleMatcher withIgnoreNullValues() {
		return withNullHandler(NullHandler.IGNORE);
	}

	/**
	 * Returns a copy of this {@link ExampleMatcher} with the specified {@code nullHandler}. This instance is immutable
	 * and unaffected by this method call.
	 *
	 * @param nullHandler must not be {@literal null}.
	 * @return new instance of {@link ExampleMatcher}.
	 */
	ExampleMatcher withNullHandler(NullHandler nullHandler);

	/**
	 * Get defined null handling.
	 *
	 * @return never {@literal null}
	 */
	NullHandler getNullHandler();

	/**
	 * Get defined {@link ExampleMatcher.StringMatcher}.
	 *
	 * @return never {@literal null}.
	 */
	StringMatcher getDefaultStringMatcher();

	/**
	 * @return {@literal true} if {@link String} should be matched with ignore case option.
	 */
	boolean isIgnoreCaseEnabled();

	/**
	 * @param path must not be {@literal null}.
	 * @return return {@literal true} if path was set to be ignored.
	 */
	default boolean isIgnoredPath(String path) {
		return getIgnoredPaths().contains(path);
	}

	/**
	 * @return unmodifiable {@link Set} of ignored paths.
	 */
	Set<String> getIgnoredPaths();

	/**
	 * @return the {@link PropertySpecifiers} within the {@link ExampleMatcher}.
	 */
	PropertySpecifiers getPropertySpecifiers();

	/**
	 * Returns whether all of the predicates of the {@link Example} are supposed to match. If {@literal false} is
	 * returned, it's sufficient if any of the predicates derived from the {@link Example} match.
	 *
	 * @return whether all of the predicates of the {@link Example} are supposed to match or any of them is sufficient.
	 */
	default boolean isAllMatching() {
		return getMatchMode().equals(MatchMode.ALL);
	}

	/**
	 * Returns whether it's sufficient that any of the predicates of the {@link Example} match. If {@literal false} is
	 * returned, all predicates derived from the example need to match to produce results.
	 *
	 * @return whether it's sufficient that any of the predicates of the {@link Example} match or all need to match.
	 */
	default boolean isAnyMatching() {
		return getMatchMode().equals(MatchMode.ANY);
	}

	/**
	 * Get the match mode of the {@link ExampleMatcher}.
	 *
	 * @return never {@literal null}.
	 * @since 2.0
	 */
	MatchMode getMatchMode();

	/**
	 * Null handling for creating criterion out of an {@link Example}.
	 *
	 * @author Christoph Strobl
	 */
	enum NullHandler {

		INCLUDE, IGNORE
	}

	/**
	 * Callback to configure a matcher.
	 *
	 * @author Mark Paluch
	 * @param <T>
	 */
	interface MatcherConfigurer<T> {
		void configureMatcher(T matcher);
	}

	/**
	 * A generic property matcher that specifies {@link StringMatcher string matching} and case sensitivity.
	 *
	 * @author Mark Paluch
	 */
	class GenericPropertyMatcher {

		@Nullable StringMatcher stringMatcher = null;
		@Nullable Boolean ignoreCase = null;
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

		protected boolean canEqual(final Object other) {
			return other instanceof GenericPropertyMatcher;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof GenericPropertyMatcher)) {
				return false;
			}

			GenericPropertyMatcher that = (GenericPropertyMatcher) o;

			if (stringMatcher != that.stringMatcher)
				return false;

			if (!ObjectUtils.nullSafeEquals(ignoreCase, that.ignoreCase)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(valueTransformer, that.valueTransformer);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(stringMatcher);
			result = 31 * result + ObjectUtils.nullSafeHashCode(ignoreCase);
			result = 31 * result + ObjectUtils.nullSafeHashCode(valueTransformer);
			return result;
		}
	}

	/**
	 * Predefined property matchers to create a {@link GenericPropertyMatcher}.
	 *
	 * @author Mark Paluch
	 */
	class GenericPropertyMatchers {

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
			return new GenericPropertyMatcher().exact();
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
	 * @author Jens Schauder
	 */
	enum StringMatcher {

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
	interface PropertyValueTransformer extends Function<Optional<Object>, Optional<Object>> {}

	/**
	 * @author Christoph Strobl
	 * @author Oliver Gierke
	 * @since 1.12
	 */
	enum NoOpPropertyValueTransformer implements ExampleMatcher.PropertyValueTransformer {

		INSTANCE;

		/*
		 * (non-Javadoc)
		 * @see java.util.function.Function#apply(java.lang.Object)
		 */
		@Override
		@SuppressWarnings("null")
		public Optional<Object> apply(Optional<Object> source) {
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
	class PropertySpecifier {

		private final String path;
		private final @Nullable StringMatcher stringMatcher;
		private final @Nullable Boolean ignoreCase;
		private final PropertyValueTransformer valueTransformer;

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

		private PropertySpecifier(String path, @Nullable StringMatcher stringMatcher, @Nullable Boolean ignoreCase,
				PropertyValueTransformer valueTransformer) {
			this.path = path;
			this.stringMatcher = stringMatcher;
			this.ignoreCase = ignoreCase;
			this.valueTransformer = valueTransformer;
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
		@Nullable
		public StringMatcher getStringMatcher() {
			return stringMatcher;
		}

		/**
		 * @return {@literal null} if not set.
		 */
		@Nullable
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
		public Optional<Object> transformValue(Optional<Object> source) {
			return getPropertyValueTransformer().apply(source);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof PropertySpecifier)) {
				return false;
			}

			PropertySpecifier that = (PropertySpecifier) o;

			if (!ObjectUtils.nullSafeEquals(path, that.path)) {
				return false;
			}

			if (stringMatcher != that.stringMatcher)
				return false;

			if (!ObjectUtils.nullSafeEquals(ignoreCase, that.ignoreCase)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(valueTransformer, that.valueTransformer);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(path);
			result = 31 * result + ObjectUtils.nullSafeHashCode(stringMatcher);
			result = 31 * result + ObjectUtils.nullSafeHashCode(ignoreCase);
			result = 31 * result + ObjectUtils.nullSafeHashCode(valueTransformer);
			return result;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof PropertySpecifier;
		}

	}

	/**
	 * Define specific property handling for Dot-Paths.
	 *
	 * @author Christoph Strobl
	 * @author Mark Paluch
	 * @since 1.12
	 */
	class PropertySpecifiers {

		private final Map<String, PropertySpecifier> propertySpecifiers = new LinkedHashMap<>();

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

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof PropertySpecifiers)) {
				return false;
			}

			PropertySpecifiers that = (PropertySpecifiers) o;
			return ObjectUtils.nullSafeEquals(propertySpecifiers, that.propertySpecifiers);
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(propertySpecifiers);
		}
	}

	/**
	 * The match modes to expose so that clients can find about how to concatenate the predicates.
	 *
	 * @author Oliver Gierke
	 * @since 1.13
	 * @see ExampleMatcher#isAllMatching()
	 */
	enum MatchMode {
		ALL, ANY;
	}
}
