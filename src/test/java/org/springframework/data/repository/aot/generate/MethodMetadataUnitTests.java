/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.repository.aot.generate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.core.TypeInformation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryInformation;

/**
 * Unit tests for {@link MethodMetadata}.
 *
 * @author Christoph Strobl
 */
class MethodMetadataUnitTests {

	@Test // GH-3270
	void getParameterNameByIndex() throws NoSuchMethodException {

		MethodMetadata metadata = methodMetadataFor("threeArgsMethod");

		assertThat(metadata.getParameterName(0)).isEqualTo("arg0");
		assertThat(metadata.getParameterName(1)).isEqualTo("arg1");
		assertThat(metadata.getParameterName(2)).isEqualTo("arg2");
	}

	@Test // GH-3270
	void getParameterNameByNonExistingIndex() throws NoSuchMethodException {

		MethodMetadata metadata = methodMetadataFor("threeArgsMethod");

		assertThat(metadata.getParameterName(-1)).isNull();
		assertThat(metadata.getParameterName(3)).isNull();
	}

	@Test // GH-3270
	void getParameterNameForNoArgsMethod() throws NoSuchMethodException {
		assertThat(methodMetadataFor("noArgsMethod").getParameterName(0)).isNull();
	}

	static MethodMetadata methodMetadataFor(String methodName) throws NoSuchMethodException {

		Method target = null;
		for (Method m : DummyRepo.class.getMethods()) {
			if (m.getName().equals(methodName)) {
				target = m;
				break;
			}
		}

		if (target == null) {
			throw new NoSuchMethodException(methodName);
		}

		RepositoryInformation ri = Mockito.mock(RepositoryInformation.class);
		Mockito.doReturn(TypeInformation.of(target.getReturnType())).when(ri).getReturnType(eq(target));
		Mockito.doReturn(TypeInformation.of(target.getReturnType())).when(ri).getReturnedDomainTypeInformation(eq(target));
		return new MethodMetadata(ri, target);
	}

	private interface DummyRepo {

		String noArgsMethod();

		String threeArgsMethod(Object arg0, Pageable arg1, Object arg2);
	}
}
