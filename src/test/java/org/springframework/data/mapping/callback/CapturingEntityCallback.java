/*
 * Copyright 2019-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.data.mapping.Person;
import org.springframework.data.mapping.PersonDocument;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * @author Christoph Strobl
 */
class CapturingEntityCallback implements EntityCallback<Person> {

	final List<Person> captured = new ArrayList<>(3);
	final @Nullable Person returnValue;

	CapturingEntityCallback() {
		this(new PersonDocument(null, null, null));
	}

	CapturingEntityCallback(@Nullable Person returnValue) {
		this.returnValue = returnValue;
	}

	public Person doSomething(Person person) {

		captured.add(person);
		return returnValue;
	}

	Person capturedValue() {
		return CollectionUtils.lastElement(captured);
	}

	List<Person> capturedValues() {
		return captured;
	}

	static class FirstCallback extends CapturingEntityCallback implements Ordered {

		@Override
		public int getOrder() {
			return 1;
		}
	}

	static class SecondCallback extends CapturingEntityCallback implements Ordered {

		public SecondCallback() {}

		public SecondCallback(Person returnValue) {
			super(returnValue);
		}

		@Override
		public int getOrder() {
			return 2;
		}
	}

	static class ThirdCallback extends CapturingEntityCallback implements Ordered {

		@Override
		public int getOrder() {
			return 3;
		}
	}
}
