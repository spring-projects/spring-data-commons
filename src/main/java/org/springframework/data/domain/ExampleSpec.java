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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.util.Assert;

/**
 * Specification for property path matching to use in query by example (QBE). An {@link ExampleSpec} can be created for
 * a {@link Class object type}. Instances of {@link ExampleSpec} but they can be refined using the various
 * {@code with...} methods in a fluent style. A {@code with...} method creates a new instance of {@link ExampleSpec}
 * containing all settings from the current instance but sets the value in the new instance. Null-handling defaults to
 * {@link NullHandler#IGNORE} and case-sensitive {@link StringMatcher#DEFAULT} string matching.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @param <T>
 * @since 1.12
 */
public class ExampleSpec<T> {

	private final Class<T> type;
	private final NullHandler nullHandler;
	private final StringMatcher defaultStringMatcher;
	private final boolean defaultIgnoreCase;
	private final PropertySpecifiers propertySpecifiers;
	private final Set<String> ignoredPaths;

	private ExampleSpec(Class<T> type) {

		Assert.notNull(type, "Type must not be null!");

		this.type = type;
		this.nullHandler = NullHandler.IGNORE;
		this.defaultStringMatcher = StringMatcher.DEFAULT;
		this.propertySpecifiers = new PropertySpecifiers();
		this.defaultIgnoreCase = false;
		this.ignoredPaths = Collections.emptySet();
	}

