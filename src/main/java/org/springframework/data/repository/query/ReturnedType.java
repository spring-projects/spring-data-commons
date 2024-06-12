/*
 * Copyright 2015-2024 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;

/**
 * A representation of the type returned by a {@link QueryMethod}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.12
 */
public abstract class ReturnedType {

	private static final Map<CacheKey, ReturnedType> cache = new ConcurrentHashMap<>(32);

	private final Class<?> domainType;

	private ReturnedType(Class<?> domainType) {
		this.domainType = domainType;
	}

	/**
	 * Creates a new {@link ReturnedType} for the given returned type, domain type and {@link ProjectionFactory}.
	 *
	 * @param returnedType must not be {@literal null}.
	 * @param domainType must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 * @return
	 */
	static ReturnedType of(Class<?> returnedType, Class<?> domainType, ProjectionFactory factory) {

		Assert.notNull(returnedType, "Returned type must not be null");
		Assert.notNull(domainType, "Domain type must not be null");
		Assert.notNull(factory, "ProjectionFactory must not be null");

		return cache.computeIfAbsent(CacheKey.of(returnedType, domainType, factory.hashCode()), key -> {

			return returnedType.isInterface()
					? new ReturnedInterface(factory.getProjectionInformation(returnedType), domainType)
					: new ReturnedClass(returnedType, domainType);
		});
	}

	/**
	 * Returns the entity type.
	 *
	 * @return
	 */
	public final Class<?> getDomainType() {
		return domainType;
	}

	/**
	 * Returns whether the given source object is an instance of the returned type.
	 *
	 * @param source can be {@literal null}.
	 * @return
	 */
	public final boolean isInstance(@Nullable Object source) {
		return getReturnedType().isInstance(source);
	}

	/**
	 * Returns whether the type is projecting, i.e. not of the domain type.
	 *
	 * @return
	 */
	public abstract boolean isProjecting();

	/**
	 * Returns the type of the individual objects to return.
	 *
	 * @return
	 */
	public abstract Class<?> getReturnedType();

	/**
	 * Returns whether the returned type will require custom construction.
	 *
	 * @return
	 */
	public abstract boolean needsCustomConstruction();

	/**
	 * Returns the type that the query execution is supposed to pass to the underlying infrastructure. {@literal null} is
	 * returned to indicate a generic type (a map or tuple-like type) shall be used.
	 *
	 * @return
	 */
	@Nullable
	public abstract Class<?> getTypeToRead();

	/**
	 * Returns the properties required to be used to populate the result.
	 *
	 * @return
	 */
	public abstract List<String> getInputProperties();

	/**
	 * A {@link ReturnedType} that's backed by an interface.
	 *
	 * @author Oliver Gierke
	 * @since 1.12
	 */
	private static final class ReturnedInterface extends ReturnedType {

		private final ProjectionInformation information;
		private final Class<?> domainType;

		/**
		 * Creates a new {@link ReturnedInterface} from the given {@link ProjectionInformation} and domain type.
		 *
		 * @param information must not be {@literal null}.
		 * @param domainType must not be {@literal null}.
		 */
		public ReturnedInterface(ProjectionInformation information, Class<?> domainType) {

			super(domainType);

			Assert.notNull(information, "Projection information must not be null");

			this.information = information;
			this.domainType = domainType;
		}

		@Override
		public Class<?> getReturnedType() {
			return information.getType();
		}

		public boolean needsCustomConstruction() {
			return isProjecting() && information.isClosed();
		}

		@Override
		public boolean isProjecting() {
			return !information.getType().isAssignableFrom(domainType);
		}

		@Nullable
		@Override
		public Class<?> getTypeToRead() {
			return isProjecting() && information.isClosed() ? null : domainType;
		}

		@Override
		public List<String> getInputProperties() {

			List<String> properties = new ArrayList<>();

			for (PropertyDescriptor descriptor : information.getInputProperties()) {
				if (!properties.contains(descriptor.getName())) {
					properties.add(descriptor.getName());
				}
			}

			return properties;
		}
	}

	/**
	 * A {@link ReturnedType} that's backed by an actual class.
	 *
	 * @author Oliver Gierke
	 * @since 1.12
	 */
	private static final class ReturnedClass extends ReturnedType {

