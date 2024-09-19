/*
 * Copyright 2013-2024 the original author or authors.
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
package org.springframework.data.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OptionalPageableHandlerMethodArgumentResolver}.
 *
 * @author Yanming Zhou
 */
class OptionalPageableHandlerMethodArgumentResolverUnitTests {

	@Test
	void optionalWrapperOfPageable() throws Exception {

		OptionalPageableHandlerMethodArgumentResolver resolver = new OptionalPageableHandlerMethodArgumentResolver(new PageableHandlerMethodArgumentResolver());

		MethodParameter methodParameter = new MethodParameter(Sample.class.getMethod("supportedMethod", Optional.class), 0);
		var request = new MockHttpServletRequest();
		request.addParameter("page", "0");
		request.addParameter("size", "23");

		Optional<Pageable> pageable = resolver.resolveArgument(methodParameter, null, new ServletWebRequest(request), null);
		assertThat(pageable).isPresent().get().extracting(Pageable::getPageSize).isEqualTo(23);
	}

	interface Sample {

		void supportedMethod(Optional<Pageable> pageable);

	}
}
