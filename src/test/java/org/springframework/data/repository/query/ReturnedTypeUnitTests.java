/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
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
 */
public class ReturnedTypeUnitTests {

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void treatsSimpleDomainTypeAsIs() throws Exception {

		ReturnedType type = getReturnedType("findAll");

		assertThat(type.getTypeToRead()).isEqualTo(Sample.class);
		assertThat(type.getInputProperties()).isEmpty();
		assertThat(type.isProjecting()).isFalse();
		assertThat(type.needsCustomConstruction()).isFalse();
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void detectsDto() throws Exception {

		ReturnedType type = getReturnedType("findAllDtos");

		assertThat(type.getTypeToRead()).isEqualTo(SampleDto.class);
		assertThat(type.getInputProperties()).contains("firstname");
		assertThat(type.isInstance(new SampleDto("firstname"))).isTrue();
		assertThat(type.isProjecting()).isTrue();
		assertThat(type.needsCustomConstruction()).isTrue();
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void detectsProjection() throws Exception {

		ReturnedType type = getReturnedType("findAllProjection");

		assertThat(type.getTypeToRead()).isNull();
		assertThat(type.getInputProperties()).contains("lastname");
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void detectsVoidMethod() throws Exception {

		ReturnedType type = getReturnedType("voidMethod");

		assertThat(type.getDomainType()).isEqualTo(Sample.class);
		assertThat(type.getReturnedType()).isEqualTo(void.class);
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void detectsClosedProjection() throws Exception {

		ReturnedType type = getReturnedType("findOneProjection");

		assertThat(type.getReturnedType()).isEqualTo(SampleProjection.class);
		assertThat(type.isProjecting()).isTrue();
		assertThat(type.needsCustomConstruction()).isTrue();
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void detectsOpenProjection() throws Exception {

		ReturnedType type = getReturnedType("findOneOpenProjection");

		assertThat(type.getReturnedType()).isEqualTo(OpenProjection.class);
		assertThat(type.isProjecting()).isTrue();
		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.getTypeToRead()).isEqualTo(Sample.class);
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void detectsComplexNumberTypes() throws Exception {

		ReturnedType type = getReturnedType("countQuery");

		assertThat(type.isProjecting()).isFalse();
		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.getTypeToRead()).isEqualTo(BigInteger.class);
	}

	/**
	 * @see DATACMNS-840
	 */
	@Test
	public void detectsSampleDtoWithDefaultConstructor() throws Exception {

		ReturnedType type = getReturnedType("dtoWithMultipleConstructors");

		assertThat(type.getInputProperties()).isEmpty();
		assertThat(type.needsCustomConstruction()).isFalse();
	}

	/**
	 * @see DATACMNS-840
	 */
	@Test
	public void doesNotConsiderAnEnumProjecting() throws Exception {

		ReturnedType type = getReturnedType("findEnum");

		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.isProjecting()).isFalse();
	}

	/**
	 * @see DATACMNS-850
	 */
	@Test
	public void considersAllJavaTypesAsNotProjecting() throws Exception {

		ReturnedType type = getReturnedType("timeQuery");

		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.isProjecting()).isFalse();
	}

	/**
	 * @see DATACMNS-862
	 */
	@Test
	public void considersInterfaceImplementedByDomainTypeNotProjecting() throws Exception {

		ReturnedType type = getReturnedType("findOneInterface");

		assertThat(type.needsCustomConstruction()).isFalse();
		assertThat(type.isProjecting()).isFalse();
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
		public String firstname, lastname;

		public Sample(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}
	}

	static class SampleDto {

		public SampleDto(String firstname) {

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
}
