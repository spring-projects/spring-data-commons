/*
 * Copyright 2017-2021 the original author or authors.
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
package org.springframework.data.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AbstractAggregateRoot}.
 *
 * @author Oliver Gierke
 */
class AbstractAggregateRootUnitTests {

	@Test // DATACMNS-928
	void registersEvent() {

		Object event = new Object();

		SampleAggregate aggregate = new SampleAggregate();
		aggregate.registerEvent(event);

		assertThat(aggregate.domainEvents()).containsExactly(event);
	}

	@Test // DATACMNS-928
	void clearsEvents() {

		Object event = new Object();

		SampleAggregate aggregate = new SampleAggregate();
		aggregate.registerEvent(event);

		assertThat(aggregate.domainEvents()).isNotEmpty();

		aggregate.clearDomainEvents();

		assertThat(aggregate.domainEvents()).isEmpty();
	}

	@Test // DATACMNS-928, DATACMNS-1162
	void copiesEventsFromExistingAggregate() {

		SampleAggregate aggregate = new SampleAggregate();
		aggregate.registerEvent(new Object());

		SampleAggregate result = new SampleAggregate().andEventsFrom(aggregate);

		assertThat(result.domainEvents()).isEqualTo(aggregate.domainEvents());
	}

	@Test // DATACMNS-928, DATACMNS-1162
	void addsEventAndReturnsAggregate() {

		Object first = new Object();
		Object second = new Object();

		SampleAggregate aggregate = new SampleAggregate();
		aggregate.registerEvent(first);

		SampleAggregate result = aggregate.andEvent(second);

		assertThat(result).isSameAs(aggregate);
		assertThat(result.domainEvents()).containsExactly(first, second);
	}

	@Test // DATACMNS-928, DATACMNS-1162
	@SuppressWarnings("null")
	void rejectsNullEvent() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new SampleAggregate().andEvent(null));
	}

	@Test // DATACMNS-928
	@SuppressWarnings("null")
	void rejectsNullEventForRegistration() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new SampleAggregate().registerEvent(null));
	}

	@Test // DATACMNS-928, DATACMNS-1162
	@SuppressWarnings("null")
	void rejectsNullAggregate() {

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> new SampleAggregate().andEventsFrom(null));
	}

	static class SampleAggregate extends AbstractAggregateRoot<SampleAggregate> {}
}
