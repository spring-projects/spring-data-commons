/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.auditing;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Auditor;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 */
class ReactiveAuditorAwareUnitTests {

	@Test // DATACMNS-1231
	void getAuditorGetsAuditorNoneWhenNoAuditorAwareNotPresent() {

		ReactiveAuditorAwareStub.of("batman").getAuditor() //
				.as(StepVerifier::create) //
				.expectNext(Auditor.of("batman")) //
				.verifyComplete();
	}

	@Test // DATACMNS-1231
	void getAuditorShouldReturnNoneIfAuditorAwareDoesNotEmitObject() {

		ReactiveAuditorAwareStub.of(null).getAuditor() //
				.as(StepVerifier::create) //
				.expectNext(Auditor.none()) //
				.verifyComplete();
	}

	static class ReactiveAuditorAwareStub<T> implements ReactiveAuditorAware<T> {

		@Nullable T value;

		private ReactiveAuditorAwareStub(@Nullable T value) {
			this.value = value;
		}

		static <T> ReactiveAuditorAware<T> of(T value) {
			return new ReactiveAuditorAwareStub<>(value);
		}

		@Override
		public Mono<T> getCurrentAuditor() {
			return Mono.justOrEmpty(value);
		}
	}
}
