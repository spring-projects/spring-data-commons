/*
 * Copyright 2019 the original author or authors.
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

import java.util.Collection;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.data.mapping.Child;
import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.PersonDocument;

/**
 * @author Christoph Strobl
 */
public class EntityCallbackDiscovererUnitTests {

	@Test // DATACMNS-1467
	public void shouldDiscoverCallbackType() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		EntityCallbackDiscoverer discoverer = new EntityCallbackDiscoverer(ctx);

		Collection<EntityCallback<Person>> entityCallbacks = discoverer.getEntityCallbacks(PersonDocument.class,
				ResolvableType.forType(BeforeSaveCallback.class));

		assertThat(entityCallbacks).hasSize(1).element(0).isInstanceOf(MyBeforeSaveCallback.class);
	}

	@Test // DATACMNS-1467
	public void shouldDiscoverCallbackTypeByName() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		EntityCallbackDiscoverer discoverer = new EntityCallbackDiscoverer(ctx);
		discoverer.clear();
		discoverer.addEntityCallbackBean("namedCallback");

		Collection<EntityCallback<Person>> entityCallbacks = discoverer.getEntityCallbacks(PersonDocument.class,
				ResolvableType.forType(BeforeSaveCallback.class));

		assertThat(entityCallbacks).hasSize(1).element(0).isInstanceOf(MyOtherCallback.class);
	}

	@Test // DATACMNS-1467
	public void shouldSupportCallbackTypes() {

		EntityCallbackDiscoverer discoverer = new EntityCallbackDiscoverer();

		assertThat(discoverer.supportsEvent(MyBeforeSaveCallback.class, ResolvableType.forClass(Person.class))).isTrue();
		assertThat(discoverer.supportsEvent(MyBeforeSaveCallback.class, ResolvableType.forClass(Child.class))).isTrue();
		assertThat(discoverer.supportsEvent(BeforeSaveCallback.class, ResolvableType.forClass(PersonDocument.class)))
				.isTrue();

		assertThat(discoverer.supportsEvent(MyBeforeSaveCallback.class, ResolvableType.forClass(Object.class))).isFalse();
		assertThat(discoverer.supportsEvent(MyBeforeSaveCallback.class, ResolvableType.forClass(User.class))).isFalse();
	}

	@Test // DATACMNS-1467
	public void shouldSupportInstanceCallbackTypes() {

		EntityCallbackDiscoverer discoverer = new EntityCallbackDiscoverer();

		MyBeforeSaveCallback callback = new MyBeforeSaveCallback();

		assertThat(discoverer.supportsEvent(callback, ResolvableType.forClass(Person.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isTrue();
		assertThat(discoverer.supportsEvent(callback, ResolvableType.forClass(Child.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isTrue();
		assertThat(discoverer.supportsEvent(callback, ResolvableType.forClass(PersonDocument.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isTrue();
		assertThat(discoverer.supportsEvent(callback, ResolvableType.forClass(PersonDocument.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isTrue();

		assertThat(discoverer.supportsEvent(callback, ResolvableType.forClass(User.class),
				ResolvableType.forClass(BeforeSaveCallback.class))).isFalse();
	}

	@Test // DATACMNS-1467
	public void shouldDispatchInOrder() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(OrderedConfig.class);

		EntityCallbackDiscoverer discoverer = new EntityCallbackDiscoverer(ctx);

		Collection<EntityCallback<Person>> entityCallbacks = discoverer.getEntityCallbacks(PersonDocument.class,
				ResolvableType.forType(EntityCallback.class));

		assertThat(entityCallbacks).containsExactly(ctx.getBean("callback1", EntityCallback.class),
				ctx.getBean("callback2", EntityCallback.class), ctx.getBean("callback3", EntityCallback.class),
				ctx.getBean("callback4", EntityCallback.class));
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
	static class OrderedConfig {

		@Bean
		EntityCallback<Person> callback4() {

			return (BeforeSaveCallback<Person>) object -> object;
		}

		@Bean
		EntityCallback<Person> callback2() {
			return new Second();
		}

		@Bean
		EntityCallback<Person> callback3() {
			return new Third();
		}

		@Bean
		EntityCallback<Person> callback1() {
			return new First();
		}

		@Order(3)
		static class Third implements EntityCallback<Person> {

			public Person beforeSave(Person object) {
				return object;
			}
		}

		static class Second implements EntityCallback<Person>, Ordered {

			public Person beforeSave(Person object) {
				return object;
			}

			@Override
			public int getOrder() {
				return 2;
			}
		}

		@Order(1)
		static class First implements EntityCallback<Person> {

			public Person beforeSave(Person object) {
				return object;
			}
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
}
