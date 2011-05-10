/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.mapping.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Value object to encapsulate the constructor to be used when mapping persistent data to objects.
 *
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Oliver Gierke
 */
public class PreferredConstructor<T> {

	private final Constructor<T> constructor;
	private final List<Parameter<?>> parameters;

	public PreferredConstructor(Constructor<T> constructor, Parameter<?>... parameters) {
		ReflectionUtils.makeAccessible(constructor);
		this.constructor = constructor;
		this.parameters = Arrays.asList(parameters);
	}

	public Constructor<T> getConstructor() {
		return constructor;
	}

	public List<Parameter<?>> getParameters() {
		return parameters;
	}

	/**
	 * Returns whether the constructor does not have any arguments.
	 *
	 * @return
	 */
	public boolean isNoArgConstructor() {
		return parameters.isEmpty();
	}

	/**
	 * Returns whether the constructor was explicitly selected (by {@link PersistenceConstructor}).
	 *
	 * @return
	 */
	public boolean isExplicitlyAnnotated() {
		return constructor.isAnnotationPresent(PersistenceConstructor.class);
	}

	public static class Parameter<T> {
		private final String name;
		private final TypeInformation<T> type;
		private final String key;

		public Parameter(String name, TypeInformation<T> type, Annotation[] annotations) {

			Assert.notNull(type);
			Assert.notNull(annotations);

			this.name = name;
			this.type = type;
			this.key = getValue(annotations);
		}

		private String getValue(Annotation[] annotations) {
			for (Annotation anno : annotations) {
				if (anno.annotationType() == Value.class) {
					return ((Value) anno).value();
				}
			}
			return null;
		}

		public String getName() {
			return name;
		}

		public TypeInformation<T> getType() {
			return type;
		}

		public Class<T> getRawType() {
			return type.getType();
		}

		public String getKey() {
			return key;
		}
	}

	public static interface ParameterValueProvider {
		<T> T getParameterValue(Parameter<T> parameter);
	}

}
