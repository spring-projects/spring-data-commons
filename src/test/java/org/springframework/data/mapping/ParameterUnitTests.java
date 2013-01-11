/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.mapping;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.lang.annotation.Annotation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link Parameter}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class ParameterUnitTests<P extends PersistentProperty<P>> {

	@Mock
	PersistentEntity<Object, P> entity;
	@Mock
	PersistentEntity<String, P> stringEntity;

	TypeInformation<Object> type = ClassTypeInformation.from(Object.class);
	Annotation[] annotations = new Annotation[0];

	@Test
	public void twoParametersWithIdenticalSetupEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>("name", type, annotations, entity);
		Parameter<Object, P> right = new Parameter<Object, P>("name", type, annotations, entity);

		assertThat(left, is(right));
		assertThat(left.hashCode(), is(right.hashCode()));
	}

	@Test
	public void twoParametersWithIdenticalSetupAndNullNameEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>(null, type, annotations, entity);
		Parameter<Object, P> right = new Parameter<Object, P>(null, type, annotations, entity);

		assertThat(left, is(right));
		assertThat(left.hashCode(), is(right.hashCode()));
	}

	@Test
	public void twoParametersWithIdenticalAndNullEntitySetupEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>("name", type, annotations, null);
		Parameter<Object, P> right = new Parameter<Object, P>("name", type, annotations, null);

		assertThat(left, is(right));
		assertThat(left.hashCode(), is(right.hashCode()));
	}

	@Test
	public void twoParametersWithDifferentNameAreNotEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>("first", type, annotations, entity);
		Parameter<Object, P> right = new Parameter<Object, P>("second", type, annotations, entity);

		assertThat(left, is(not(right)));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void twoParametersWithDifferenTypeAreNotEqual() {

		Parameter left = new Parameter<Object, P>("name", type, annotations, entity);
		Parameter right = new Parameter<String, P>("name", ClassTypeInformation.from(String.class), annotations,
				stringEntity);

		assertThat(left, is(not(right)));
	}
}
