/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.repository.query;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.util.Optionals;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A representation of the type returned by a {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 * @since 1.12
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ReturnedType {

	private final @NonNull Class<?> domainType;

	/**
	 * Creates a new {@link ReturnedType} for the given returned type, domain type and {@link ProjectionFactory}.
	 * 
	 * @param returnedType must not be {@literal null}.
	 * @param domainType must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 * @return
	 */
	static ReturnedType of(Class<?> returnedType, Class<?> domainType, ProjectionFactory factory) {

		Assert.notNull(returnedType, "Returned type must not be null!");
		Assert.notNull(domainType, "Domain type must not be null!");
		Assert.notNull(factory, "ProjectionFactory must not be null!");

		return (ReturnedType) (returnedType.isInterface()
				? new ReturnedInterface(factory.getProjectionInformation(returnedType), domainType)
				: new ReturnedClass(returnedType, domainType));
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
	public final boolean isInstance(Object source) {
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

			Assert.notNull(information, "Projection information must not be null!");

			this.information = information;
			this.domainType = domainType;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedTypeInformation#getReturnedType()
		 */
		@Override
		public Class<?> getReturnedType() {
			return information.getType();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ReturnedType#needsCustomConstruction()
		 */
		public boolean needsCustomConstruction() {
			return isProjecting() && information.isClosed();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedType#isProjecting()
		 */
		@Override
		public boolean isProjecting() {
			return !information.getType().isAssignableFrom(domainType);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedTypeInformation#getTypeToRead()
		 */
		@Override
		public Class<?> getTypeToRead() {
			return isProjecting() && information.isClosed() ? null : domainType;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedTypeInformation#getInputProperties()
		 */
		@Override
		public List<String> getInputProperties() {

			List<String> properties = new ArrayList<String>();

			for (PropertyDescriptor descriptor : information.getInputProperties()) {
				properties.add(descriptor.getName());
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

		private static final Set<Class<?>> VOID_TYPES = new HashSet<Class<?>>(Arrays.asList(Void.class, void.class));

		private final Class<?> type;
		private final List<String> inputProperties;

		/**
		 * Creates a new {@link ReturnedClass} instance for the given returned type and domain type.
		 * 
		 * @param returnedType must not be {@literal null}.
		 * @param domainType must not be {@literal null}.
		 * @param projectionInformation
		 */
		public ReturnedClass(Class<?> returnedType, Class<?> domainType) {

			super(domainType);

			Assert.notNull(returnedType, "Returned type must not be null!");
			Assert.notNull(domainType, "Domain type must not be null!");
			Assert.isTrue(!returnedType.isInterface(), "Returned type must not be an interface!");

			this.type = returnedType;
			this.inputProperties = detectConstructorParameterNames(returnedType);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedTypeInformation#getReturnedType()
		 */
		@Override
		public Class<?> getReturnedType() {
			return type;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedType#getTypeToRead()
		 */
		public Class<?> getTypeToRead() {
			return type;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedType#isProjecting()
		 */
		@Override
		public boolean isProjecting() {
			return isDto();
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedType#needsCustomConstruction()
		 */
		public boolean needsCustomConstruction() {
			return isDto() && !inputProperties.isEmpty();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ResultFactory.ReturnedTypeInformation#getInputProperties()
		 */
		@Override
		public List<String> getInputProperties() {
			return inputProperties;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private List<String> detectConstructorParameterNames(Class<?> type) {

			if (!isDto()) {
				return Collections.emptyList();
			}

			PreferredConstructorDiscoverer<?, ?> discoverer = new PreferredConstructorDiscoverer(type);

			return discoverer.getConstructor().map(it -> {

				return it.getParameters().stream()//
						.flatMap(parameter -> Optionals.toStream(parameter.getName()))//
						.collect(Collectors.toList());

			}).orElse(Collections.emptyList());
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
}
