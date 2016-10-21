/*
 * Copyright 2016 the original author or authors.
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

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Oliver Gierke
 */
public class AbstractAggregateRoot {

	private transient final @Getter(onMethod = @__(@DomainEvents)) List<Object> domainEvents = new ArrayList<Object>();

	protected <T> T registerEvent(T event) {

		this.domainEvents.add(event);
		return event;
	}

	@AfterDomainEventPublication
	public void clearDomainEvents() {
		this.domainEvents.clear();
	}
}
