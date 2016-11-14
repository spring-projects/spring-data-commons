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

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.util.Optional;

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

	@Mock PersistentEntity<Object, P> entity;
	@Mock PersistentEntity<String, P> stringEntity;

	TypeInformation<Object> type = ClassTypeInformation.from(Object.class);
	Annotation[] annotations = new Annotation[0];

	@Test
	public void twoParametersWithIdenticalSetupEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>(Optional.of("name"), type, annotations, Optional.of(entity));
		Parameter<Object, P> right = new Parameter<Object, P>(Optional.of("name"), type, annotations, Optional.of(entity));

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	public void twoParametersWithIdenticalSetupAndNullNameEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>(Optional.empty(), type, annotations, Optional.of(entity));
		Parameter<Object, P> right = new Parameter<Object, P>(Optional.empty(), type, annotations, Optional.of(entity));

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	public void twoParametersWithIdenticalAndNullEntitySetupEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>(Optional.of("name"), type, annotations, Optional.empty());
		Parameter<Object, P> right = new Parameter<Object, P>(Optional.of("name"), type, annotations, Optional.empty());

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	public void twoParametersWithDifferentNameAreNotEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>(Optional.of("first"), type, annotations, Optional.of(entity));
		Parameter<Object, P> right = new Parameter<Object, P>(Optional.of("second"), type, annotations,
				Optional.of(entity));

		assertThat(left).isNotEqualTo(right);
	}

	@Test
	public void twoParametersWithDifferenTypeAreNotEqual() {

		Parameter<Object, P> left = new Parameter<Object, P>(Optional.of("name"), type, annotations, Optional.of(entity));
		Parameter<String, P> right = new Parameter<String, P>(Optional.of("name"), ClassTypeInformation.from(String.class),
				annotations, Optional.of(stringEntity));

		assertThat(left).isNotEqualTo(right);
	}
}
