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
package org.springframework.data.projection;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration.ConfigurationBuilder;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;

/**
 * Integration tests for projections.
 * 
 * @author Oliver Gierke
 */
public class ProjectionIntegrationTests {

	/**
	 * @see DATACMNS-909
	 */
	@Test
	public void jacksonSerializationDoesNotExposeDecoratedClass() throws Exception {

		ProxyProjectionFactory factory = new ProxyProjectionFactory();
		SampleProjection projection = factory.createProjection(SampleProjection.class);

		ParseContext context = JsonPath.using(new ConfigurationBuilder().options(Option.SUPPRESS_EXCEPTIONS).build());
		DocumentContext json = context.parse(new ObjectMapper().writeValueAsString(projection));

		assertThat(json.read("$.decoratedClass", String.class), is(nullValue()));
	}

	interface SampleProjection {
		String getName();
	}
}
