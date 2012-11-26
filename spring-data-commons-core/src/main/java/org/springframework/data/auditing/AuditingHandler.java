/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.auditing;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Auditable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.util.Assert;

/**
 * Auditing handler to mark entity objects created and modified.
 * 
 * @author Oliver Gierke
 * @since 1.5
 */
public class AuditingHandler<T> implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuditingHandler.class);

	private final AuditableBeanWrapperFactory factory = new AuditableBeanWrapperFactory();
	private DateTimeProvider dateTimeProvider = CurrentDateTimeProvider.INSTANCE;
	private AuditorAware<T> auditorAware;
	private boolean dateTimeForNow = true;
	private boolean modifyOnCreation = true;

	/**
	 * Setter to inject a {@code AuditorAware} component to retrieve the current auditor.
	 * 
	 * @param auditorAware the auditorAware to set
	 */
	public void setAuditorAware(final AuditorAware<T> auditorAware) {

		Assert.notNull(auditorAware);
		this.auditorAware = auditorAware;
	}

	/**
	 * Setter do determine if {@link Auditable#setCreatedDate(DateTime)} and
	 * {@link Auditable#setLastModifiedDate(DateTime)} shall be filled with the current Java time. Defaults to
	 * {@code true}. One might set this to {@code false} to use database features to set entity time.
	 * 
	 * @param dateTimeForNow the dateTimeForNow to set
	 */
	public void setDateTimeForNow(boolean dateTimeForNow) {
		this.dateTimeForNow = dateTimeForNow;
	}

	/**
	 * Set this to false if you want to treat entity creation as modification and thus set the current date as
	 * modification date, too. Defaults to {@code true}.
	 * 
	 * @param modifyOnCreation if modification information shall be set on creation, too
	 */
	public void setModifyOnCreation(boolean modifyOnCreation) {
		this.modifyOnCreation = modifyOnCreation;
	}

	/**
	 * Sets the {@link DateTimeProvider} to be used to determine the dates to be set.
	 * 
	 * @param dateTimeProvider
	 */
	public void setDateTimeProvider(DateTimeProvider dateTimeProvider) {
		this.dateTimeProvider = dateTimeProvider == null ? CurrentDateTimeProvider.INSTANCE : dateTimeProvider;
	}

	/**
	 * Marks the given object as created.
	 * 
	 * @param source
	 */
	public void markCreated(Object source) {
		touch(source, true);
	}

	/**
	 * Marks the given object as modified.
	 * 
	 * @param source
	 */
	public void markModified(Object source) {
		touch(source, false);
	}

	private void touch(Object target, boolean isNew) {

		AuditableBeanWrapper wrapper = factory.getBeanWrapperFor(target);

		if (wrapper == null) {
			return;
		}

		T auditor = touchAuditor(wrapper, isNew);
		DateTime now = dateTimeForNow ? touchDate(wrapper, isNew) : null;

		Object defaultedNow = now == null ? "not set" : now;
		Object defaultedAuditor = auditor == null ? "unknown" : auditor;

		LOGGER.debug("Touched {} - Last modification at {} by {}", new Object[] { target, defaultedNow, defaultedAuditor });
	}

	/**
	 * Sets modifying and creating auditioner. Creating auditioner is only set on new auditables.
	 * 
	 * @param auditable
	 * @return
	 */
	private T touchAuditor(AuditableBeanWrapper wrapper, boolean isNew) {

		if (null == auditorAware) {
			return null;
		}

		T auditor = auditorAware.getCurrentAuditor();

		if (isNew) {
			wrapper.setCreatedBy(auditor);
			if (!modifyOnCreation) {
				return auditor;
			}
		}

		wrapper.setLastModifiedBy(auditor);
		return auditor;
	}

	/**
	 * Touches the auditable regarding modification and creation date. Creation date is only set on new auditables.
	 * 
	 * @param wrapper
	 * @return
	 */
	private DateTime touchDate(AuditableBeanWrapper wrapper, boolean isNew) {

		DateTime now = dateTimeProvider.getDateTime();

		if (isNew) {
			wrapper.setCreatedDate(now);
			if (!modifyOnCreation) {
				return now;
			}
		}

		wrapper.setLastModifiedDate(now);
		return now;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {

		if (auditorAware == null) {
			LOGGER.debug("No AuditorAware set! Auditing will not be applied!");
		}
	}
}
