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

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Example.StringMatcher;
import org.springframework.data.domain.PropertySpecifier.PropertyValueTransformer;
import org.springframework.util.Assert;

/**
 * Define specific property handling for a Dot-Path.
 * 
 * @author Christoph Strobl
 * @since 1.12
 */
public class PropertySpecifier {

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
	 * @return {literal true} in case {@link StringMatcher} defined.
	 */
	public boolean hasStringMatcher() {
		return this.stringMatcher != null;
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

	/**
	 * Creates new case ignoring {@link PropertySpecifier} for given path.
	 * 
	 * @param propertyPath must not be {@literal null}.
	 * @return
	 */
	public static PropertySpecifier ignoreCase(String propertyPath) {
		return new Builder(propertyPath).matchStringsWithIgnoreCase().get();
	}

	/**
	 * Creates new {@link PropertySpecifier} using given {@link PropertyValueTransformer}.
	 * 
	 * @param propertyPath must not be {@literal null}.
	 * @param valueTransformer should not be {@literal null}, will be defaulted to {@link NoOpPropertyValueTransformer}.
	 * @return
	 */
	public static PropertySpecifier transform(String propertyPath, PropertyValueTransformer valueTransformer) {
		return new Builder(propertyPath).withValueTransformer(
				valueTransformer != null ? valueTransformer : NoOpPropertyValueTransformer.INSTANCE).get();
	}

	/**
	 * Create new {@link Builder} for specifying {@link PropertySpecifier}.
	 * 
	 * @param propertyPath must not be {@literal null}.
	 * @return
	 */
	public static Builder newPropertySpecifier(String propertyPath) {
		return new Builder(propertyPath);
	}

	/**
	 * Builder for specifying desired behavior of {@link PropertySpecifier}.
	 * 
	 * @author Christoph Strobl
	 */
	public static class Builder {

		private PropertySpecifier specifier;

		Builder(String path) {
			specifier = new PropertySpecifier(path);
		}

		/**
		 * Sets the {@link StringMatcher} used for {@link PropertySpecifier}.
		 * 
		 * @param stringMatcher
		 * @return
		 * @see Builder#stringMatcher(StringMatcher)
		 */
		public Builder withStringMatcher(StringMatcher stringMatcher) {
			return matchString(stringMatcher);
		}

		/**
		 * Sets the {@link PropertyValueTransformer} used for {@link PropertySpecifier}.
		 * 
		 * @param valueTransformer
		 * @return
		 * @see Builder#valueTransformer(PropertyValueTransformer)
		 */
		public Builder withValueTransformer(PropertyValueTransformer valueTransformer) {

			specifier.valueTransformer = valueTransformer;
			return this;
		}

		/**
		 * Sets the {@link PropertyValueTransformer} used for {@link PropertySpecifier}.
		 * 
		 * @param valueTransformer
		 * @return
		 * @see Builder#valueTransformer(PropertyValueTransformer)
		 */
		public Builder transforming(PropertyValueTransformer valueTransformer) {
			return withValueTransformer(valueTransformer);
		}

		/**
		 * Sets the {@link StringMatcher} used for {@link PropertySpecifier}.
		 * 
		 * @param stringMatcher
		 * @return
		 */
		public Builder matchString(StringMatcher stringMatcher) {
			return matchString(stringMatcher, specifier.ignoreCase);
		}

		/**
		 * Sets the {@link StringMatcher} used for {@link PropertySpecifier}.
		 * 
		 * @param stringMatcher
		 * @param ignoreCase
		 * @return
		 */
		public Builder matchString(StringMatcher stringMatcher, Boolean ignoreCase) {

			specifier.stringMatcher = stringMatcher;
			specifier.ignoreCase = ignoreCase;
			return this;
		}

		/**
		 * Enable case ignoring string matching.
		 * 
		 * @return
		 */
		public Builder matchStringsWithIgnoreCase() {
			specifier.ignoreCase = true;
			return this;
		}

		/**
		 * Set string matching to {@link StringMatcher#STARTING}
		 * 
		 * @return
		 */
		public Builder matchStringStartingWith() {
			return matchString(StringMatcher.STARTING);
		}

		/**
		 * Set string matching to {@link StringMatcher#ENDING}
		 * 
		 * @return
		 */
		public Builder matchStringEndingWith() {
			return matchString(StringMatcher.ENDING);
		}

		/**
		 * Set string matching to {@link StringMatcher#CONTAINING}
		 * 
		 * @return
		 */
		public Builder matchStringContaining() {
			return matchString(StringMatcher.CONTAINING);
		}

		/**
		 * @return {@link PropertySpecifier} as defined.
		 */
		public PropertySpecifier get() {
			return this.specifier;
		}
	}

	public static interface PropertyValueTransformer extends Converter<Object, Object> {
		// TODO: should we use the converter interface directly or not at all?
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.12
	 */
	static enum NoOpPropertyValueTransformer implements PropertyValueTransformer {

		INSTANCE;

		@Override
		public Object convert(Object source) {
			return source;
		}

	}
}
