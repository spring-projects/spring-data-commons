/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.core;

import org.junit.platform.commons.annotation.Testable;
import org.openjdk.jmh.annotations.Benchmark;
import org.springframework.data.BenchmarkSettings;

/**
 * Benchmarks for {@link TypedPropertyPath}.
 *
 * @author Mark Paluch
 */
@Testable
public class TypedPropertyPathBenchmarks extends BenchmarkSettings {

	@Benchmark
	public Object benchmarkMethodReference() {
		return TypedPropertyPath.path(Person::firstName);
	}

	@Benchmark
	public Object benchmarkComposedMethodReference() {
		return TypedPropertyPath.path(Person::address).then(Address::city);
	}

	@Benchmark
	public TypedPropertyPath<Person, String> benchmarkLambda() {
		return TypedPropertyPath.path(person -> person.firstName());
	}

	@Benchmark
	public TypedPropertyPath<Person, String> benchmarkComposedLambda() {
		return TypedPropertyPath.path((Person person) -> person.address()).then(address -> address.city());
	}

	@Benchmark
	public Object dotPath() {
		return TypedPropertyPath.path(Person::firstName).toDotPath();
	}

	@Benchmark
	public Object composedDotPath() {
		return TypedPropertyPath.path(Person::address).then(Address::city).toDotPath();
	}

	record Person(String firstName, String lastName, Address address) {

	}

	record Address(String city) {

	}

}
