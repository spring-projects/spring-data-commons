/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * Unit tests for {@link ReturnedType}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
class ReturnedTypeUnitTests {

	@Test // DATACMNS-89
	void treatsSimpleDomainTypeAsIs() throws Exception {

		ReturnedType type = getReturnedType("findAll");

		assertThat(type.getTypeToRead()).isEqualTo(Sample.class);
		assertThat(type.getInputProperties()).isEmpty();
		assertThat(type.isProjecting()).isFalse();
		assertThat(type.needsCustomConstruction()).isFalse();
	}

	@Test // DATACMNS-89
	void detectsDto() throws Exception {

		ReturnedType type = getReturnedType("findAllDtos");

		assertThat(type.getTypeToRead()).isEqualTo(SampleDto.class);
		assertThat(type.getInputProperties()).contains("firstname");
		assertThat(type.isInstance(new SampleDto("firstname"))).isTrue();
		assertThat(type.isProjecting()).isTrue();
		assertThat(type.needsCustomConstruction()).isTrue();
	}

	@Test // DATACMNS-89
	void detectsProjection() throws Exception {

		ReturnedType type = getReturnedType("findAllProjection");

		assertThat(type.getTypeToRead()).isNull();
		assertThat(type.getInputProperties()).contains("lastname");
	}

	@Test // DATACMNS-89
	void detectsVoidMethod() throws Exception {

		ReturnedType type = getReturnedType("voidMethod");

		assertThat(type.getDomainType()).isEqualTo(Sample.class);
		assertThat(type.getReturnedType()).isEqualTo(void.class);
	}

	@Test // DATACMNS-89
	void detectsClosedProjection() throws Exception {

		ReturnedType type = getReturnedType("findOneProjection");

		assertThat(type.getReturnedType()).isEqualTo(SampleProjection.class);
		assertThat(type.isProjecting()).isTrue();
		assertThat(type.needsCustomConstruction()).isTrue();
	}

	@Test // DATACMNS-89
	void detectsOpenProjection() throws Exception {

		ReturnedType type = getReturnedType("findOneOpenProjection");

		assertThat(type.getReturnedType()).isEqualTo(OpenProjection.class);
		assertThat(type.isProjecting()).isTrue();
		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.getTypeToRead()).isEqualTo(Sample.class);
	}

	@Test // DATACMNS-89
	void detectsComplexNumberTypes() throws Exception {

		ReturnedType type = getReturnedType("countQuery");

		assertThat(type.isProjecting()).isFalse();
		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.getTypeToRead()).isEqualTo(BigInteger.class);
	}

	@Test // DATACMNS-840
	void detectsSampleDtoWithDefaultConstructor() throws Exception {

		ReturnedType type = getReturnedType("dtoWithMultipleConstructors");

		assertThat(type.getInputProperties()).isEmpty();
		assertThat(type.needsCustomConstruction()).isFalse();
	}

	@Test // DATACMNS-840
	void doesNotConsiderAnEnumProjecting() throws Exception {

		ReturnedType type = getReturnedType("findEnum");

		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.isProjecting()).isFalse();
	}

	@Test // DATACMNS-850
	void considersAllJavaTypesAsNotProjecting() throws Exception {

		ReturnedType type = getReturnedType("timeQuery");

		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.isProjecting()).isFalse();
	}

	@Test // DATACMNS-862
	void considersInterfaceImplementedByDomainTypeNotProjecting() throws Exception {

		ReturnedType type = getReturnedType("findOneInterface");

		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.isProjecting()).isFalse();
	}

	@Test // DATACMNS-963
	void detectsDistinctInputProperties() {

		ReturnedType type = ReturnedType.of(Child.class, Object.class, new SpelAwareProxyProjectionFactory());

		List<String> properties = type.getInputProperties();

		assertThat(properties).hasSize(1);
		assertThat(properties).containsExactly("firstname");
	}

	@Test // DATACMNS-1112
	void cachesInstancesBySourceTypes() {

		SpelAwareProxyProjectionFactory factory = new SpelAwareProxyProjectionFactory();

		ReturnedType left = ReturnedType.of(Child.class, Object.class, factory);
		ReturnedType right = ReturnedType.of(Child.class, Object.class, factory);

		assertThat(left).isSameAs(right);
	}

	private static ReturnedType getReturnedType(String methodName, Class<?>... parameters) throws Exception {
		return getQueryMethod(methodName, parameters).getResultProcessor().getReturnedType();
	}

	private static QueryMethod getQueryMethod(String name, Class<?>... parameters) throws Exception {

		Method method = SampleRepository.class.getMethod(name, parameters);
		return new QueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory());
	}

	interface SampleRepository extends Repository<Sample, Long> {

		void voidMethod();

		List<Sample> findAll();

		List<SampleDto> findAllDtos();

		List<SampleProjection> findAllProjection();

		Sample findOne();

		SampleDto findOneDto();

		SampleProjection findOneProjection();

		OpenProjection findOneOpenProjection();

		Page<SampleProjection> findPageProjection(Pageable pageable);

		BigInteger countQuery();

		SampleDtoWithMultipleConstructors dtoWithMultipleConstructors();

		MyEnum findEnum();

		LocalDateTime timeQuery();

		SampleInterface findOneInterface();

		static enum MyEnum {
			VALUE
		}
	}

	static interface SampleInterface {}

	static class Sample implements SampleInterface {
		String firstname, lastname;

		Sample(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}
	}

	static class SampleDto {

		SampleDto(String firstname) {

		}
	}

	static class SampleDtoWithMultipleConstructors {

		SampleDtoWithMultipleConstructors(String firstname) {}

		SampleDtoWithMultipleConstructors(int age) {}
	}

	interface SampleProjection {

		String getLastname();
	}

	interface OpenProjection {

		String getLastname();

		@Value("#{target.firstname + ' ' + target.lastname}")
		String getFullName();
	}

	static interface Parent {
		String getFirstname();
	}

	static interface Child extends Parent {
		String getFirstname();
	}
}
