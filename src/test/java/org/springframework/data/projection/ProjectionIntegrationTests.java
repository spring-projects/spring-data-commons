/*
 * Copyright 2016-present the original author or authors.
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
package org.springframework.data.projection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

/**
 * Integration tests for projections.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
class ProjectionIntegrationTests {

	@Test // DATACMNS-909
	void jacksonSerializationDoesNotExposeDecoratedClass() throws Exception {

		var factory = new ProxyProjectionFactory();
		var projection = factory.createProjection(SampleProjection.class);

		var context = JsonPath.using(new ConfigurationBuilder().options(Option.SUPPRESS_EXCEPTIONS).build());
		var json = context.parse(new ObjectMapper().writeValueAsString(projection));

		assertThat(json.read("$.decoratedClass", String.class)).isNull();
	}

	@Test // GH-3170
	void jacksonSerializationConsidersJspecifyNullableAnnotations() throws Exception {

		var factory = new ProxyProjectionFactory();
		var projection = factory.createProjection(SampleProjectionJSpecify.class);

		var context = JsonPath.using(new ConfigurationBuilder().options(Option.SUPPRESS_EXCEPTIONS).build());
		var json = context.parse(new ObjectMapper().writeValueAsString(projection));

		assertThat(json.read("$.decoratedClass", String.class)).isNull();
	}

	@SuppressWarnings("deprecation")
	interface SampleProjection {
		@Nullable
		String getName();
	}

	interface SampleProjectionJSpecify {
		@org.jspecify.annotations.Nullable
		String getName();
	}
}