		private static final Set<Class<?>> VOID_TYPES = new HashSet<>(Arrays.asList(Void.class, void.class));

		private final Class<?> type;
		private final List<String> inputProperties;

		/**
		 * Creates a new {@link ReturnedClass} instance for the given returned type and domain type.
		 *
		 * @param returnedType must not be {@literal null}.
		 * @param domainType must not be {@literal null}.
		 */
		public ReturnedClass(Class<?> returnedType, Class<?> domainType) {

			super(domainType);

			Assert.notNull(returnedType, "Returned type must not be null");
			Assert.notNull(domainType, "Domain type must not be null");
			Assert.isTrue(!returnedType.isInterface(), "Returned type must not be an interface");

			this.type = returnedType;
			this.inputProperties = detectConstructorParameterNames(returnedType);
		}

		@Override
		public Class<?> getReturnedType() {
			return type;
		}

		@NonNull
		public Class<?> getTypeToRead() {
			return type;
		}

		@Override
		public boolean isProjecting() {
			return isDto();
		}

		public boolean needsCustomConstruction() {
			return isDto() && !inputProperties.isEmpty();
		}

		@Override
		public List<String> getInputProperties() {
			return inputProperties;
		}

		private List<String> detectConstructorParameterNames(Class<?> type) {

			if (!isDto()) {
				return Collections.emptyList();
			}

			PreferredConstructor<?, ?> constructor = PreferredConstructorDiscoverer.discover(type);

			if (constructor == null) {
				return Collections.emptyList();
			}

			List<String> properties = new ArrayList<>(constructor.getConstructor().getParameterCount());

			for (Parameter<Object, ?> parameter : constructor.getParameters()) {
				properties.add(parameter.getName());
			}

			return properties;
		}

		private boolean isDto() {
			return !Object.class.equals(type) && //
					!type.isEnum() && //
					!isDomainSubtype() && //
					!isPrimitiveOrWrapper() && //
					!Number.class.isAssignableFrom(type) && //
					!VOID_TYPES.contains(type) && //
					!type.getPackage().getName().startsWith("java.");
		}

		private boolean isDomainSubtype() {
			return getDomainType().equals(type) && getDomainType().isAssignableFrom(type);
		}

		private boolean isPrimitiveOrWrapper() {
			return ClassUtils.isPrimitiveOrWrapper(type);
		}
	}

	private static final class CacheKey {

		private final Class<?> returnedType;
		private final Class<?> domainType;
		private final int projectionFactoryHashCode;

		private CacheKey(Class<?> returnedType, Class<?> domainType, int projectionFactoryHashCode) {

			this.returnedType = returnedType;
			this.domainType = domainType;
			this.projectionFactoryHashCode = projectionFactoryHashCode;
		}

		public static CacheKey of(Class<?> returnedType, Class<?> domainType, int projectionFactoryHashCode) {
			return new CacheKey(returnedType, domainType, projectionFactoryHashCode);
		}

		public Class<?> getReturnedType() {
			return this.returnedType;
		}

		public Class<?> getDomainType() {
			return this.domainType;
		}

		public int getProjectionFactoryHashCode() {
			return this.projectionFactoryHashCode;
		}

		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof CacheKey cacheKey)) {
				return false;
			}

			if (projectionFactoryHashCode != cacheKey.projectionFactoryHashCode) {
				return false;
			}

			if (!ObjectUtils.nullSafeEquals(returnedType, cacheKey.returnedType)) {
				return false;
			}

			return ObjectUtils.nullSafeEquals(domainType, cacheKey.domainType);
		}

		@Override
		public int hashCode() {
			int result = ObjectUtils.nullSafeHashCode(returnedType);
			result = 31 * result + ObjectUtils.nullSafeHashCode(domainType);
			result = 31 * result + projectionFactoryHashCode;
			return result;
		}

		@Override
		public String toString() {
			return "ReturnedType.CacheKey(returnedType=" + this.getReturnedType() + ", domainType=" + this.getDomainType()
					+ ", projectionFactoryHashCode=" + this.getProjectionFactoryHashCode() + ")";
		}
	}
}
