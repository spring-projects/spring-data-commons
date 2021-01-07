/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.util.Assert;

/**
 * Auditing handler to mark entity objects created and modified.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.5
 */
public class AuditingHandler extends AuditingHandlerSupport implements InitializingBean {

	private static final Log logger = LogFactory.getLog(AuditingHandler.class);

	private Optional<AuditorAware<?>> auditorAware;

	/**
	 * Creates a new {@link AuditableBeanWrapper} using the given {@link MappingContext} when looking up auditing metadata
	 * via reflection.
	 *
	 * @param mappingContext must not be {@literal null}.
	 * @since 1.8
	 * @deprecated use {@link AuditingHandler(PersistentEntities)} instead.
	 */
	@Deprecated
	public AuditingHandler(
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {
		this(PersistentEntities.of(mappingContext));
	}

	/**
	 * Creates a new {@link AuditableBeanWrapper} using the given {@link PersistentEntities} when looking up auditing
	 * metadata via reflection.
	 *
	 * @param entities must not be {@literal null}.
	 * @since 1.10
	 */
	public AuditingHandler(PersistentEntities entities) {

		super(entities);
		Assert.notNull(entities, "PersistentEntities must not be null!");

		this.auditorAware = Optional.empty();
	}

	/**
	 * Setter to inject a {@code AuditorAware} component to retrieve the current auditor.
	 *
	 * @param auditorAware must not be {@literal null}.
	 */
	public void setAuditorAware(AuditorAware<?> auditorAware) {

		Assert.notNull(auditorAware, "AuditorAware must not be null!");
		this.auditorAware = Optional.of(auditorAware);
	}

	/**
	 * Marks the given object as created.
	 *
	 * @param source
	 */
	public <T> T markCreated(T source) {

		Assert.notNull(source, "Entity must not be null!");

		return markCreated(getAuditor(), source);
	}

	/**
	 * Marks the given object as modified.
	 *
	 * @param source
	 */
	public <T> T markModified(T source) {

		Assert.notNull(source, "Entity must not be null!");

		return markModified(getAuditor(), source);
	}

	Auditor<?> getAuditor() {

		return auditorAware.map(AuditorAware::getCurrentAuditor).map(Auditor::ofOptional) //
				.orElse(Auditor.none());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {

		if (!auditorAware.isPresent()) {
			logger.debug("No AuditorAware set! Auditing will not be applied!");
		}
	}
}
