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
 * Benchmarks for {@link SerializableLambdaReader}.
 *
 * @author Mark Paluch
 */
@Testable
public class SerializableLambdaReaderBenchmarks extends BenchmarkSettings {

	private static final SerializableLambdaReader reader = new SerializableLambdaReader(PropertyReference.class);

	@Benchmark
	public Object benchmarkMethodReference() {

		PropertyReference<Person, String> methodReference = Person::firstName;
		return reader.read(methodReference);
	}

	@Benchmark
	public Object benchmarkLambda() {

		PropertyReference<Person, String> methodReference = person -> person.firstName();
		return reader.read(methodReference);
	}

	record Person(String firstName, String lastName) {

	}

}
