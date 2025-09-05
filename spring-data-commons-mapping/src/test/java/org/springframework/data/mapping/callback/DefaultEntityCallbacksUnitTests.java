/*
 * Copyright 2019-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.PersonDocument;
import org.springframework.data.mapping.PersonNoId;
import org.springframework.data.mapping.callback.CapturingEntityCallback.FirstCallback;
import org.springframework.data.mapping.callback.CapturingEntityCallback.SecondCallback;
import org.springframework.data.mapping.callback.CapturingEntityCallback.ThirdCallback;

/**
 * Unit tests for {@link DefaultEntityCallbacks}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class DefaultEntityCallbacksUnitTests {

	@Test // DATACMNS-1467
	void shouldDispatchCallback() {

		var ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		var callbacks = new DefaultEntityCallbacks(ctx);

		var personDocument = new PersonDocument(null, "Walter", null);
		var afterCallback = callbacks.callback(BeforeSaveCallback.class, personDocument);

		assertThat(afterCallback.getSsn()).isEqualTo(6);
	}

	@Test // DATACMNS-1467
	void shouldDispatchCallsToLambdaReceivers() {

		var ctx = new AnnotationConfigApplicationContext(LambdaConfig.class);

		var callbacks = new DefaultEntityCallbacks(ctx);

		var personDocument = new PersonDocument(null, "Walter", null);
		var afterCallback = callbacks.callback(BeforeSaveCallback.class, personDocument);

		assertThat(afterCallback).isSameAs(personDocument);
		assertThat(afterCallback.getSsn()).isEqualTo(6);
	}

	@Test // DATACMNS-1467
	void invokeGenericEvent() {

		var callbacks = new DefaultEntityCallbacks();
		callbacks.addEntityCallback(new GenericPersonCallback());

		Person afterCallback = callbacks.callback(GenericPersonCallback.class,
				new PersonDocument(null, "Walter", null));

		assertThat(afterCallback.getSsn()).isEqualTo(6);
	}

	@Test // DATACMNS-1467
	void invokeGenericEventWithArgs() {

		var callbacks = new DefaultEntityCallbacks();
		callbacks.addEntityCallback(new GenericPersonCallbackWithArgs());

		Person afterCallback = callbacks.callback(GenericPersonCallbackWithArgs.class,
				new PersonDocument(null, "Walter", null), "agr0", Float.POSITIVE_INFINITY);

		assertThat(afterCallback.getSsn()).isEqualTo(6);
	}

	@Test // DATACMNS-1467
	void invokeInvalidEvent() {

		var callbacks = new DefaultEntityCallbacks();
		callbacks.addEntityCallback(new InvalidEntityCallback() {});

		assertThatIllegalStateException().isThrownBy(() -> callbacks.callback(InvalidEntityCallback.class,
				new PersonDocument(null, "Walter", null), "agr0", Float.POSITIVE_INFINITY));
	}

	@Test // DATACMNS-1467
	void passesInvocationResultOnAlongTheChain() {

		CapturingEntityCallback first = new FirstCallback();
		CapturingEntityCallback second = new SecondCallback();

		var callbacks = new DefaultEntityCallbacks();
		callbacks.addEntityCallback(first);
		callbacks.addEntityCallback(second);

		var initial = new PersonDocument(null, "Walter", null);

		callbacks.callback(CapturingEntityCallback.class, initial);

		assertThat(first.capturedValue()).isSameAs(initial);
		assertThat(first.capturedValues()).hasSize(1);
		assertThat(second.capturedValue()).isNotSameAs(initial);
		assertThat(second.capturedValues()).hasSize(1);
	}

	@Test // DATACMNS-1467
	void errorsOnNullEntity() {

		var callbacks = new DefaultEntityCallbacks();
		callbacks.addEntityCallback(new CapturingEntityCallback());

		assertThatIllegalArgumentException().isThrownBy(() -> callbacks.callback(CapturingEntityCallback.class, null));
	}

	@Test // DATACMNS-1467
	void errorsOnNullValueReturnedByCallbackEntity() {

		CapturingEntityCallback first = new FirstCallback();
		CapturingEntityCallback second = new SecondCallback(null);
		CapturingEntityCallback third = new ThirdCallback();

		var callbacks = new DefaultEntityCallbacks();
		callbacks.addEntityCallback(first);
		callbacks.addEntityCallback(second);
		callbacks.addEntityCallback(third);

		var initial = new PersonDocument(null, "Walter", null);

		assertThatIllegalArgumentException()
				.isThrownBy(() -> callbacks.callback(CapturingEntityCallback.class, initial));

		assertThat(first.capturedValue()).isSameAs(initial);
		assertThat(second.capturedValue()).isNotNull().isNotSameAs(initial);
		assertThat(third.capturedValues()).isEmpty();
	}

	@Test // GH-2583
	void skipsInvocationUsingJava18ReflectiveTypeRejection() {

		DefaultEntityCallbacks callbacks = new DefaultEntityCallbacks();
		callbacks.addEntityCallback(new Java18ClassCastStyle());

		Person person = new PersonNoId(42, "Walter", "White");

		Person afterCallback = callbacks.callback(BeforeConvertCallback.class, person);

		assertThat(afterCallback).isSameAs(person);
	}

	@Test // DATACMNS-1467
	void detectsMultipleCallbacksWithinOneClass() {

		var ctx = new AnnotationConfigApplicationContext(MultipleCallbacksInOneClassConfig.class);
		var callbacks = new DefaultEntityCallbacks(ctx);

		var personDocument = new PersonDocument(null, "Walter", null);
		callbacks.callback(BeforeSaveCallback.class, personDocument);

		assertThat(ctx.getBean("callbacks", MultipleCallbacks.class).invocations).containsExactly("save");

		callbacks.callback(BeforeConvertCallback.class, personDocument);

		assertThat(ctx.getBean("callbacks", MultipleCallbacks.class).invocations).containsExactly("save", "convert");
	}

	@Test // GH-2822
	void genericCallbackDiscoveredForObjectDeclaration() {

		var ctx = new AnnotationConfigApplicationContext(SomeConfiguration.class);
		var callbacks = new DefaultEntityCallbacks(ctx);

		callbacks.callback(BeforeSaveCallback.class, new PersonDocument(null, "Walter", null));

		assertThat(ctx.getBean(SomeConfiguration.class).invoked).isTrue();
	}

	@Configuration
	static class MyConfig {

		@Bean
		MyBeforeSaveCallback callback() {
			return new MyBeforeSaveCallback();
		}

		@Bean
		@Lazy
		Object namedCallback() {
			return new MyOtherCallback();
		}
	}

	@Configuration
	static class LambdaConfig {

		@Bean
		BeforeSaveCallback<User> userCallback() {
			return object -> object;
		}

		@Bean
		BeforeSaveCallback<Person> personCallback() {

			return object -> {
				object.setSsn(object.getFirstName().length());
				return object;
			};
		}
	}

	@Configuration
	static class MultipleCallbacksInOneClassConfig {

		@Bean
		MultipleCallbacks callbacks() {
			return new MultipleCallbacks();
		}
	}

	interface BeforeConvertCallback<T> extends EntityCallback<T>, Ordered {
		T onBeforeConvert(T object);

		@Override
		default int getOrder() {
			return 0;
		}
	}

	interface BeforeSaveCallback<T> extends EntityCallback<T> {
		T onBeforeSave(T object);
	}

	static class MyBeforeSaveCallback implements BeforeSaveCallback<Person> {

		@Override
		public Person onBeforeSave(Person object) {

			object.setSsn(object.getFirstName().length());
			return object;
		}
	}

	static class MyOtherCallback implements BeforeSaveCallback<Person> {

		@Override
		public Person onBeforeSave(Person object) {
			return object;
		}
	}

	static class User {}

	static class GenericPersonCallback implements EntityCallback<Person> {

		public Person onBeforeSave(Person value) {

			value.setSsn(value.getFirstName().length());
			return value;
		}
	}

	static class GenericPersonCallbackWithArgs implements EntityCallback<Person> {

		public Person onBeforeSave(Person value, String agr1, Object arg2) {

			value.setSsn(value.getFirstName().length());
			return value;
		}
	}

	interface InvalidEntityCallback extends EntityCallback<Person> {

		default Person onBeforeSave(String value, Person entity) {
			return entity;
		}
	}

	static class MultipleCallbacks implements BeforeConvertCallback<Person>, BeforeSaveCallback<Person> {

		List<String> invocations = new ArrayList(2);

		@Override
		public Person onBeforeConvert(Person object) {

			invocations.add("convert");
			return object;
		}

		@Override
		public Person onBeforeSave(Person object) {

			invocations.add("save");
			return object;
		}
	}

	static class Java18ClassCastStyle implements BeforeConvertCallback<Person> {

		@Override
		public Person onBeforeConvert(Person object) {
			throw new IllegalArgumentException("argument type mismatch");
		}

	}

	// GH-2822
	@Configuration
	static class SomeConfiguration {

		boolean invoked = false;

		@Bean
		BeforeSaveCallback<Object> someGenericCallback() {
			return it -> {
				this.invoked = true;
				return it;
			};
		}
	}
}
