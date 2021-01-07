/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.mapping;

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.mapping.PreferredConstructor.Parameter;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * Unit tests for {@link Parameter}.
 *
 * @author Oliver Gierke
 */
@ExtendWith(MockitoExtension.class)
class ParameterUnitTests<P extends PersistentProperty<P>> {

	@Mock PersistentEntity<Object, P> entity;
	@Mock PersistentEntity<String, P> stringEntity;

	private TypeInformation<Object> type = ClassTypeInformation.from(Object.class);
	private Annotation[] annotations = new Annotation[0];

	@Test
	void twoParametersWithIdenticalSetupEqual() {

		Parameter<Object, P> left = new Parameter<>("name", type, annotations, entity);
		Parameter<Object, P> right = new Parameter<>("name", type, annotations, entity);

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	void twoParametersWithIdenticalSetupAndNullNameEqual() {

		Parameter<Object, P> left = new Parameter<>(null, type, annotations, entity);
		Parameter<Object, P> right = new Parameter<>(null, type, annotations, entity);

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	void twoParametersWithIdenticalAndNullEntitySetupEqual() {

		Parameter<Object, P> left = new Parameter<>("name", type, annotations, null);
		Parameter<Object, P> right = new Parameter<>("name", type, annotations, null);

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	void twoParametersWithDifferentNameAreNotEqual() {

		Parameter<Object, P> left = new Parameter<>("first", type, annotations, entity);
		Parameter<Object, P> right = new Parameter<>("second", type, annotations, entity);

		assertThat(left).isNotEqualTo(right);
	}

	@Test
	void twoParametersWithDifferenTypeAreNotEqual() {

		Parameter<Object, P> left = new Parameter<>("name", type, annotations, entity);
		Parameter<String, P> right = new Parameter<>("name", ClassTypeInformation.from(String.class), annotations,
				stringEntity);

		assertThat(left).isNotEqualTo(right);
	}
}
