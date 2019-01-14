/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.mapping.callback;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ResolvableType;
import org.springframework.data.mapping.Child;
import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.PersonDocument;

/**
 * Unit tests for {@link SimpleEntityCallbacks}.
 *
 * @author Mark Paluch
 */
public class SimpleEntityCallbacksUnitTests {

	@Test
	public void shouldDispatchCallback() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		SimpleEntityCallbacks callbacks = new SimpleEntityCallbacks(ctx);

		PersonDocument personDocument = new PersonDocument(null, "Walter", null);
		PersonDocument afterCallback = callbacks.callback(personDocument, BeforeSaveCallback.class,
				BeforeSaveCallback::onBeforeSave);

		assertThat(afterCallback.getSsn()).isEqualTo(6);
	}

	@Test
	public void shouldDispatchCallsToLambdaReceivers() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(LambdaConfig.class);

		SimpleEntityCallbacks callbacks = new SimpleEntityCallbacks(ctx);

		PersonDocument personDocument = new PersonDocument(null, "Walter", null);
		PersonDocument afterCallback = callbacks.callback(personDocument, BeforeSaveCallback.class,
				BeforeSaveCallback::onBeforeSave);

		assertThat(afterCallback.getSsn()).isEqualTo(6);
	}

	@Test
	public void shouldDiscoverCallbackType() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		SimpleEntityCallbacks callbacks = new SimpleEntityCallbacks(ctx);

		Collection<EntityCallback<?>> entityCallbacks = callbacks.getEntityCallbacks(new PersonDocument(null, null, null),
				ResolvableType.forType(BeforeSaveCallback.class));

		assertThat(entityCallbacks).hasSize(1).element(0).isInstanceOf(MyBeforeSaveCallback.class);
	}

	@Test
	public void shouldDiscoverCallbackTypeByName() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		SimpleEntityCallbacks callbacks = new SimpleEntityCallbacks(ctx);
		callbacks.removeAllCallbacks();
		callbacks.addEntityCallbackBean("namedCallback");

		Collection<EntityCallback<?>> entityCallbacks = callbacks.getEntityCallbacks(new PersonDocument(null, null, null),
				ResolvableType.forType(BeforeSaveCallback.class));

		assertThat(entityCallbacks).hasSize(1).element(0).isInstanceOf(MyOtherCallback.class);
	}

	@Test
	public void shouldSupportCallbackTypes() {

		SimpleEntityCallbacks callbacks = new SimpleEntityCallbacks();

		assertThat(callbacks.supportsEvent(MyBeforeSaveCallback.class, ResolvableType.forClass(Person.class))).isTrue();
		assertThat(callbacks.supportsEvent(MyBeforeSaveCallback.class, ResolvableType.forClass(Child.class))).isTrue();
		assertThat(callbacks.supportsEvent(BeforeSaveCallback.class, ResolvableType.forClass(PersonDocument.class)))
				.isTrue();

		assertThat(callbacks.supportsEvent(MyBeforeSaveCallback.class, ResolvableType.forClass(Object.class))).isFalse();
		assertThat(callbacks.supportsEvent(MyBeforeSaveCallback.class, ResolvableType.forClass(User.class))).isFalse();
	}

	@Test
	public void shouldSupportInstanceCallbackTypes() {

		SimpleEntityCallbacks callbacks = new SimpleEntityCallbacks();

		MyBeforeSaveCallback callback = new MyBeforeSaveCallback();

		assertThat(callbacks.supportsEvent(callback, ResolvableType.forClass(Person.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isTrue();
		assertThat(callbacks.supportsEvent(callback, ResolvableType.forClass(Child.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isTrue();
		assertThat(callbacks.supportsEvent(callback, ResolvableType.forClass(PersonDocument.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isTrue();
		assertThat(callbacks.supportsEvent(callback, ResolvableType.forClass(PersonDocument.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isTrue();

		assertThat(callbacks.supportsEvent(callback, ResolvableType.forClass(User.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isFalse();
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
}
