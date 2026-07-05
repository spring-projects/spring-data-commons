/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * Unit tests for {@link ParametersParameterAccessor}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Mark Paluch
 */
class ParametersParameterAccessorUnitTests {

	Parameters<?, ?> parameters;
	RepositoryMetadata metadata;

	@BeforeEach
	void setUp() throws Exception {
		parameters = new DefaultParameters(ParametersSource.of(Sample.class.getMethod("method", String.class, int.class)));
	}

	@Test
	void accessorIteratorHasNext() throws SecurityException, NoSuchMethodException {

		ParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { "Foo", 2 });

		var iterator = accessor.iterator();
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo("Foo");
		assertThat(iterator.hasNext()).isTrue();
		assertThat(iterator.next()).isEqualTo(2);
		assertThat(iterator.hasNext()).isFalse();
	}

	@Test
	void detectsNullValue() throws Exception {

		ParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { null, 5 });
		assertThat(accessor.hasBindableNullValue()).isTrue();

		var method = Sample.class.getMethod("method", Pageable.class, String.class);
		var parameters = new DefaultParameters(ParametersSource.of(method));

		accessor = new ParametersParameterAccessor(parameters, new Object[] { null, "Foo" });
		assertThat(accessor.hasBindableNullValue()).isFalse();
	}

	@Test // DATACMNS-804
	void iteratesonlyOverBindableValues() throws Exception {

		var method = Sample.class.getMethod("method", Pageable.class, String.class);
		var parameters = new DefaultParameters(ParametersSource.of(method));

		var accessor = new ParametersParameterAccessor(parameters, new Object[] { PageRequest.of(0, 10), "Foo" });

		assertThat(accessor).hasSize(1);
		assertThat(accessor.getBindableValue(0)).isEqualTo("Foo");
	}

	@Test // GH-2151
	void handlesScrollPositionAsAParameterType() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", ScrollPosition.class, String.class);
		var parameters = new DefaultParameters(ParametersSource.of(method));

		var accessor = new ParametersParameterAccessor(parameters, new Object[] { ScrollPosition.offset(1), "Foo" });

		assertThat(accessor).hasSize(1);
		assertThat(accessor.getBindableValue(0)).isEqualTo("Foo");
	}

	@Test // #2626
	void handlesPageRequestAsAParameterType() throws NoSuchMethodException {

		var method = Sample.class.getMethod("methodWithPageRequest", PageRequest.class, String.class);
		var parameters = new DefaultParameters(ParametersSource.of(method));

		var accessor = new ParametersParameterAccessor(parameters, new Object[] { PageRequest.of(0, 10), "Foo" });

		assertThat(accessor).hasSize(1);
		assertThat(accessor.getBindableValue(0)).isEqualTo("Foo");
	}

	@Test // GH-2728
	void handlesLimitAsAParameterType() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", Limit.class, String.class);
		var accessor = new ParametersParameterAccessor(new DefaultParameters(ParametersSource.of(method)),
				new Object[] { Limit.of(100), "spring" });

		assertThat(accessor).hasSize(1);
		assertThat(accessor.getBindableValue(0)).isEqualTo("spring");
	}

	@Test // GH-2728
	void returnsLimitIfAvailable() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", Limit.class, String.class);
		var accessor = new ParametersParameterAccessor(new DefaultParameters(ParametersSource.of(method)),
				new Object[] { Limit.of(100), "spring" });

		assertThat(accessor.getLimit()).extracting(Limit::max).isEqualTo(100);
	}

	@Test // GH-2728
	void readsLimitFromPageableIfAvailable() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", Pageable.class, String.class);
		var accessor = new ParametersParameterAccessor(new DefaultParameters(ParametersSource.of(method)),
				new Object[] { Pageable.ofSize(100), "spring" });

		assertThat(accessor.getLimit()).extracting(Limit::max).isEqualTo(100);
	}

	@Test // GH-2728
	void returnsUnlimitedIfNoLimitingAvailable() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", Sort.class, String.class);
		var accessor = new ParametersParameterAccessor(new DefaultParameters(ParametersSource.of(method)),
				new Object[] { Pageable.ofSize(100), "spring" });

		assertThat(accessor.getLimit().isUnlimited()).isTrue();
	}

	@Test // GH-2728
	void appliesLimitToPageableIfAvailable() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", Limit.class, String.class);
		var accessor = new ParametersParameterAccessor(new DefaultParameters(ParametersSource.of(method)),
				new Object[] { Limit.of(100), "spring" });

		Pageable pageable = accessor.getPageable();
		assertThat(pageable).extracting(Pageable::getPageSize).isEqualTo(100);
		assertThat(pageable.getSort().isUnsorted()).isTrue();
	}

	@Test // GH-2728
	void appliesLimitToPageableIfRequested() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", Limit.class, String.class);
		var accessor = new ParametersParameterAccessor(new DefaultParameters(ParametersSource.of(method)),
				new Object[] { Limit.of(100), "spring" });

		assertThat(accessor).hasSize(1);
		assertThat(accessor.getBindableValue(0)).isEqualTo("spring");
	}

	@Test // GH-2728
	void appliesSortToPageableIfAvailable() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", Sort.class, String.class);
		var accessor = new ParametersParameterAccessor(new DefaultParameters(ParametersSource.of(method)),
				new Object[] { Sort.by("one", "two"), "spring" });

		Pageable pageable = accessor.getPageable();
		assertThat(pageable.isPaged()).isFalse();
		assertThat(pageable.getSort()).containsExactly(Order.by("one"), Order.by("two"));
	}

	@Test // GH-2728
	void appliesSortAndLimitToPageableIfAvailable() throws NoSuchMethodException {

		var method = Sample.class.getMethod("method", Sort.class, Limit.class, String.class);
		var accessor = new ParametersParameterAccessor(new DefaultParameters(ParametersSource.of(method)),
				new Object[] { Sort.by("one", "two"), Limit.of(42), "spring" });

		Pageable pageable = accessor.getPageable();
		assertThat(pageable).extracting(Pageable::getPageSize).isEqualTo(42);
		assertThat(pageable.getSort()).containsExactly(Order.by("one"), Order.by("two"));
	}

	interface Sample {

		void method(String string, int integer);

		void method(Pageable pageable, String string);

		void method(ScrollPosition scrollPosition, String string);

		void methodWithPageRequest(PageRequest pageRequest, String string);

		void method(Limit limit, String string);

		void method(Sort sort, Limit limit, String string);

		void method(Sort sort, String string);
	}
}
