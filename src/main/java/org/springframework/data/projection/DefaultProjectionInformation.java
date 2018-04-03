/*
 * Copyright 2015 the original author or authors.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.core.type.MethodMetadata;
import org.springframework.data.type.MethodsMetadata;
import org.springframework.data.type.classreading.MethodsMetadataReader;
import org.springframework.data.type.classreading.MethodsMetadataReaderFactory;
import org.springframework.data.util.ReflectionUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link ProjectionInformation}. Exposes all properties of the type as required input
 * properties.
 * 
 * @author Oliver Gierke
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

		List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>();

		for (PropertyDescriptor descriptor : properties) {
			if (isInputProperty(descriptor)) {
				result.add(descriptor);
			}
		}

		return result;
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

		List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>();
		MethodsMetadata metadata = getMetadata(type);
		final Map<String, Integer> orders = getMethodOrder(metadata);

		for (PropertyDescriptor descriptor : filterDefaultMethods(BeanUtils.getPropertyDescriptors(type))) {

			Method readMethod = descriptor.getReadMethod();

			if (readMethod == null) {
				continue;
			}

			if (metadata == null || orders.containsKey(readMethod.getName())) {
				result.add(descriptor);
			}
		}

		if (metadata == null) {
			return result;
		}

		Collections.sort(result, new Comparator<PropertyDescriptor>() {

			/* 
			 * (non-Javadoc)
			 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
			 */
			@Override
			public int compare(PropertyDescriptor left, PropertyDescriptor right) {
				return orders.get(left.getReadMethod().getName()) - orders.get(right.getReadMethod().getName());
			}
		});

		for (String name : metadata.getInterfaceNames()) {
			result.addAll(collectDescriptors(loadClass(name, type.getClassLoader())));
		}

		return result;
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

		if (metadata == null) {
			return Collections.emptyMap();
		}

		Set<MethodMetadata> methods = metadata.getMethods();
		Map<String, Integer> result = new HashMap<String, Integer>(methods.size());
		int i = 0;

		for (MethodMetadata methodMetadata : methods) {

			String name = methodMetadata.getMethodName();

			if (!result.containsKey(name)) {
				result.put(name, i++);
			}
		}

		return result;
	}

	/**
	 * Attempts to obtain {@link MethodsMetadata} from {@link Class}. Returns {@link Optional} containing
	 * {@link MethodsMetadata} if metadata was read successfully, {@link Optional#empty()} otherwise.
	 *
	 * @param type must not be {@literal null}.
	 * @return the optional {@link MethodsMetadata}.
	 */
	private static MethodsMetadata getMetadata(Class<?> type) {

		try {

			MethodsMetadataReaderFactory factory = new MethodsMetadataReaderFactory(type.getClassLoader());
			MethodsMetadataReader metadataReader = factory.getMetadataReader(ClassUtils.getQualifiedName(type));

			return metadataReader.getMethodsMetadata();

		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns all {@link PropertyDescriptor}s that don't have a Java 8 default method as getter.
	 * 
	 * @param descriptors must not be {@literal null}.
	 * @return
	 */
	private static List<PropertyDescriptor> filterDefaultMethods(PropertyDescriptor[] descriptors) {

		List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>(descriptors.length);

		for (PropertyDescriptor descriptor : descriptors) {
			if (!hasDefaultGetter(descriptor)) {
				result.add(descriptor);
			}
		}

		return result;
	}

	/**
	 * Returns whether the given {@link PropertyDescriptor} has a getter that is a Java 8 default method.
	 * 
	 * @param descriptor must not be {@literal null}.
	 * @return
	 */
	private static boolean hasDefaultGetter(PropertyDescriptor descriptor) {

		Method method = descriptor.getReadMethod();

		return method != null && ReflectionUtils.isDefaultMethod(method);
	}
}
