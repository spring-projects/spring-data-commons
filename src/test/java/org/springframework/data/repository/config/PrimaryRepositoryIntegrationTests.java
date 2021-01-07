/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.repository.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.CrudRepository;

/**
 * Integration tests for DATACMNS-1591.
 *
 * @author Oliver Drotbohm
 */
class PrimaryRepositoryIntegrationTests {

	@Test // DATACMNS-1591
	void returnsPrimaryInstance() {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

		// Two beans available but FirstRepository is the primary one
		assertThatCode(() -> context.getBean(FirstRepository.class)).doesNotThrowAnyException();
	}

	@Configuration
	@EnableRepositories(considerNestedRepositories = true, //
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Marker.class))
	static class Config {}

	interface Marker {}

	@Primary
	interface FirstRepository<T> extends CrudRepository<T, Long>, Marker {}

	interface SecondRepository extends FirstRepository<Object>, Marker {}
}
