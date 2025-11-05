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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link QueryCreationException}.
 *
 * @author Mark Paluch
 */
class QueryCreationExceptionUnitTests {

	@Test // GH-3396
	void getMessageReturnsPlainMessage() throws NoSuchMethodException {

		QueryCreationException exception = QueryCreationException.create("message", null, Object.class,
				getClass().getDeclaredMethod("getMessageReturnsPlainMessage"));

		assertThat(exception.getMessage()).isEqualTo("message");
	}

	@Test // GH-3396
	void getLocalizedMessageReturnsContextualMessage() throws NoSuchMethodException {

		QueryCreationException exception = QueryCreationException.create("message", null, Object.class,
				getClass().getDeclaredMethod("getMessageReturnsPlainMessage"));

		assertThat(exception.getLocalizedMessage()).isEqualTo(
				"Cannot create query for method [" + getClass().getSimpleName() + ".getMessageReturnsPlainMessage()]; message");
	}

	@Test // GH-3396
	void toStringReturnsContextualMessage() throws NoSuchMethodException {

		QueryCreationException exception = QueryCreationException.create("message", null, Object.class,
				getClass().getDeclaredMethod("getMessageReturnsPlainMessage"));

		assertThat(exception.toString()).contains(
				"Cannot create query for method [" + getClass().getSimpleName() + ".getMessageReturnsPlainMessage()]; message");
	}

}
