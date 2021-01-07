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
package org.springframework.data.support;

import java.util.Collection;

import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.PropertySpecifier;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;

/**
 * Accessor for the {@link ExampleMatcher} to use in modules that support query by example (QBE) querying.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @since 1.12
 */
public class ExampleMatcherAccessor {

	private final ExampleMatcher matcher;

	public ExampleMatcherAccessor(ExampleMatcher matcher) {
		this.matcher = matcher;
	}

	/**
	 * Returns the {@link PropertySpecifier}s of the underlying {@link ExampleMatcher}.
	 *
	 * @return unmodifiable {@link Collection} of {@link ExampleMatcher.PropertySpecifier}s.
	 */
	public Collection<ExampleMatcher.PropertySpecifier> getPropertySpecifiers() {
		return matcher.getPropertySpecifiers().getSpecifiers();
	}

	/**
	 * Returns whether the underlying {@link ExampleMatcher} contains a {@link PropertySpecifier} for the given path.
	 *
	 * @param path the dot-path identifying a property.
	 * @return {@literal true} in case {@link ExampleMatcher.PropertySpecifier} defined for given path.
	 */
	public boolean hasPropertySpecifier(String path) {
		return matcher.getPropertySpecifiers().hasSpecifierForPath(path);
	}

	/**
	 * Get the {@link ExampleMatcher.PropertySpecifier} for given path. <br />
	 * Please check if {@link #hasPropertySpecifier(String)} to avoid running into {@literal null} values.
	 *
	 * @param path Dot-Path to property.
	 * @return {@literal null} when no {@link ExampleMatcher.PropertySpecifier} defined for path.
	 */
	public ExampleMatcher.PropertySpecifier getPropertySpecifier(String path) {
		return matcher.getPropertySpecifiers().getForPath(path);
	}

	/**
	 * @return true if at least one {@link ExampleMatcher.PropertySpecifier} defined.
	 */
	public boolean hasPropertySpecifiers() {
		return matcher.getPropertySpecifiers().hasValues();
	}

	/**
	 * Get the {@link ExampleMatcher.StringMatcher} for a given path or return the default one if none defined.
	 *
	 * @param path
	 * @return never {@literal null}.
	 */
	public ExampleMatcher.StringMatcher getStringMatcherForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return matcher.getDefaultStringMatcher();
		}

		ExampleMatcher.PropertySpecifier specifier = getPropertySpecifier(path);
		StringMatcher stringMatcher = specifier.getStringMatcher();

		return stringMatcher != null ? stringMatcher : matcher.getDefaultStringMatcher();
	}

	/**
	 * Get defined null handling.
	 *
	 * @return never {@literal null}
	 */
	public ExampleMatcher.NullHandler getNullHandler() {
		return matcher.getNullHandler();
	}

	/**
	 * Get defined {@link ExampleMatcher.StringMatcher}.
	 *
	 * @return never {@literal null}.
	 */
	public ExampleMatcher.StringMatcher getDefaultStringMatcher() {
		return matcher.getDefaultStringMatcher();
	}

	/**
	 * @return {@literal true} if {@link String} should be matched with ignore case option.
	 */
	public boolean isIgnoreCaseEnabled() {
		return matcher.isIgnoreCaseEnabled();
	}

	/**
	 * @param path
	 * @return return {@literal true} if path was set to be ignored.
	 */
	public boolean isIgnoredPath(String path) {
		return matcher.isIgnoredPath(path);
	}

	/**
	 * Get the ignore case flag for a given path or return the default one if none defined.
	 *
	 * @param path
	 * @return never {@literal null}.
	 */
	public boolean isIgnoreCaseForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return matcher.isIgnoreCaseEnabled();
		}

		ExampleMatcher.PropertySpecifier specifier = getPropertySpecifier(path);
		Boolean ignoreCase = specifier.getIgnoreCase();

		return ignoreCase != null ? ignoreCase : matcher.isIgnoreCaseEnabled();
	}

	/**
	 * Get the ignore case flag for a given path or return {@link ExampleMatcher.NoOpPropertyValueTransformer} if none
	 * defined.
	 *
	 * @param path
	 * @return never {@literal null}.
	 */
	public ExampleMatcher.PropertyValueTransformer getValueTransformerForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return ExampleMatcher.NoOpPropertyValueTransformer.INSTANCE;
		}

		return getPropertySpecifier(path).getPropertyValueTransformer();
	}
}
