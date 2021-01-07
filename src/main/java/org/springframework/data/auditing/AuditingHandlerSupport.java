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

import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.data.domain.Auditable;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Support class to implement auditing handlers.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 2.4
 */
public abstract class AuditingHandlerSupport {

	private static final Log logger = LogFactory.getLog(AuditingHandlerSupport.class);

	private final AuditableBeanWrapperFactory factory;

	private DateTimeProvider dateTimeProvider = CurrentDateTimeProvider.INSTANCE;
	private boolean dateTimeForNow = true;
	private boolean modifyOnCreation = true;

	/**
	 * Creates a new {@link AuditableBeanWrapper} using the given {@link PersistentEntities} when looking up auditing
	 * metadata via reflection.
	 *
	 * @param entities must not be {@literal null}.
	 */
	public AuditingHandlerSupport(PersistentEntities entities) {

		Assert.notNull(entities, "PersistentEntities must not be null!");

		this.factory = new MappingAuditableBeanWrapperFactory(entities);
	}

	/**
	 * Setter do determine if {@link Auditable#setCreatedDate(TemporalAccessor)}} and
	 * {@link Auditable#setLastModifiedDate(TemporalAccessor)} shall be filled with the current Java time. Defaults to
	 * {@code true}. One might set this to {@code false} to use database features to set entity time.
	 *
	 * @param dateTimeForNow the dateTimeForNow to set
	 */
	public void setDateTimeForNow(boolean dateTimeForNow) {
		this.dateTimeForNow = dateTimeForNow;
	}

	/**
	 * Set this to true if you want to treat entity creation as modification and thus setting the current date as
	 * modification date during creation, too. Defaults to {@code true}.
	 *
	 * @param modifyOnCreation if modification information shall be set on creation, too
	 */
	public void setModifyOnCreation(boolean modifyOnCreation) {
		this.modifyOnCreation = modifyOnCreation;
	}

	/**
	 * Sets the {@link DateTimeProvider} to be used to determine the dates to be set.
	 *
	 * @param dateTimeProvider can be {@literal null}, defaults to {@link CurrentDateTimeProvider} in that case.
	 */
	public void setDateTimeProvider(@Nullable DateTimeProvider dateTimeProvider) {
		this.dateTimeProvider = dateTimeProvider == null ? CurrentDateTimeProvider.INSTANCE : dateTimeProvider;
	}

	/**
	 * Returns whether the given source is considered to be auditable in the first place.
	 *
	 * @param source must not be {@literal null}.
	 * @return {@literal true} if the given {@literal source} considered to be auditable.
	 */
	protected final boolean isAuditable(Object source) {

		Assert.notNull(source, "Source entity must not be null!");

		return factory.getBeanWrapperFor(source).isPresent();
	}

	/**
	 * Marks the given object as created.
	 *
	 * @param auditor can be {@literal null}.
	 * @param source must not be {@literal null}.
	 */
	<T> T markCreated(Auditor auditor, T source) {

		Assert.notNull(source, "Source entity must not be null!");

		return touch(auditor, source, true);
	}

	/**
	 * Marks the given object as modified.
	 *
	 * @param auditor
	 * @param source
	 */
	<T> T markModified(Auditor auditor, T source) {

		Assert.notNull(source, "Source entity must not be null!");

		return touch(auditor, source, false);
	}

	private <T> T touch(Auditor auditor, T target, boolean isNew) {

		Optional<AuditableBeanWrapper<T>> wrapper = factory.getBeanWrapperFor(target);

		return wrapper.map(it -> {

			touchAuditor(auditor, it, isNew);
			Optional<TemporalAccessor> now = dateTimeForNow ? touchDate(it, isNew) : Optional.empty();

			if (logger.isDebugEnabled()) {

				Object defaultedNow = now.map(Object::toString).orElse("not set");
				Object defaultedAuditor = auditor.isPresent() ? auditor.toString() : "unknown";

				logger.debug(
						LogMessage.format("Touched %s - Last modification at %s by %s", target, defaultedNow, defaultedAuditor));
			}

			return it.getBean();
		}).orElse(target);
	}

	/**
	 * Sets modifying and creating auditor. Creating auditor is only set on new auditables.
	 *
	 * @param auditor
	 * @param wrapper
	 * @param isNew
	 * @return
	 */
	private void touchAuditor(Auditor auditor, AuditableBeanWrapper<?> wrapper, boolean isNew) {

		if(!auditor.isPresent()) {
			return;
		}

		Assert.notNull(wrapper, "AuditableBeanWrapper must not be null!");

		if (isNew) {
			wrapper.setCreatedBy(auditor.getValue());
		}

		if (!isNew || modifyOnCreation) {
			wrapper.setLastModifiedBy(auditor.getValue());
		}
	}

	/**
	 * Touches the auditable regarding modification and creation date. Creation date is only set on new auditables.
	 *
	 * @param wrapper
	 * @param isNew
	 * @return
	 */
	private Optional<TemporalAccessor> touchDate(AuditableBeanWrapper<?> wrapper, boolean isNew) {

		Assert.notNull(wrapper, "AuditableBeanWrapper must not be null!");

		Optional<TemporalAccessor> now = dateTimeProvider.getNow();

		Assert.notNull(now, () -> String.format("Now must not be null! Returned by: %s!", dateTimeProvider.getClass()));

		now.filter(__ -> isNew).ifPresent(wrapper::setCreatedDate);
		now.filter(__ -> !isNew || modifyOnCreation).ifPresent(wrapper::setLastModifiedDate);

		return now;
	}
}
