/*
 * Copyright 2012-present the original author or authors.
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
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.ParameterUnitTests.IFace.ClassMember;
import org.springframework.data.mapping.ParameterUnitTests.IFace.RecordMember;
import org.springframework.data.mapping.ParameterUnitTests.StaticType.NonStaticInner;
import org.springframework.data.mapping.ParameterUnitTests.StaticType.RecordInner;
import org.springframework.data.mapping.ParameterUnitTests.StaticType.StaticInner;

/**
 * Unit tests for {@link Parameter}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Chris Bono
 */
@ExtendWith(MockitoExtension.class)
class ParameterUnitTests<P extends PersistentProperty<P>> {

	@Mock PersistentEntity<Object, P> entity;
	@Mock PersistentEntity<String, P> stringEntity;

	private TypeInformation<Object> type = TypeInformation.of(Object.class);
	private Annotation[] annotations = new Annotation[0];

	@Test
	void twoParametersWithIdenticalSetupEqual() {

		var left = new Parameter<Object, P>("name", type, annotations, entity);
		var right = new Parameter<Object, P>("name", type, annotations, entity);

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	void twoParametersWithIdenticalSetupAndNullNameEqual() {

		var left = new Parameter<Object, P>(null, type, annotations, entity);
		var right = new Parameter<Object, P>(null, type, annotations, entity);

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	void twoParametersWithIdenticalAndNullEntitySetupEqual() {

		var left = new Parameter<Object, P>("name", type, annotations, null);
		var right = new Parameter<Object, P>("name", type, annotations, null);

		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	void twoParametersWithDifferentNameAreNotEqual() {

		var left = new Parameter<Object, P>("first", type, annotations, entity);
		var right = new Parameter<Object, P>("second", type, annotations, entity);

		assertThat(left).isNotEqualTo(right);
	}

	@Test
	void twoParametersWithDifferenTypeAreNotEqual() {

		var left = new Parameter<Object, P>("name", type, annotations, entity);
		var right = new Parameter<String, P>("name", TypeInformation.of(String.class), annotations,
				stringEntity);

		assertThat(left).isNotEqualTo(right);
	}

	@Test // GH-3038
	void shouldNotConsiderRecordTypeOfInterfaceEnclosingClassParameter() {

		PersistentEntity pe = Mockito.mock(PersistentEntity.class);
		when(pe.getType()).thenReturn(RecordMember.class);

		Parameter<IFace, P> iFace = new Parameter<IFace, P>("iFace", TypeInformation.of(IFace.class), annotations, pe);
		assertThat(iFace.isEnclosingClassParameter()).isFalse();
	}

	@Test // GH-3038
	void shouldNotConsiderMemberTypeOfInterfaceEnclosingClassParameter() {

		PersistentEntity pe = Mockito.mock(PersistentEntity.class);
		when(pe.getType()).thenReturn(ClassMember.class);

		Parameter<IFace, P> iFace = new Parameter<IFace, P>("iFace", TypeInformation.of(IFace.class), annotations, pe);
		assertThat(iFace.isEnclosingClassParameter()).isFalse();
	}

	@Test // GH-3038
	void shouldConsiderMemberTypeOfClassEnclosingClassParameter() {

		PersistentEntity pe = Mockito.mock(PersistentEntity.class);
		when(pe.getType()).thenReturn(NonStaticInner.class);

		Parameter<StaticType, P> iFace = new Parameter<StaticType, P>("outer", TypeInformation.of(StaticType.class),
				annotations, pe);
		assertThat(iFace.isEnclosingClassParameter()).isTrue();
	}

	@Test // GH-3038
	void shouldNotConsiderStaticMemberTypeOfClassEnclosingClassParameter() {

		PersistentEntity pe = Mockito.mock(PersistentEntity.class);
		when(pe.getType()).thenReturn(StaticInner.class);

		Parameter<StaticType, P> iFace = new Parameter<StaticType, P>("outer", TypeInformation.of(StaticType.class),
				annotations, pe);
		assertThat(iFace.isEnclosingClassParameter()).isFalse();
	}

	@Test // GH-3038
	void shouldNotConsiderRecordMemberTypeOfClassEnclosingClassParameter() {

		PersistentEntity pe = Mockito.mock(PersistentEntity.class);
		when(pe.getType()).thenReturn(RecordInner.class);

		Parameter<StaticType, P> iFace = new Parameter<StaticType, P>("outer", TypeInformation.of(StaticType.class),
				annotations, pe);
		assertThat(iFace.isEnclosingClassParameter()).isFalse();
	}

	@Test // GH-3088
	void getRequiredNameDoesNotThrowExceptionWhenHasName() {

		var parameter = new Parameter<>("someName", type, annotations, entity);
		assertThat(parameter.getRequiredName()).isEqualTo("someName");
	}

	@Test // GH-3088
	void getRequiredNameThrowsExceptionWhenHasNoName() {

		var parameter = new Parameter<>(null, type, annotations, entity);
		assertThatIllegalStateException().isThrownBy(() -> parameter.getRequiredName())
				.withMessage("No name associated with this parameter");
	}

	@Test // GH-3088
	void hasNameReturnsTrueWhenHasName() {

		var parameter = new Parameter<>("someName", type, annotations, entity);
		assertThat(parameter.hasName()).isTrue();
	}

	@Test // GH-3088
	void hasNameReturnsFalseWhenHasNoName() {

		var parameter = new Parameter<>(null, type, annotations, entity);
		assertThat(parameter.hasName()).isFalse();
	}

	interface IFace {

		record RecordMember(IFace iFace) {
		}

		class ClassMember {
			ClassMember(IFace iface) {}
		}
	}

	static class StaticType {

		class NonStaticInner {
			NonStaticInner() {}
		}

		static class StaticInner {
			StaticInner(StaticType outer) {}
		}

		record RecordInner(StaticType outer) {
		}
	}
}
