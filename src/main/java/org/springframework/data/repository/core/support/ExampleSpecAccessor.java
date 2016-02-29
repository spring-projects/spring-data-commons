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

import org.springframework.data.domain.ExampleSpec;
import org.springframework.data.domain.TypedExampleSpec;

/**
 * Accessor for the {@link ExampleSpec} to use in modules that support query by example (QBE) querying.
 *
 * @author Mark Paluch
 * @since 1.12
 */
public class ExampleSpecAccessor {

	private final ExampleSpec exampleSpec;

	public ExampleSpecAccessor(ExampleSpec exampleSpec) {
		this.exampleSpec = exampleSpec;
	}

	/**
	 * @return unmodifiable {@link Collection} of {@link ExampleSpec.PropertySpecifier}s.
	 */
	public Collection<ExampleSpec.PropertySpecifier> getPropertySpecifiers() {
		return exampleSpec.getPropertySpecifiers().getSpecifiers();
	}

	/**
	 * @param path Dot-Path to property.
	 * @return {@literal true} in case {@link ExampleSpec.PropertySpecifier} defined for given path.
	 */
	public boolean hasPropertySpecifier(String path) {
		return exampleSpec.getPropertySpecifiers().hasSpecifierForPath(path);
	}

	/**
	 * Get the {@link ExampleSpec.PropertySpecifier} for given path. <br />
	 * Please check if {@link #hasPropertySpecifier(String)} to avoid running into {@literal null} values.
	 *
	 * @param path Dot-Path to property.
	 * @return {@literal null} when no {@link ExampleSpec.PropertySpecifier} defined for path.
	 */
	public ExampleSpec.PropertySpecifier getPropertySpecifier(String path) {
		return exampleSpec.getPropertySpecifiers().getForPath(path);
	}

	/**
	 * @return true if at least one {@link ExampleSpec.PropertySpecifier} defined.
	 */
	public boolean hasPropertySpecifiers() {
		return exampleSpec.getPropertySpecifiers().hasValues();
	}

	/**
	 * Get the {@link ExampleSpec.StringMatcher} for a given path or return the default one if none defined.
	 *
	 * @param path
	 * @return never {@literal null}.
	 */
	public ExampleSpec.StringMatcher getStringMatcherForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return exampleSpec.getDefaultStringMatcher();
		}

		ExampleSpec.PropertySpecifier specifier = getPropertySpecifier(path);
		return specifier.getStringMatcher() != null ? specifier.getStringMatcher() : exampleSpec.getDefaultStringMatcher();
	}

	/**
	 * Get defined null handling.
	 *
	 * @return never {@literal null}
	 */
	public ExampleSpec.NullHandler getNullHandler() {
		return exampleSpec.getNullHandler();
	}

	/**
	 * Get defined {@link ExampleSpec.StringMatcher}.
	 *
	 * @return never {@literal null}.
	 */
	public ExampleSpec.StringMatcher getDefaultStringMatcher() {
		return exampleSpec.getDefaultStringMatcher();
	}

	/**
	 * @return {@literal true} if {@link String} should be matched with ignore case option.
	 */
	public boolean isIgnoreCaseEnabled() {
		return exampleSpec.isIgnoreCaseEnabled();
	}

	/**
	 * @param path
	 * @return return {@literal true} if path was set to be ignored.
	 */
	public boolean isIgnoredPath(String path) {
		return exampleSpec.isIgnoredPath(path);
	}

	/**
	 * Get the ignore case flag for a given path or return the default one if none defined.
	 *
	 * @param path
	 * @return never {@literal null}.
	 */
	public boolean isIgnoreCaseForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return exampleSpec.isIgnoreCaseEnabled();
		}

		ExampleSpec.PropertySpecifier specifier = getPropertySpecifier(path);
		return specifier.getIgnoreCase() != null ? specifier.getIgnoreCase().booleanValue()
				: exampleSpec.isIgnoreCaseEnabled();
	}

	/**
	 * Get the ignore case flag for a given path or return {@link ExampleSpec.NoOpPropertyValueTransformer} if none
	 * defined.
	 *
	 * @param path
	 * @return never {@literal null}.
	 */
	public ExampleSpec.PropertyValueTransformer getValueTransformerForPath(String path) {

		if (!hasPropertySpecifier(path)) {
			return ExampleSpec.NoOpPropertyValueTransformer.INSTANCE;
		}

		return getPropertySpecifier(path).getPropertyValueTransformer();
	}

	/**
	 * @return {@literal true} if the {@link ExampleSpec} is typed.
	 */
	public boolean isTyped() {
		return exampleSpec instanceof TypedExampleSpec<?>;
	}

}
