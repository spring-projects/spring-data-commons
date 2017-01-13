/*
 * Copyright 2015-2017 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

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

	@Test // DATACMNS-89
	public void treatsSimpleDomainTypeAsIs() throws Exception {

		ReturnedType type = getReturnedType("findAll");

		assertThat(type.getTypeToRead(), is(typeCompatibleWith(Sample.class)));
		assertThat(type.getInputProperties(), is(empty()));
		assertThat(type.isProjecting(), is(false));
		assertThat(type.needsCustomConstruction(), is(false));
	}

	@Test // DATACMNS-89
	public void detectsDto() throws Exception {

		ReturnedType type = getReturnedType("findAllDtos");

		assertThat(type.getTypeToRead(), is(typeCompatibleWith(SampleDto.class)));
		assertThat(type.getInputProperties(), contains("firstname"));
		assertThat(type.isInstance(new SampleDto("firstname")), is(true));
		assertThat(type.isProjecting(), is(true));
		assertThat(type.needsCustomConstruction(), is(true));
	}

	@Test // DATACMNS-89
	public void detectsProjection() throws Exception {

		ReturnedType type = getReturnedType("findAllProjection");

		assertThat(type.getTypeToRead(), is(nullValue()));
		assertThat(type.getInputProperties(), contains("lastname"));
	}

	@Test // DATACMNS-89
	public void detectsVoidMethod() throws Exception {

		ReturnedType type = getReturnedType("voidMethod");

		assertThat(type.getDomainType(), is(typeCompatibleWith(Sample.class)));
		assertThat(type.getReturnedType(), is(typeCompatibleWith(void.class)));
	}

	@Test // DATACMNS-89
	public void detectsClosedProjection() throws Exception {

		ReturnedType type = getReturnedType("findOneProjection");

		assertThat(type.getReturnedType(), is(typeCompatibleWith(SampleProjection.class)));
		assertThat(type.isProjecting(), is(true));
		assertThat(type.needsCustomConstruction(), is(true));
	}

	@Test // DATACMNS-89
	public void detectsOpenProjection() throws Exception {

		ReturnedType type = getReturnedType("findOneOpenProjection");

		assertThat(type.getReturnedType(), is(typeCompatibleWith(OpenProjection.class)));
		assertThat(type.isProjecting(), is(true));
		assertThat(type.needsCustomConstruction(), is(false));
		assertThat(type.getTypeToRead(), is(typeCompatibleWith(Sample.class)));
	}

	@Test // DATACMNS-89
	public void detectsComplexNumberTypes() throws Exception {

		ReturnedType type = getReturnedType("countQuery");

		assertThat(type.isProjecting(), is(false));
		assertThat(type.needsCustomConstruction(), is(false));
		assertThat(type.getTypeToRead(), is(typeCompatibleWith(BigInteger.class)));
	}

	@Test // DATACMNS-840
	public void detectsSampleDtoWithDefaultConstructor() throws Exception {

		ReturnedType type = getReturnedType("dtoWithMultipleConstructors");

		assertThat(type.getInputProperties(), is(empty()));
		assertThat(type.needsCustomConstruction(), is(false));
	}

	@Test // DATACMNS-840
	public void doesNotConsiderAnEnumProjecting() throws Exception {

		ReturnedType type = getReturnedType("findEnum");

		assertThat(type.needsCustomConstruction(), is(false));
		assertThat(type.isProjecting(), is(false));
	}

	@Test // DATACMNS-850
	public void considersAllJavaTypesAsNotProjecting() throws Exception {

		ReturnedType type = getReturnedType("timeQuery");

		assertThat(type.needsCustomConstruction(), is(false));
		assertThat(type.isProjecting(), is(false));
	}

	@Test // DATACMNS-862
	public void considersInterfaceImplementedByDomainTypeNotProjecting() throws Exception {

		ReturnedType type = getReturnedType("findOneInterface");

		assertThat(type.needsCustomConstruction(), is(false));
		assertThat(type.isProjecting(), is(false));
	}

	@Test // DATACMNS-963
	public void detectsDistinctInputProperties() {

		ReturnedType type = ReturnedType.of(Child.class, Object.class, new SpelAwareProxyProjectionFactory());

		List<String> properties = type.getInputProperties();

		assertThat(properties, hasSize(1));
		assertThat(properties, contains("firstname"));
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

	static interface Parent {
		String getFirstname();
	}

	static interface Child extends Parent {
		String getFirstname();
	}
}
