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
package org.springframework.data.mapping;

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

	/**
	 * Creates a new {@link PreferredConstructor} from the given {@link Constructor} and {@link Parameter}s.
	 * 
	 * @param constructor
	 * @param parameters
	 */
	public PreferredConstructor(Constructor<T> constructor, Parameter<?>... parameters) {

		Assert.notNull(constructor);
		Assert.notNull(parameters);

		ReflectionUtils.makeAccessible(constructor);
		this.constructor = constructor;
		this.parameters = Arrays.asList(parameters);
	}

	/**
	 * Returns the underlying {@link Constructor}.
	 * 
	 * @return
	 */
	public Constructor<T> getConstructor() {
		return constructor;
	}

	/**
	 * Returns the {@link Parameter}s of the constructor.
	 * 
	 * @return
	 */
	public Iterable<Parameter<?>> getParameters() {
		return parameters;
	}

	/**
	 * Returns whether the constructor has {@link Parameter}s.
	 * 
	 * @see #isNoArgConstructor()
	 * @return
	 */
	public boolean hasParameters() {
		return !parameters.isEmpty();
	}

	/**
	 * Returns whether the constructor does not have any arguments.
	 * 
	 * @see #hasParameters()
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

	/**
	 * Value object to represent constructor parameters.
	 * 
	 * @param <T> the type of the paramter
	 * @author Oliver Gierke
	 */
	public static class Parameter<T> {

		private final String name;
		private final TypeInformation<T> type;
		private final String key;

		/**
		 * Creates a new {@link Parameter} with the given name, {@link TypeInformation} as well as an array of
		 * {@link Annotation}s. Will insprect the annotations for an {@link Value} annotation to lookup a key or an SpEL
		 * expression to be evaluated.
		 * 
		 * @param name the name of the parameter, can be {@literal null}
		 * @param type must not be {@literal null}
		 * @param annotations must not be {@literal null} but can be empty
		 */
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

		/**
		 * Returns the name of the parameter or {@literal null} if none was given.
		 * 
		 * @return
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the {@link TypeInformation} of the parameter.
		 * 
		 * @return
		 */
		public TypeInformation<T> getType() {
			return type;
		}

		/**
		 * Returns the raw resolved type of the parameter.
		 * 
		 * @return
		 */
		public Class<T> getRawType() {
			return type.getType();
		}

		/**
		 * Returns the key to be used when looking up a source data structure to populate the actual parameter value.
		 * 
		 * @return
		 */
		public String getKey() {
			return key;
		}
	}
}
