/*
 * Copyright 2015-2017 the original author or authors.
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
package org.springframework.data.projection;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.beans.BeanUtils;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.data.type.MethodsMetadata;
import org.springframework.data.type.MethodsMetadataReader;
import org.springframework.data.type.classreading.MethodsMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link ProjectionInformation}. Exposes all properties of the type as required input
 * properties.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.12
 */
class DefaultProjectionInformation implements ProjectionInformation {

	private final Class<?> projectionType;
	private final List<PropertyDescriptor> properties;

	/**
	 * Creates a new {@link DefaultProjectionInformation} for the given type.
	 *
	 * @param type must not be {@literal null}.
	 */
	DefaultProjectionInformation(Class<?> type) {

		Assert.notNull(type, "Projection type must not be null!");

		this.projectionType = type;
		this.properties = collectDescriptors(type);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.ProjectionInformation#getType()
	 */
	@Override
	public Class<?> getType() {
		return projectionType;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.ProjectionInformation#getInputProperties()
	 */
	public List<PropertyDescriptor> getInputProperties() {

		return properties.stream()//
				.filter(this::isInputProperty)//
				.distinct()//
				.collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.ProjectionInformation#isDynamic()
	 */
	@Override
	public boolean isClosed() {
		return this.properties.equals(getInputProperties());
	}

	/**
	 * Returns whether the given {@link PropertyDescriptor} describes an input property for the projection, i.e. a
	 * property that needs to be present on the source to be able to create reasonable projections for the type the
	 * descriptor was looked up on.
	 *
	 * @param descriptor will never be {@literal null}.
	 * @return
	 */
	protected boolean isInputProperty(PropertyDescriptor descriptor) {
		return true;
	}

	/**
	 * Collects {@link PropertyDescriptor}s for all properties exposed by the given type and all its super interfaces.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	private static List<PropertyDescriptor> collectDescriptors(Class<?> type) {

		List<PropertyDescriptor> result = new ArrayList<>();

		Optional<MethodsMetadata> metadata = getMetadata(type);
		Stream<PropertyDescriptor> stream = Arrays.stream(BeanUtils.getPropertyDescriptors(type))//
				.filter(it -> !hasDefaultGetter(it));

		Stream<PropertyDescriptor> streamToUse = metadata.map(DefaultProjectionInformation::getMethodOrder)
				.filter(it -> !it.isEmpty()) //
				.map(it -> stream.filter(descriptor -> it.containsKey(descriptor.getReadMethod().getName()))
						.sorted(Comparator.comparingInt(left -> it.get(left.getReadMethod().getName())))) //
				.orElse(stream);

		result.addAll(streamToUse.collect(Collectors.toList()));

		if (metadata.isPresent()) {

			Stream<String> interfaceNames = metadata.map(ClassMetadata::getInterfaceNames) //
					.map(Arrays::stream) //
					.orElse(Stream.empty());

			result.addAll(interfaceNames.map(it -> loadClass(it, type.getClassLoader())) //
					.map(DefaultProjectionInformation::collectDescriptors) //
					.flatMap(List::stream) //
					.collect(Collectors.toList()));
		} else {

			for (Class<?> interfaze : type.getInterfaces()) {
				result.addAll(collectDescriptors(interfaze));
			}
		}

		return result.stream().distinct().collect(Collectors.toList());
	}

	private static Class<?> loadClass(String className, ClassLoader classLoader) {

		try {
			return ClassUtils.forName(className, classLoader);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(String.format("Cannot load class %s", className));
		}
	}

	/**
	 * Returns a {@link Map} containing method name to its positional index according to {@link MethodsMetadata}.
	 *
	 * @param metadata
	 * @return
	 */
	private static Map<String, Integer> getMethodOrder(MethodsMetadata metadata) {

		List<String> methods = metadata.getMethods() //
				.stream() //
				.map(MethodMetadata::getMethodName) //
				.distinct() //
				.collect(Collectors.toList());

		return IntStream.range(0, methods.size()) //
				.boxed() //
				.collect(Collectors.toMap(methods::get, i -> i));
	}

	/**
	 * Attempts to obtain {@link MethodsMetadata} from {@link Class}. Returns {@link Optional} containing
	 * {@link MethodsMetadata} if metadata was read successfully, {@link Optional#empty()} otherwise.
	 *
	 * @param type must not be {@literal null}.
	 * @return the optional {@link MethodsMetadata}.
	 */
	private static Optional<MethodsMetadata> getMetadata(Class<?> type) {

		try {

			MethodsMetadataReaderFactory factory = new MethodsMetadataReaderFactory(type.getClassLoader());
			MethodsMetadataReader metadataReader = factory.getMetadataReader(ClassUtils.getQualifiedName(type));
			return Optional.of(metadataReader.getMethodsMetadata());
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	/**
	 * Returns whether the given {@link PropertyDescriptor} has a getter that is a Java 8 default method.
	 *
	 * @param descriptor must not be {@literal null}.
	 * @return
	 */
	private static boolean hasDefaultGetter(PropertyDescriptor descriptor) {

		Method method = descriptor.getReadMethod();
		return method == null ? false : method.isDefault();
	}
}
