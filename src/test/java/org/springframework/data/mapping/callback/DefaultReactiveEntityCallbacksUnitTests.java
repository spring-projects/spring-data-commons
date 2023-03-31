/*
 * Copyright 2019-2023 the original author or authors.
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
package org.springframework.data.mapping.callback;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.PersonDocument;
import org.springframework.data.mapping.PersonNoId;
import org.springframework.data.mapping.callback.CapturingEntityCallback.FirstCallback;
import org.springframework.data.mapping.callback.CapturingEntityCallback.SecondCallback;
import org.springframework.data.mapping.callback.CapturingEntityCallback.ThirdCallback;

/**
 * Unit tests for {@link DefaultReactiveEntityCallbacks}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Michael J. Simons
 */
class DefaultReactiveEntityCallbacksUnitTests {

	@Test // DATACMNS-1467
	void dispatchResolvesOnSubscribe() {

		var ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		var callbacks = new DefaultReactiveEntityCallbacks(ctx);

		var personDocument = new PersonDocument(null, "Walter", null);
		var afterCallback = callbacks.callback(ReactiveBeforeSaveCallback.class, personDocument);

		assertThat(personDocument.getSsn()).isNull();

		afterCallback.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getSsn()).isEqualTo(6)) //
				.verifyComplete();
	}

	@Test // DATACMNS-1467
	void invokeGenericEvent() {

		var callbacks = new DefaultReactiveEntityCallbacks();
		callbacks.addEntityCallback(new GenericPersonCallback());

		callbacks.callback(GenericPersonCallback.class, new PersonDocument(null, "Walter", null)) //
				.as(StepVerifier::create) //
				.consumeNextWith(it -> assertThat(it.getSsn()).isEqualTo(6)) //
				.verifyComplete();
	}

	@Test // DATACMNS-1467
	void passesInvocationResultOnAlongTheChain() {

		CapturingEntityCallback first = new FirstCallback();
		CapturingEntityCallback second = new SecondCallback();

		var callbacks = new DefaultReactiveEntityCallbacks();
		callbacks.addEntityCallback(first);
		callbacks.addEntityCallback(second);

		var initial = new PersonDocument(null, "Walter", null);

		callbacks.callback(CapturingEntityCallback.class, initial) //
				.as(StepVerifier::create) //
				.expectNextCount(1) //
				.verifyComplete();

		assertThat(first.capturedValue()).isSameAs(initial);
		assertThat(first.capturedValues()).hasSize(1);
		assertThat(second.capturedValue()).isNotSameAs(initial);
		assertThat(second.capturedValues()).hasSize(1);
	}

	@Test // DATACMNS-1467
	void errorsOnNullEntity() {

		var callbacks = new DefaultReactiveEntityCallbacks();
		callbacks.addEntityCallback(new CapturingEntityCallback());

		assertThatIllegalArgumentException()
				.isThrownBy(() -> callbacks.callback(CapturingEntityCallback.class, null));
	}

	@Test // DATACMNS-1467
	void errorsOnNullValueReturnedByCallbackEntity() {

		CapturingEntityCallback first = new FirstCallback();
		CapturingEntityCallback second = new SecondCallback(null);
		CapturingEntityCallback third = new ThirdCallback();

		var callbacks = new DefaultReactiveEntityCallbacks();
		callbacks.addEntityCallback(first);
		callbacks.addEntityCallback(second);
		callbacks.addEntityCallback(third);

		var initial = new PersonDocument(null, "Walter", null);

		callbacks.callback(CapturingEntityCallback.class, initial) //
				.as(StepVerifier::create) //
				.expectError(IllegalArgumentException.class) //
				.verify();

		assertThat(first.capturedValue()).isSameAs(initial);
		assertThat(second.capturedValue()).isNotNull().isNotSameAs(initial);
		assertThat(third.capturedValues()).isEmpty();
	}

	@Test // GH-2808
	void skipsInvocationUsingJava18ReflectiveTypeRejection() {

		DefaultReactiveEntityCallbacks callbacks = new DefaultReactiveEntityCallbacks();
		callbacks.addEntityCallback(new DefaultEntityCallbacksUnitTests.Java18ClassCastStyle());

		Person person = new PersonNoId(42, "Walter", "White");
		List<Person> capturedPerson = new ArrayList<>();

		callbacks.callback(DefaultEntityCallbacksUnitTests.BeforeConvertCallback.class, person) //
			.as(StepVerifier::create)
			.recordWith(() -> capturedPerson)
			.expectNextCount(1)
			.verifyComplete();

		assertThat(capturedPerson.get(0)).isSameAs(person);
	}

	@Configuration
	static class MyConfig {

		@Bean
		MyReactiveBeforeSaveCallback callback() {
			return new MyReactiveBeforeSaveCallback();
		}

	}

	interface ReactiveBeforeSaveCallback<T> extends EntityCallback<T> {
		Mono<T> onBeforeSave(T object);
	}

	static class MyReactiveBeforeSaveCallback implements ReactiveBeforeSaveCallback<Person> {

		@Override
		public Mono<Person> onBeforeSave(Person object) {

			var result = new PersonDocument(object.getFirstName().length(), object.getFirstName(),
					object.getLastName());

			return Mono.just(result);
		}
	}

	static class GenericPersonCallback implements EntityCallback<Person> {

		public Person onBeforeSave(Person value) {

			value.setSsn(value.getFirstName().length());
			return value;
		}
	}
}
