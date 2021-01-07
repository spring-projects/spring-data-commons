/*
 * Copyright 2017-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Default implementation of {@link ExampleMatcher}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.0
 */
class TypedExampleMatcher implements ExampleMatcher {

	private final NullHandler nullHandler;
	private final StringMatcher defaultStringMatcher;
	private final PropertySpecifiers propertySpecifiers;
	private final Set<String> ignoredPaths;
	private final boolean defaultIgnoreCase;
	private final MatchMode mode;

	TypedExampleMatcher() {

		this(NullHandler.IGNORE, StringMatcher.DEFAULT, new PropertySpecifiers(), Collections.emptySet(), false,
				MatchMode.ALL);
	}

	private TypedExampleMatcher(NullHandler nullHandler, StringMatcher defaultStringMatcher,
			PropertySpecifiers propertySpecifiers, Set<String> ignoredPaths, boolean defaultIgnoreCase, MatchMode mode) {
		this.nullHandler = nullHandler;
		this.defaultStringMatcher = defaultStringMatcher;
		this.propertySpecifiers = propertySpecifiers;
		this.ignoredPaths = ignoredPaths;
		this.defaultIgnoreCase = defaultIgnoreCase;
		this.mode = mode;
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

	TypedExampleMatcher withMode(MatchMode mode) {
		return this.mode == mode ? this
				: new TypedExampleMatcher(this.nullHandler, this.defaultStringMatcher, this.propertySpecifiers,
						this.ignoredPaths, this.defaultIgnoreCase, mode);
	}

	private PropertySpecifier getOrCreatePropertySpecifier(String propertyPath, PropertySpecifiers propertySpecifiers) {

		if (propertySpecifiers.hasSpecifierForPath(propertyPath)) {
			return propertySpecifiers.getForPath(propertyPath);
		}

		return new PropertySpecifier(propertyPath);
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

		if (!(o instanceof TypedExampleMatcher)) {
			return false;
		}

		TypedExampleMatcher that = (TypedExampleMatcher) o;

		if (defaultIgnoreCase != that.defaultIgnoreCase) {
			return false;
		}

		if (nullHandler != that.nullHandler) {
			return false;
		}

		if (defaultStringMatcher != that.defaultStringMatcher) {
			return false;
		}

		if (!ObjectUtils.nullSafeEquals(propertySpecifiers, that.propertySpecifiers)) {

			return false;
		}

		if (!ObjectUtils.nullSafeEquals(ignoredPaths, that.ignoredPaths)) {
			return false;
		}

		return mode == that.mode;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(nullHandler);
		result = 31 * result + ObjectUtils.nullSafeHashCode(defaultStringMatcher);
		result = 31 * result + ObjectUtils.nullSafeHashCode(propertySpecifiers);
		result = 31 * result + ObjectUtils.nullSafeHashCode(ignoredPaths);
		result = 31 * result + (defaultIgnoreCase ? 1 : 0);
		result = 31 * result + ObjectUtils.nullSafeHashCode(mode);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TypedExampleMatcher{" + "nullHandler=" + nullHandler + ", defaultStringMatcher=" + defaultStringMatcher
				+ ", propertySpecifiers=" + propertySpecifiers + ", ignoredPaths=" + ignoredPaths + ", defaultIgnoreCase="
				+ defaultIgnoreCase + ", mode=" + mode + '}';
	}
}
