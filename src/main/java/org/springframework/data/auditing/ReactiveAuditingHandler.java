/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.data.auditing;

import reactor.core.publisher.Mono;

import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.util.Assert;

/**
 * Auditing handler to mark entity objects created and modified.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 * @since 2.4
 */
public class ReactiveAuditingHandler extends AuditingHandlerSupport {

	private ReactiveAuditorAware<?> auditorAware = Mono::empty;

	/**
	 * Creates a new {@link ReactiveAuditingHandler} using the given {@link PersistentEntities} when looking up auditing
	 * metadata.
	 *
	 * @param entities must not be {@literal null}.
	 */
	public ReactiveAuditingHandler(PersistentEntities entities) {
		super(entities);
	}

	/**
	 * Setter to inject a {@link ReactiveAuditorAware} component to retrieve the current auditor.
	 *
	 * @param auditorAware must not be {@literal null}.
	 */
	public void setAuditorAware(ReactiveAuditorAware<?> auditorAware) {

		Assert.notNull(auditorAware, "AuditorAware must not be null!");
		this.auditorAware = auditorAware;
	}

	/**
	 * Marks the given object as created.
	 *
	 * @param source must not be {@literal null}.
	 */
	public <T> Mono<T> markCreated(T source) {

		Assert.notNull(source, "Entity must not be null!");

		return getAuditor() //
				.map(auditor -> markCreated(auditor, source));
	}

	/**
	 * Marks the given object as modified.
	 *
	 * @param source must not be {@literal null}.
	 */
	public <T> Mono<T> markModified(T source) {

		Assert.notNull(source, "Entity must not be null!");

		return getAuditor() //
				.map(auditor -> markModified(auditor, source));
	}

	private Mono<? extends Auditor<?>> getAuditor() {

		return auditorAware.getCurrentAuditor() //
				.map(Auditor::of) //
				.defaultIfEmpty(Auditor.none());
	}
}
