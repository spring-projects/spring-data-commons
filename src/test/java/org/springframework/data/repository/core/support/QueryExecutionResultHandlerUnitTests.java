/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.repository.Repository;

/**
 * Unit tests for {@link QueryExecutionResultHandler}.
 * 
 * @author Oliver Gierke
 */
public class QueryExecutionResultHandlerUnitTests {

	QueryExecutionResultHandler handler = new QueryExecutionResultHandler();

	/**
	 * @see DATACMNS-610
	 */
	@Test
	public void convertsListsToSet() throws Exception {

		TypeDescriptor descriptor = getTypeDescriptorFor("set");
		List<Entity> source = Collections.singletonList(new Entity());

		assertThat(handler.postProcessInvocationResult(source, descriptor), is(instanceOf(Set.class)));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	public void turnsNullIntoJdk8Optional() throws Exception {

		Object result = handler.postProcessInvocationResult(null, getTypeDescriptorFor("jdk8Optional"));
		assertThat(result, is((Object) Optional.empty()));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void wrapsValueIntoJdk8Optional() throws Exception {

		Entity entity = new Entity();

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("jdk8Optional"));
		assertThat(result, is(instanceOf(Optional.class)));

		Optional<Entity> optional = (Optional<Entity>) result;
		assertThat(optional, is(Optional.of(entity)));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	public void turnsNullIntoGuavaOptional() throws Exception {

		Object result = handler.postProcessInvocationResult(null, getTypeDescriptorFor("guavaOptional"));
		assertThat(result, is((Object) com.google.common.base.Optional.absent()));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void wrapsValueIntoGuavaOptional() throws Exception {

		Entity entity = new Entity();

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("guavaOptional"));
		assertThat(result, is(instanceOf(com.google.common.base.Optional.class)));

		com.google.common.base.Optional<Entity> optional = (com.google.common.base.Optional<Entity>) result;
		assertThat(optional, is(com.google.common.base.Optional.of(entity)));
	}

	/**
	 * @see DATACMNS-917
	 */
	@Test
	public void defaultsNullToEmptyMap() throws Exception {
		assertThat(handler.postProcessInvocationResult(null, getTypeDescriptorFor("map")), is(instanceOf(Map.class)));
	}

	private static TypeDescriptor getTypeDescriptorFor(String methodName) throws Exception {

		Method method = Sample.class.getMethod(methodName);
		MethodParameter parameter = new MethodParameter(method, -1);

		return TypeDescriptor.nested(parameter, 0);
	}

	static interface Sample extends Repository<Entity, Long> {

		Set<Entity> set();

		Optional<Entity> jdk8Optional();

		com.google.common.base.Optional<Entity> guavaOptional();

		Map<Integer, Entity> map();
	}

	static class Entity {}
}
