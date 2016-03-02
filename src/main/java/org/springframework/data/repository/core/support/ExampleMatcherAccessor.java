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
package org.springframework.data.repository.core.support;

import java.util.Collection;

import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.PropertySpecifier;

/**
 * Accessor for the {@link ExampleMatcher} to use in modules that support query by example (QBE) querying.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @since 1.12
 */
public class ExampleMatcherAccessor {

	private final ExampleMatcher specification;

	public ExampleMatcherAccessor(ExampleMatcher specification) {
		this.specification = specification;
	}

	/**
	 * Returns the {@link PropertySpecifier}s of the underlying {@link ExampleMatcher}.
	 * 
	 * @return unmodifiable {@link Collection} of {@link ExampleMatcher.PropertySpecifier}s.
	 */
	public Collection<ExampleMatcher.PropertySpecifier> getPropertySpecifiers() {
		return specification.getPropertySpecifiers().getSpecifiers();
	}

	/**
	 * Returns whether the underlying {@link ExampleMatcher} contains a {@link PropertySpecifier} for the given path.
	 * 
	 * @param path the dot-path identifying a property.
	 * @return {@literal true} in case {@link ExampleMatcher.PropertySpecifier} defined for given path.
	 */
	public boolean hasPropertySpecifier(String path) {
		return specification.getPropertySpecifiers().hasSpecifierForPath(path);
	}

	/**
	 * Get the {@link ExampleMatcher.PropertySpecifier} for given path. <br />
	 * Please check if {@link #hasPropertySpecifier(String)} to avoid running into {@literal null} values.
	 *
	 * @param path Dot-Path to property.
	 * @return {@literal null} when no {@link ExampleMatcher.PropertySpecifier} defined for path.
	 */
	public ExampleMatcher.PropertySpecifier getPropertySpecifier(String path) {
		return specification.getPropertySpecifiers().getForPath(path);
	}

	/**
	 * @return true if at least one {@link ExampleMatcher.PropertySpecifier} defined.
	 */
	public boolean hasPropertySpecifiers() {
		return specification.getPropertySpecifiers().hasValues();
	}

	/**
	 * Get the {@link ExampleMatcher.StringMatcher} for a given path or return the default one if none defined.
	 *
	 * @param path
	 * @return never {@literal null}.
	 */
	public ExampleMatcher.StringMatcher getStringMatcherForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return specification.getDefaultStringMatcher();
		}

		ExampleMatcher.PropertySpecifier specifier = getPropertySpecifier(path);
		return specifier.getStringMatcher() != null ? specifier.getStringMatcher()
				: specification.getDefaultStringMatcher();
	}

	/**
	 * Get defined null handling.
	 *
	 * @return never {@literal null}
	 */
	public ExampleMatcher.NullHandler getNullHandler() {
		return specification.getNullHandler();
	}

	/**
	 * Get defined {@link ExampleMatcher.StringMatcher}.
	 *
	 * @return never {@literal null}.
	 */
	public ExampleMatcher.StringMatcher getDefaultStringMatcher() {
		return specification.getDefaultStringMatcher();
	}

	/**
	 * @return {@literal true} if {@link String} should be matched with ignore case option.
	 */
	public boolean isIgnoreCaseEnabled() {
		return specification.isIgnoreCaseEnabled();
	}

	/**
	 * @param path
	 * @return return {@literal true} if path was set to be ignored.
	 */
	public boolean isIgnoredPath(String path) {
		return specification.isIgnoredPath(path);
	}

	/**
	 * Get the ignore case flag for a given path or return the default one if none defined.
	 *
	 * @param path
	 * @return never {@literal null}.
	 */
	public boolean isIgnoreCaseForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return specification.isIgnoreCaseEnabled();
		}

		ExampleMatcher.PropertySpecifier specifier = getPropertySpecifier(path);
		return specifier.getIgnoreCase() != null ? specifier.getIgnoreCase().booleanValue()
				: specification.isIgnoreCaseEnabled();
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
