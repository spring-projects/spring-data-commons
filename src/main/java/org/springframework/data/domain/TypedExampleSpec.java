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
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@code TypedExampleSpec} is a special {@link ExampleSpec} that holds information of the type to query for.
 *
 * @author Mark Paluch
 * @since 1.12
 */
public class TypedExampleSpec<T> extends ExampleSpec {

	protected final Class<T> type;

	TypedExampleSpec(Class<T> type) {
		super();

		Assert.notNull(type, "Type must not be null!");

		this.type = type;
	}

	TypedExampleSpec(Class<T> type, NullHandler nullHandler, StringMatcher defaultStringMatcher,
			PropertySpecifiers propertySpecifiers, Set<String> ignoredPaths, boolean defaultIgnoreCase) {

		super(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths, defaultIgnoreCase);

		Assert.notNull(type, "Type must not be null!");
		this.type = type;
	}

	/**
	 * Create a new {@link TypedExampleSpec} for the {@code type} including all non-null properties by default.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	public static <T> TypedExampleSpec<T> of(Class<T> type) {
		return new TypedExampleSpec<T>(type);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and ignore the {@code propertyPaths}.
	 *
	 * @param ignoredPaths must not be {@literal null} and not empty.
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withIgnorePaths(String... ignoredPaths) {

		Assert.notEmpty(ignoredPaths, "IgnoredPaths must not be empty!");
		Assert.noNullElements(ignoredPaths, "IgnoredPaths must not contain null elements!");

		Set<String> newIgnoredPaths = new LinkedHashSet<String>(this.ignoredPaths);
		newIgnoredPaths.addAll(Arrays.asList(ignoredPaths));

		return new TypedExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, newIgnoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set string matching to
	 * {@link StringMatcher#STARTING}.
	 *
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withStringMatcherStarting() {
		return new TypedExampleSpec<T>(type, nullHandler, StringMatcher.STARTING, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set string matching to
	 * {@link StringMatcher#ENDING}.
	 *
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withStringMatcherEnding() {
		return new TypedExampleSpec<T>(type, nullHandler, StringMatcher.ENDING, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set string matching to
	 * {@link StringMatcher#CONTAINING}.
	 *
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withStringMatcherContaining() {
		return new TypedExampleSpec<T>(type, nullHandler, StringMatcher.CONTAINING, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set the {@code defaultStringMatcher}.
	 *
	 * @param defaultStringMatcher must not be {@literal null}.
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withStringMatcher(StringMatcher defaultStringMatcher) {

		Assert.notNull(ignoredPaths, "DefaultStringMatcher must not be empty!");

		return new TypedExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} with ignoring case sensitivity by
	 * default.
	 *
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withIgnoreCase() {
		return withIgnoreCase(true);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} with {@code defaultIgnoreCase}.
	 *
	 * @param defaultIgnoreCase
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withIgnoreCase(boolean defaultIgnoreCase) {
		return new TypedExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
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
	@Override
	public TypedExampleSpec<T> withMatcher(String propertyPath,
			MatcherConfigurer<GenericPropertyMatcher> matcherConfigurer) {

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
	@Override
	public TypedExampleSpec<T> withMatcher(String propertyPath, GenericPropertyMatcher genericPropertyMatcher) {

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

		return new TypedExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
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
	@Override
	public TypedExampleSpec<T> withTransformer(String propertyPath, PropertyValueTransformer propertyValueTransformer) {

		Assert.hasText(propertyPath, "PropertyPath must not be empty!");
		Assert.notNull(propertyValueTransformer, "PropertyValueTransformer must not be empty!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);
		PropertySpecifier propertySpecifier = getOrCreatePropertySpecifier(propertyPath, propertySpecifiers);

		propertySpecifiers.add(propertySpecifier.withValueTransformer(propertyValueTransformer));

		return new TypedExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and ignore case sensitivity for the
	 * {@code propertyPaths}.
	 *
	 * @param propertyPaths must not be {@literal null} and not empty.
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withIgnoreCase(String... propertyPaths) {

		Assert.notEmpty(propertyPaths, "PropertyPaths must not be empty!");
		Assert.noNullElements(propertyPaths, "PropertyPaths must not contain null elements!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);

		for (String propertyPath : propertyPaths) {
			PropertySpecifier propertySpecifier = getOrCreatePropertySpecifier(propertyPath, propertySpecifiers);
			propertySpecifiers.add(propertySpecifier.withIgnoreCase(true));
		}

		return new TypedExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set treatment of {@literal null}
	 * values to {@link NullHandler#INCLUDE}.
	 *
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withIncludeNullValues() {
		return new TypedExampleSpec<T>(type, NullHandler.INCLUDE, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set treatment of {@literal null}
	 * values to {@link NullHandler#IGNORE}.
	 *
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withIgnoreNullValues() {
		return new TypedExampleSpec<T>(type, NullHandler.IGNORE, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Create a new {@link ExampleSpec} based on the current {@link ExampleSpec} and set treatment of {@literal null}
	 * values to {@code nullHandler}.
	 *
	 * @param nullHandler must not be {@literal null}.
	 * @return
	 */
	@Override
	public TypedExampleSpec<T> withNullHandler(NullHandler nullHandler) {

		Assert.notNull(nullHandler, "NullHandler must not be null!");
		return new TypedExampleSpec<T>(type, nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase);
	}

	/**
	 * Get the actual type for the {@link TypedExampleSpec} used. This is usually the given class, but the original class
	 * in case of a CGLIB-generated subclass.
	 *
	 * @return
	 * @see ClassUtils#getUserClass(Class)
	 */
	@SuppressWarnings("unchecked")
	public Class<T> getType() {
		return (Class<T>) ClassUtils.getUserClass(type);
	}

}
