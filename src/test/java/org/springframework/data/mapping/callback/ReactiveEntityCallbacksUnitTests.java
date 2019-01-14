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

import reactor.core.publisher.Mono;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.PersonDocument;

/**
 * Unit tests for {@link ReactiveEntityCallbacks}.
 *
 * @author Mark Paluch
 */
public class ReactiveEntityCallbacksUnitTests {

	@Test
	public void shouldDispatchCallback() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfig.class);

		ReactiveEntityCallbacks callbacks = new ReactiveEntityCallbacks(ctx);

		PersonDocument personDocument = new PersonDocument(null, "Walter", null);
		Mono<PersonDocument> afterCallback = callbacks.callbackLater(personDocument, ReactiveBeforeSaveCallback.class,
				ReactiveBeforeSaveCallback::onBeforeSave);

		assertThat(personDocument.getSsn()).isNull();
		assertThat(afterCallback.block().getSsn()).isEqualTo(6);
	}

	@Configuration
	static class MyConfig {

		@Bean
		MyReactiveBeforeSaveCallback callback() {
			return new MyReactiveBeforeSaveCallback();
		}

	}

	static class MyReactiveBeforeSaveCallback implements ReactiveBeforeSaveCallback<Person> {

		@Override
		public Mono<Person> onBeforeSave(Person object) {

			PersonDocument result = new PersonDocument(object.getFirstName().length(), object.getFirstName(),
					object.getLastName());

			return Mono.just(result);
		}
	}
}
