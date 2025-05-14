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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.query.DefaultParameters;
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

	@Test // GH-3279
	void returnsCorrectParameterNames() throws NoSuchMethodException {

		AotQueryMethodGenerationContext ctx = ctxFor("limitScrollPositionDynamicProjection");

		assertThat(ctx.getLimitParameterName()).isEqualTo("l");
		assertThat(ctx.getPageableParameterName()).isNull();
		assertThat(ctx.getScrollPositionParameterName()).isEqualTo("sp");
		assertThat(ctx.getDynamicProjectionParameterName()).isEqualTo("projection");
	}

	@Test // GH-3279
	void returnsCorrectParameterNameForPageable() throws NoSuchMethodException {

		AotQueryMethodGenerationContext ctx = ctxFor("pageable");

		assertThat(ctx.getLimitParameterName()).isNull();
		assertThat(ctx.getPageableParameterName()).isEqualTo("p");
		assertThat(ctx.getScrollPositionParameterName()).isNull();
		assertThat(ctx.getDynamicProjectionParameterName()).isNull();
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
		Mockito.doReturn(TypeInformation.of(String.class)).when(ri).getReturnType(eq(target));
		Mockito.doReturn(TypeInformation.of(String.class)).when(ri).getReturnedDomainTypeInformation(eq(target));

		return new AotQueryMethodGenerationContext(ri, target,
				new QueryMethod(target, AbstractRepositoryMetadata.getMetadata(DummyRepo.class),
						new SpelAwareProxyProjectionFactory(), DefaultParameters::new),
				Mockito.mock(AotRepositoryFragmentMetadata.class));
	}

	private interface DummyRepo extends Repository<String, Long> {

		Page<String> reservedParameterMethod(Object arg0, Pageable arg1, Object arg2);

		<T> Window<T> limitScrollPositionDynamicProjection(Limit l, ScrollPosition sp, Class<T> projection);

		Page<String> pageable(Pageable p);
	}
}
