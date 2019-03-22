/*
 * Copyright 2012-2018 the original author or authors.
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

import lombok.RequiredArgsConstructor;

import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Auditable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.util.BeanLookup;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.ProxyUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Auditing handler to mark entity objects created and modified.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.5
 */
public class AuditingHandler implements BeanFactoryAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuditingHandler.class);

	private final DefaultAuditableBeanWrapperFactory factory;

	private DateTimeProvider dateTimeProvider = CurrentDateTimeProvider.INSTANCE;
	private @Nullable Lazy<AuditorAwareAdapter> auditorAware;
	private boolean dateTimeForNow = true;
	private boolean modifyOnCreation = true;

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

		Assert.notNull(entities, "PersistentEntities must not be null!");

		this.factory = new MappingAuditableBeanWrapperFactory(entities);
	}

	/**
	 * Setter to inject a {@code AuditorAware} component to retrieve the current auditor.
	 *
	 * @param auditorAware must not be {@literal null}.
	 */
	public void setAuditorAware(AuditorAware<?> auditorAware) {

		Assert.notNull(auditorAware, "AuditorAware must not be null!");
		this.auditorAware = Lazy.of(new AuditorAwareAdapter(Collections.singleton(auditorAware)));
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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

		if (!ListableBeanFactory.class.isInstance(beanFactory) || auditorAware != null) {
			return;
		}

		Supplier<Collection<? extends AuditorAware>> orderedBeansOfType = () -> BeanLookup
				.orderedBeansOfType(AuditorAware.class, beanFactory);

		this.auditorAware = Lazy.of(orderedBeansOfType) //
				.map(Collection.class::cast) // weird but needed as the compiler doesn't allow the following cast
				.map(it -> new AuditorAwareAdapter((Collection<? extends AuditorAware<?>>) it));
	}

	/**
	 * Marks the given object as created.
	 *
	 * @param source
	 */
	public <T> T markCreated(T source) {

		Assert.notNull(source, "Entity must not be null!");

		return touch(source, true);
	}

	/**
	 * Marks the given object as modified.
	 *
	 * @param source
	 */
	public <T> T markModified(T source) {

		Assert.notNull(source, "Entity must not be null!");

		return touch(source, false);
	}

	/**
	 * Returns whether the given source is considered to be auditable in the first place
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	protected final boolean isAuditable(Object source) {

		Assert.notNull(source, "Source must not be null!");

		return factory.getBeanWrapperFor(source).isPresent();
	}

	private <T> T touch(T target, boolean isNew) {

		Optional<AuditableBeanWrapper<T>> wrapper = factory.getBeanWrapperFor(target);

		return wrapper.map(it -> {

			Optional<TemporalAccessor> now = dateTimeForNow ? touchDate(it, isNew) : Optional.empty();

			touchAuditor(target, it, isNew, now);

			return it.getBean();

		}).orElse(target);
	}

	private AuditorAwareAdapter getAuditorAware() {

		Lazy<AuditorAwareAdapter> toUse = auditorAware;

		return toUse == null ? AuditorAwareAdapter.EMPTY : toUse.get();
	}

	/**
	 * Sets modifying and creating auditor. Creating auditor is only set on new auditables.
	 *
	 * @param auditable
	 * @return
	 */
	private void touchAuditor(Object target, AuditableBeanWrapper<?> wrapper, boolean isNew,
			Optional<TemporalAccessor> time) {

		Assert.notNull(wrapper, "AuditableBeanWrapper must not be null!");

		AuditorLookup lookup = new AuditorLookup(getAuditorAware());

		if (isNew) {

			Optional<?> creator = wrapper.getCreatorType() //
					.flatMap(lookup::getTypedAuditor) //
					.map(wrapper::setCreatedBy);

			logModification(target, time, creator, "Touched {} for creation by {} at {}!");
		}

		if (!isNew || modifyOnCreation) {

			Optional<?> modifier = wrapper.getModifierType() //
					.flatMap(lookup::getTypedAuditor) //
					.map(wrapper::setLastModifiedBy);

			logModification(target, time, modifier, "Touched {} for modification by {} at {}!");
		}
	}

	/**
	 * Touches the auditable regarding modification and creation date. Creation date is only set on new auditables.
	 *
	 * @param wrapper
	 * @return
	 */
	private Optional<TemporalAccessor> touchDate(AuditableBeanWrapper<?> wrapper, boolean isNew) {

		Assert.notNull(wrapper, "AuditableBeanWrapper must not be null!");

		Optional<TemporalAccessor> now = dateTimeProvider.getNow();

		Assert.notNull(now, () -> String.format("Now must not be null! Returned by: %s!", dateTimeProvider.getClass()));

		now.filter(__ -> isNew).ifPresent(it -> wrapper.setCreatedDate(it));
		now.filter(__ -> !isNew || modifyOnCreation).ifPresent(it -> wrapper.setLastModifiedDate(it));

		return now;
	}

	private static void logModification(Object target, Optional<TemporalAccessor> time, Optional<?> auditor,
			String template) {

		if (!LOGGER.isDebugEnabled()) {
			return;
		}

		Object defaultedNow = time.map(Object::toString).orElse("not set");
		Object defaultedAuditor = auditor.map(Object::toString).orElse("unknown");

		LOGGER.debug(template, target, defaultedAuditor, defaultedNow);
	}

	/**
	 * Simple value object to cache auditor lookups by type so that we don't unnecessarily invoke {@link AuditorAware}
	 * instances for the same type multiple times.
	 *
	 * @author Oliver Gierke
	 */
	@RequiredArgsConstructor
	private static class AuditorLookup {

		private final AuditorAwareAdapter adapter;
		private final Map<Class<?>, Optional<?>> resolvedAuditors = new HashMap<>(2);

		/**
		 * Returns the auditor of the given type.
		 * 
		 * @param type must not be {@literal null}.
		 * @return
		 */
		public Optional<?> getTypedAuditor(Class<?> type) {

			return resolvedAuditors.computeIfAbsent(type,
					it -> adapter.getAuditorAwareFor(it).flatMap(AuditorLookup::lookupAuditor));
		}

		private static Optional<?> lookupAuditor(AuditorAware<?> auditorAware) {

			Optional<?> auditor = auditorAware.getCurrentAuditor();

			Assert.notNull(auditor,
					() -> String.format("Auditor must not be null! Returned by: %s!", AopUtils.getTargetClass(auditorAware)));

			return auditor;
		}
	}

	/**
	 * Simple registry that allows per auditor type lookups of {@link AuditorAware} instances.
	 *
	 * @author Oliver Gierke
	 * @since 2.1
	 */
	private static class AuditorAwareAdapter {

		private static final AuditorAwareAdapter EMPTY = new AuditorAwareAdapter(Collections.emptyList());

		private final Map<Class<?>, AuditorAware<?>> delegates;

		public AuditorAwareAdapter(Collection<? extends AuditorAware<?>> delegates) {

			this.delegates = delegates.stream()
					.collect(Collectors.toMap(AuditorAwareAdapter::determineGenericArgument, Function.identity()));
		}

		/**
		 * Returns the {@link AuditorAware} for the given auditor type.
		 * 
		 * @param auditorType must not be {@literal null}.
		 * @return
		 */
		public Optional<AuditorAware<?>> getAuditorAwareFor(Class<?> auditorType) {

			return delegates.entrySet().stream()//
					.filter(it -> it.getKey().isAssignableFrom(auditorType))//
					.findFirst()//
					.map(Entry::getValue);
		}

		private static Class<?> determineGenericArgument(AuditorAware<?> auditorAware) {

			Class<?> userClass = ProxyUtils.getUserClass(auditorAware);
			Class<?> resolvedType = ResolvableType.forClass(AuditorAware.class, userClass) //
					.getGeneric(0).resolve();

			if (resolvedType == null) {
				throw new IllegalStateException(
						String.format("Cannot determine auditor type for AuditorAware %s!", auditorAware.getClass()));
			}

			return resolvedType;
		}
	}
}
