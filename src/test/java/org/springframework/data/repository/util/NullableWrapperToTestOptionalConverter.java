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
package org.springframework.data.repository.util;

import org.springframework.core.convert.ConversionService;

/**
 * Helper type for unit tests for {@link QueryExecutionConverters}.
 *
 * @author Darek Kaczynski
 */
public class NullableWrapperToTestOptionalConverter implements NullableWrapperConverter {

	@Override
	public Class<?>[] getWrapperTypes() {
		return new Class<?>[]{TestOptional.class};
	}

	@Override
	public Object getNullValue() {
		return TestOptional.nullValue();
	}

	@Override
	public boolean canUnwrap() {
		return true;
	}

	@Override
	public Object wrap(Object source, ConversionService conversionService) {
		return TestOptional.of(source);
	}

	@Override
	public Object unwrap(Object source) {
		return ((TestOptional) source).get();
	}

	public abstract static class TestOptional {

		public static TestOptional nullValue() {
			return NullTestOptional.NULL_VALUE;
		}

		public static TestOptional of(Object wrapped) {
			return wrapped == null ? nullValue() : new SomeTestOptional(wrapped);
		}

		public abstract Object get();
	}

	public static class NullTestOptional extends TestOptional {

		private static final TestOptional NULL_VALUE = new NullTestOptional();

		@Override
		public Object get() {
			return null;
		}
	}

	public static class SomeTestOptional extends TestOptional {

		private final Object wrapped;

		public SomeTestOptional(Object wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public Object get() {
			return wrapped;
		}
	}
}