	private ExampleSpec(Class<T> type, NullHandler nullHandler, StringMatcher defaultStringMatcher,
			PropertySpecifiers propertySpecifiers, Set<String> ignoredPaths, boolean defaultIgnoreCase) {

		Assert.notNull(type, "Type must not be null!");

		this.type = type;
		this.nullHandler = nullHandler;
		this.defaultStringMatcher = defaultStringMatcher;
		this.propertySpecifiers = propertySpecifiers;
		this.ignoredPaths = Collections.unmodifiableSet(ignoredPaths);
		this.defaultIgnoreCase = defaultIgnoreCase;
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and ignore the {@code propertyPaths}.
	 *
	 * @param ignoredPaths must not be {@literal null} and not empty.
	 * @return
	 */
	public ExampleSpec<T> withIgnorePaths(String... ignoredPaths) {

		Assert.notEmpty(ignoredPaths, "IgnoredPaths must not be empty!");
		Assert.noNullElements(ignoredPaths, "IgnoredPaths must not contain null elements!");

		Set<String> newIgnoredPaths = new LinkedHashSet<String>(this.ignoredPaths);
		newIgnoredPaths.addAll(Arrays.asList(ignoredPaths));

		return new ExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, newIgnoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set string matching to
	 * {@link StringMatcher#STARTING}.
	 *
	 * @return
	 */
	public ExampleSpec<T> withStringMatcherStarting() {
		return new ExampleSpec<T>(type, nullHandler, StringMatcher.STARTING, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set string matching to
	 * {@link StringMatcher#ENDING}.
	 *
	 * @return
	 */
	public ExampleSpec<T> withStringMatcherEnding() {
		return new ExampleSpec<T>(type, nullHandler, StringMatcher.ENDING, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set string matching to
	 * {@link StringMatcher#CONTAINING}.
	 *
	 * @return
	 */
	public ExampleSpec<T> withStringMatcherContaining() {
		return new ExampleSpec<T>(type, nullHandler, StringMatcher.CONTAINING, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set the {@code defaultStringMatcher}.
	 *
	 * @param defaultStringMatcher must not be {@literal null}.
	 * @return
	 */
	public ExampleSpec<T> withStringMatcher(StringMatcher defaultStringMatcher) {

		Assert.notNull(ignoredPaths, "DefaultStringMatcher must not be empty!");

		return new ExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} with ignoring case sensitivity by
	 * default.
	 *
	 * @return
	 */
	public ExampleSpec<T> withIgnoreCase() {
		return withIgnoreCase(true);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} with {@code defaultIgnoreCase}.
	 *
	 * @param defaultIgnoreCase
	 * @return
	 */
	public ExampleSpec<T> withIgnoreCase(boolean defaultIgnoreCase) {
		return new ExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and configure a
	 * {@code GenericPropertyMatcher} for the {@code propertyPath}.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param matcherConfigurer callback to configure a {@link GenericPropertyMatcher}, must not be {@literal null}.
	 * @return
	 */
	public ExampleSpec<T> withMatcher(String propertyPath, MatcherConfigurer<GenericPropertyMatcher> matcherConfigurer) {

		Assert.hasText(propertyPath, "PropertyPath must not be empty!");
		Assert.notNull(matcherConfigurer, "MatcherConfigurer must not be empty!");

		GenericPropertyMatcher genericPropertyMatcher = new GenericPropertyMatcher();
		matcherConfigurer.configureMatcher(genericPropertyMatcher);

		return withMatcher(propertyPath, genericPropertyMatcher);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and configure a
	 * {@code GenericPropertyMatcher} for the {@code propertyPath}.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param genericPropertyMatcher callback to configure a {@link GenericPropertyMatcher}, must not be {@literal null}.
	 * @return
	 */
	public ExampleSpec<T> withMatcher(String propertyPath, GenericPropertyMatcher genericPropertyMatcher) {

		Assert.hasText(propertyPath, "PropertyPath must not be empty!");
		Assert.notNull(genericPropertyMatcher, "GenericPropertyMatcher must not be empty!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);
		PropertySpecifier propertySpecifier = new PropertySpecifier(propertyPath);

		propertySpecifier.stringMatcher = genericPropertyMatcher.stringMatcher;
		propertySpecifier.ignoreCase = genericPropertyMatcher.ignoreCase;
		propertySpecifier.valueTransformer = genericPropertyMatcher.valueTransformer;

		propertySpecifiers.add(propertySpecifier);

		return new ExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and configure a
	 * {@code PropertyValueTransformer} for the {@code propertyPath}.
	 *
	 * @param propertyPath must not be {@literal null}.
	 * @param propertyValueTransformer must not be {@literal null}.
	 * @return
	 */
	public ExampleSpec<T> withTransformer(String propertyPath, PropertyValueTransformer propertyValueTransformer) {

		Assert.hasText(propertyPath, "PropertyPath must not be empty!");
		Assert.notNull(propertyValueTransformer, "PropertyValueTransformer must not be empty!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);
		PropertySpecifier propertySpecifier = createOrClonePropertySpecifier(propertyPath, propertySpecifiers);

		propertySpecifier.valueTransformer = propertyValueTransformer;

		propertySpecifiers.add(propertySpecifier);

		return new ExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and ignore case sensitivity for the
	 * {@code propertyPaths}.
	 *
	 * @param propertyPaths must not be {@literal null} and not empty.
	 * @return
	 */
	public ExampleSpec<T> withIgnoreCase(String... propertyPaths) {

		Assert.notEmpty(propertyPaths, "PropertyPaths must not be empty!");
		Assert.noNullElements(propertyPaths, "PropertyPaths must not contain null elements!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);

		for (String propertyPath : propertyPaths) {
			PropertySpecifier propertySpecifier = createOrClonePropertySpecifier(propertyPath, propertySpecifiers);
			propertySpecifier.ignoreCase = true;
			propertySpecifiers.add(propertySpecifier);
		}

		return new ExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	private PropertySpecifier createOrClonePropertySpecifier(String propertyPath, PropertySpecifiers propertySpecifiers) {
		PropertySpecifier propertySpecifier;

		if (propertySpecifiers.hasSpecifierForPath(propertyPath)) {
			propertySpecifier = new PropertySpecifier(propertyPath);
			PropertySpecifier existing = propertySpecifiers.getForPath(propertyPath);
			propertySpecifier.ignoreCase = existing.ignoreCase;
			propertySpecifier.stringMatcher = existing.stringMatcher;
		} else {
			propertySpecifier = new PropertySpecifier(propertyPath);
		}
		return propertySpecifier;
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set treatment of {@literal null}
	 * values to {@link NullHandler#INCLUDE}.
	 *
	 * @return
	 */
	public ExampleSpec<T> withIncludeNullValues() {
		return new ExampleSpec<T>(type, NullHandler.INCLUDE, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set treatment of {@literal null}
	 * values to {@link NullHandler#IGNORE}.
	 *
	 * @return
	 */
	public ExampleSpec<T> withIgnoreNullValues() {
		return new ExampleSpec<T>(type, NullHandler.IGNORE, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set treatment of {@literal null}
	 * values to {@code nullHandler}.
	 *
	 * @param nullHandler must not be {@literal null}.
	 * @return
	 */
	public ExampleSpec<T> withNullHandler(NullHandler nullHandler) {

		Assert.notNull(nullHandler, "NullHandler must not be null!");
		return new ExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link Example} containing the {@code probe}.
	 *
	 * @param probe must not be {@literal null}.
	 * @return
	 */
	public Example<T> createExample(T probe) {
		return Example.of(probe, this);
	}

	/**
	 * Get defined null handling.
	 *
	 * @return never {@literal null}
	 */
	public ExampleSpec.NullHandler getNullHandler() {
		return nullHandler;
	}

	/**
	 * Get defined {@link ExampleSpec.StringMatcher}.
	 *
	 * @return never {@literal null}.
	 */
	public ExampleSpec.StringMatcher getDefaultStringMatcher() {
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

	PropertySpecifiers getPropertySpecifiers() {
		return propertySpecifiers;
	}

	/**
	 * Create a new {@link ExampleSpec} including all non-null properties by default.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static <T> ExampleSpec<T> of(Class<T> type) {
		return new ExampleSpec<T>(type);
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
	public static class GenericPropertyMatcher {

		private StringMatcher stringMatcher = null;
		private Boolean ignoreCase = null;
		private PropertyValueTransformer valueTransformer = NoOpPropertyValueTransformer.INSTANCE;

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
		DEFAULT(null),
		/**
		 * Matches the exact string
		 */
		EXACT(Type.SIMPLE_PROPERTY),
		/**
		 * Matches string starting with pattern
		 */
		STARTING(Type.STARTING_WITH),
		/**
		 * Matches string ending with pattern
		 */
		ENDING(Type.ENDING_WITH),
		/**
		 * Matches string containing pattern
		 */
		CONTAINING(Type.CONTAINING),
		/**
		 * Treats strings as regular expression patterns
		 */
		REGEX(Type.REGEX);

		private Type type;

		private StringMatcher(Type type) {
			this.type = type;
		}

		/**
		 * Get the according {@link Part.Type}.
		 *
		 * @return {@literal null} for {@link StringMatcher#DEFAULT}.
		 */
		public Type getPartType() {
			return type;
		}

	}

	/**
	 * Allows to transform the property value before it is used in the query.
	 */
	public static interface PropertyValueTransformer extends Converter<Object, Object> {
		// TODO: should we use the converter interface directly or not at all?
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.12
	 */
	static enum NoOpPropertyValueTransformer implements ExampleSpec.PropertyValueTransformer {

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
	 * @since 1.12
	 */
	static class PropertySpecifier {

		private final String path;

		private StringMatcher stringMatcher;
		private Boolean ignoreCase;
		private PropertyValueTransformer valueTransformer;

		/**
		 * Creates new {@link PropertySpecifier} for given path.
		 *
		 * @param path Dot-Path to the property. Must not be {@literal null}.
		 */
		PropertySpecifier(String path) {

			Assert.hasText(path, "Path must not be null/empty!");
			this.path = path;
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

	static class PropertySpecifiers {

		private final Map<String, PropertySpecifier> propertySpecifiers = new LinkedHashMap<String, PropertySpecifier>();

		public PropertySpecifiers() {}

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

}
