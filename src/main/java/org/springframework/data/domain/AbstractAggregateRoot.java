/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.data.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.util.Assert;

/**
 * Convenience base class for aggregate roots that exposes a {@link #registerEvent(Object)} to capture domain events and
 * expose them via {@link #domainEvents())}. The implementation is using the general event publication mechanism implied
 * by {@link DomainEvents} and {@link AfterDomainEventPublication}. If in doubt or need to customize anything here,
 * rather build your own base class and use the annotations directly.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.13
 */
public class AbstractAggregateRoot<A extends AbstractAggregateRoot<A>> {

	private transient final @Transient List<Object> domainEvents = new ArrayList<>();

	/**
	 * Registers the given event object for publication on a call to a Spring Data repository's save methods.
	 *
	 * @param event must not be {@literal null}.
	 * @return the event that has been added.
	 * @see #andEvent(Object)
	 */
	protected <T> T registerEvent(T event) {

		Assert.notNull(event, "Domain event must not be null!");

		this.domainEvents.add(event);
		return event;
	}

	/**
	 * Clears all domain events currently held. Usually invoked by the infrastructure in place in Spring Data
	 * repositories.
	 */
	@AfterDomainEventPublication
	protected void clearDomainEvents() {
		this.domainEvents.clear();
	}

	/**
	 * All domain events currently captured by the aggregate.
	 */
	@DomainEvents
	protected Collection<Object> domainEvents() {
		return Collections.unmodifiableList(domainEvents);
	}

	/**
	 * Adds all events contained in the given aggregate to the current one.
	 *
	 * @param aggregate must not be {@literal null}.
	 * @return the aggregate
	 */
	@SuppressWarnings("unchecked")
	protected final A andEventsFrom(A aggregate) {

		Assert.notNull(aggregate, "Aggregate must not be null!");

		this.domainEvents.addAll(aggregate.domainEvents());

		return (A) this;
	}

	/**
	 * Adds the given event to the aggregate for later publication when calling a Spring Data repository's save-method.
	 * Does the same as {@link #registerEvent(Object)} but returns the aggregate instead of the event.
	 *
	 * @param event must not be {@literal null}.
	 * @return the aggregate
	 * @see #registerEvent(Object)
	 */
	@SuppressWarnings("unchecked")
	protected final A andEvent(Object event) {

		registerEvent(event);

		return (A) this;
	}
}
