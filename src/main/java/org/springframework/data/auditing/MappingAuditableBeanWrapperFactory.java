/*
 * Copyright 2014-2015 the original author or authors.
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

import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Auditable;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.util.Optionals;
import org.springframework.util.Assert;

/**
 * {@link AuditableBeanWrapperFactory} that will create am {@link AuditableBeanWrapper} using mapping information
 * obtained from a {@link MappingContext} to detect auditing configuration and eventually invoking setting the auditing
 * values.
 * 
 * @author Oliver Gierke
 * @since 1.8
 */
public class MappingAuditableBeanWrapperFactory extends DefaultAuditableBeanWrapperFactory {

	private final PersistentEntities entities;
	private final Map<Class<?>, MappingAuditingMetadata> metadataCache;

	/**
	 * Creates a new {@link MappingAuditableBeanWrapperFactory} using the given {@link PersistentEntities}.
	 * 
	 * @param entities must not be {@literal null}.
	 */
	public MappingAuditableBeanWrapperFactory(PersistentEntities entities) {

		Assert.notNull(entities, "PersistentEntities must not be null!");

		this.entities = entities;
		this.metadataCache = new HashMap<Class<?>, MappingAuditingMetadata>();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.auditing.AuditableBeanWrapperFactory#getBeanWrapperFor(java.lang.Object)
	 */
	@Override
	public Optional<AuditableBeanWrapper> getBeanWrapperFor(Optional<? extends Object> source) {

		return source.flatMap(it -> {

			if (it instanceof Auditable) {
				return super.getBeanWrapperFor(source);
			}

			Class<?> type = it.getClass();
			PersistentEntity<?, ?> entity = entities.getPersistentEntity(type);

			if (entity == null) {
				return super.getBeanWrapperFor(source);
			}

			MappingAuditingMetadata metadata = metadataCache.computeIfAbsent(type,
					foo -> new MappingAuditingMetadata(entity));

			return Optional.ofNullable(metadata.isAuditable()
					? new MappingMetadataAuditableBeanWrapper(entity.getPropertyAccessor(it), metadata) : null);
		});
	}

	/**
	 * Captures {@link PersistentProperty} instances equipped with auditing annotations.
	 * 
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	static class MappingAuditingMetadata {

		private final Optional<? extends PersistentProperty<?>> createdByProperty, createdDateProperty,
				lastModifiedByProperty, lastModifiedDateProperty;

		/**
		 * Creates a new {@link MappingAuditingMetadata} instance from the given {@link PersistentEntity}.
		 * 
		 * @param entity must not be {@literal null}.
		 */
		public MappingAuditingMetadata(PersistentEntity<?, ? extends PersistentProperty<?>> entity) {

			Assert.notNull(entity, "PersistentEntity must not be null!");

			this.createdByProperty = entity.getPersistentProperty(CreatedBy.class);
			this.createdDateProperty = entity.getPersistentProperty(CreatedDate.class);
			this.lastModifiedByProperty = entity.getPersistentProperty(LastModifiedBy.class);
			this.lastModifiedDateProperty = entity.getPersistentProperty(LastModifiedDate.class);
		}

		/**
		 * Returns whether the {@link PersistentEntity} is auditable at all (read: any of the auditing annotations is
		 * present).
		 * 
		 * @return
		 */
		public boolean isAuditable() {
			return Optionals.isAnyPresent(createdByProperty, createdDateProperty, lastModifiedByProperty,
					lastModifiedDateProperty);
		}
	}

	/**
	 * {@link AuditableBeanWrapper} using {@link MappingAuditingMetadata} and a {@link PersistentPropertyAccessor} to set
	 * values on auditing properties.
	 * 
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	static class MappingMetadataAuditableBeanWrapper extends DateConvertingAuditableBeanWrapper {

		private final PersistentPropertyAccessor accessor;
		private final MappingAuditingMetadata metadata;

		/**
		 * Creates a new {@link MappingMetadataAuditableBeanWrapper} for the given target and
		 * {@link MappingAuditingMetadata}.
		 * 
		 * @param target must not be {@literal null}.
		 * @param metadata must not be {@literal null}.
		 */
		public MappingMetadataAuditableBeanWrapper(PersistentPropertyAccessor accessor, MappingAuditingMetadata metadata) {

			Assert.notNull(accessor, "Target object must not be null!");
			Assert.notNull(metadata, "Auditing metadata must not be null!");

			this.accessor = accessor;
			this.metadata = metadata;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedBy(java.util.Optional)
		 */
		@Override
		public Optional<? extends Object> setCreatedBy(Optional<? extends Object> value) {

			metadata.createdByProperty.ifPresent(it -> {
				this.accessor.setProperty(it, value);
			});

			return value;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setCreatedDate(java.util.Optional)
		 */
		@Override
		public Optional<TemporalAccessor> setCreatedDate(Optional<TemporalAccessor> value) {

			metadata.createdDateProperty.ifPresent(it -> {
				this.accessor.setProperty(it, getDateValueToSet(value, it.getType(), it));
			});

			return value;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedBy(java.util.Optional)
		 */
		@Override
		public Optional<? extends Object> setLastModifiedBy(Optional<? extends Object> value) {

			metadata.lastModifiedByProperty.ifPresent(it -> {
				this.accessor.setProperty(it, value);
			});

			return value;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#getLastModifiedDate()
		 */
		@Override
		public Optional<TemporalAccessor> getLastModifiedDate() {
			return getAsTemporalAccessor(metadata.lastModifiedDateProperty.map(it -> accessor.getProperty(it)),
					TemporalAccessor.class);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(java.util.Optional)
		 */
		@Override
		public Optional<TemporalAccessor> setLastModifiedDate(Optional<TemporalAccessor> value) {

			metadata.lastModifiedDateProperty.ifPresent(it -> {
				this.accessor.setProperty(it, getDateValueToSet(value, it.getType(), it));
			});

			return value;
		}
	}
}
