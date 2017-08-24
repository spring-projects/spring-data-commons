/*
 * Copyright 2017. the original author or authors.
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
import lombok.experimental.Wither;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link ExampleMatcher}.
 *
 * @author Christoph Strobl
 * @since 2.0
 */
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class TypedExampleMatcher implements ExampleMatcher {

	private final NullHandler nullHandler;
	private final StringMatcher defaultStringMatcher;
	private final PropertySpecifiers propertySpecifiers;
	private final Set<String> ignoredPaths;
	private final boolean defaultIgnoreCase;
	private final @Wither(AccessLevel.PACKAGE) MatchMode mode;

	TypedExampleMatcher() {

		this(NullHandler.IGNORE, StringMatcher.DEFAULT, new PropertySpecifiers(), Collections.emptySet(), false,
				MatchMode.ALL);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#withIgnorePaths(java.lang.String...)
	 */
	@Override
	public ExampleMatcher withIgnorePaths(String... ignoredPaths) {

		Assert.notEmpty(ignoredPaths, "IgnoredPaths must not be empty!");
		Assert.noNullElements(ignoredPaths, "IgnoredPaths must not contain null elements!");

		Set<String> newIgnoredPaths = new LinkedHashSet<>(this.ignoredPaths);
		newIgnoredPaths.addAll(Arrays.asList(ignoredPaths));

		return new TypedExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, newIgnoredPaths,
				defaultIgnoreCase, mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#withStringMatcher(java.lang.String)
	 */
	@Override
	public ExampleMatcher withStringMatcher(StringMatcher defaultStringMatcher) {

		Assert.notNull(ignoredPaths, "DefaultStringMatcher must not be empty!");

		return new TypedExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase, mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#withIgnoreCase(boolean)
	 */
	@Override
	public ExampleMatcher withIgnoreCase(boolean defaultIgnoreCase) {
		return new TypedExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase, mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#withMatcher(java.lang.String, org.springframework.data.domain.ExampleMatcher.MatcherConfigurer)
	 */
	@Override
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

		propertySpecifier = propertySpecifier.withValueTransformer(genericPropertyMatcher.valueTransformer);

		propertySpecifiers.add(propertySpecifier);

		return new TypedExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase, mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#withTransformer(java.lang.String, org.springframework.data.domain.ExampleMatcher.PropertyValueTransformer)
	 */
	@Override
	public ExampleMatcher withTransformer(String propertyPath, PropertyValueTransformer propertyValueTransformer) {

		Assert.hasText(propertyPath, "PropertyPath must not be empty!");
		Assert.notNull(propertyValueTransformer, "PropertyValueTransformer must not be empty!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);
		PropertySpecifier propertySpecifier = getOrCreatePropertySpecifier(propertyPath, propertySpecifiers);

		propertySpecifiers.add(propertySpecifier.withValueTransformer(propertyValueTransformer));

		return new TypedExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase, mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#withIgnoreCase(java.lang.String...)
	 */
	@Override
	public ExampleMatcher withIgnoreCase(String... propertyPaths) {

		Assert.notEmpty(propertyPaths, "PropertyPaths must not be empty!");
		Assert.noNullElements(propertyPaths, "PropertyPaths must not contain null elements!");

		PropertySpecifiers propertySpecifiers = new PropertySpecifiers(this.propertySpecifiers);

		for (String propertyPath : propertyPaths) {
			PropertySpecifier propertySpecifier = getOrCreatePropertySpecifier(propertyPath, propertySpecifiers);
			propertySpecifiers.add(propertySpecifier.withIgnoreCase(true));
		}

		return new TypedExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase, mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#withNullHandler(org.springframework.data.domain.ExampleMatcher.NullHandler)
	 */
	@Override
	public ExampleMatcher withNullHandler(NullHandler nullHandler) {

		Assert.notNull(nullHandler, "NullHandler must not be null!");
		return new TypedExampleMatcher(nullHandler, defaultStringMatcher, propertySpecifiers, ignoredPaths,
				defaultIgnoreCase, mode);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#getNullHandler()
	 */
	@Override
	public NullHandler getNullHandler() {
		return nullHandler;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#getDefaultStringMatcher()
	 */
	@Override
	public StringMatcher getDefaultStringMatcher() {
		return defaultStringMatcher;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#isIgnoreCaseEnabled()
	 */
	@Override
	public boolean isIgnoreCaseEnabled() {
		return this.defaultIgnoreCase;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#getIgnoredPaths()
	 */
	@Override
	public Set<String> getIgnoredPaths() {
		return ignoredPaths;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#getPropertySpecifiers()
	 */
	@Override
	public PropertySpecifiers getPropertySpecifiers() {
		return propertySpecifiers;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.domain.ExampleMatcher#getMatchMode()
	 */
	@Override
	public MatchMode getMatchMode() {
		return mode;
	}

	private PropertySpecifier getOrCreatePropertySpecifier(String propertyPath, PropertySpecifiers propertySpecifiers) {

		if (propertySpecifiers.hasSpecifierForPath(propertyPath)) {
			return propertySpecifiers.getForPath(propertyPath);
		}

		return new PropertySpecifier(propertyPath);
	}
}
