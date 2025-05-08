/*
 * Copyright 2025 the original author or authors.
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

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.TypeInformation;

/**
 * Tests targeting {@link AotQueryMethodGenerationContext}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
class AotQueryMethodGenerationContextUnitTests {

	@Test // GH-3270
	void suggestLocalVariableNameConsidersMethodArguments() throws NoSuchMethodException {

		AotQueryMethodGenerationContext ctx = ctxFor("reservedParameterMethod");

		assertThat(ctx.localVariable("foo")).isEqualTo("foo");
		assertThat(ctx.localVariable("arg0")).isNotIn("arg0", "arg1", "arg2");
	}

	AotQueryMethodGenerationContext ctxFor(String methodName) throws NoSuchMethodException {

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

		return new AotQueryMethodGenerationContext(ri, target, Mockito.mock(QueryMethod.class),
				Mockito.mock(AotRepositoryFragmentMetadata.class));
	}

	private interface DummyRepo {
		String reservedParameterMethod(Object arg0, Pageable arg1, Object arg2);
	}
}
