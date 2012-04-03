/*
 * Copyright 2011-2012 by the original author(s).
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

import static org.springframework.util.ObjectUtils.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Value object to encapsulate the constructor to be used when mapping persistent data to objects.
 * 
 * @author Oliver Gierke
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class PreferredConstructor<T, P extends PersistentProperty<P>> {

	private final Constructor<T> constructor;
	private final List<Parameter<Object, P>> parameters;

	/**
	 * Creates a new {@link PreferredConstructor} from the given {@link Constructor} and {@link Parameter}s.
	 * 
	 * @param constructor
	 * @param parameters
	 */
	public PreferredConstructor(Constructor<T> constructor, Parameter<Object, P>... parameters) {

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
	public Iterable<Parameter<Object, P>> getParameters() {
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
	 * Returns whether the given {@link PersistentProperty} is referenced in a constructor argument of the
	 * {@link PersistentEntity} backing this {@link MappedConstructor}.
	 * 
	 * @param property must not be {@literal null}.
	 * @return
	 */
	public boolean isConstructorParameter(P property) {

		Assert.notNull(property);

		for (Parameter<?, P> parameter : parameters) {
			if (parameter.maps(property)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns whether the given {@link Parameter} is one referring to an enclosing class. That is in case the class this
	 * {@link PreferredConstructor} belongs to is a member class actually. If that's the case the compiler creates a first
	 * constructor argument of the enclosing class type.
	 * 
	 * @param parameter must not be {@literal null}.
	 * @return
	 */
	public boolean isEnclosingClassParameter(Parameter<?, P> parameter) {

		Assert.notNull(parameter);

		if (parameters.isEmpty()) {
			return false;
		}

		return parameters.get(0).equals(parameter) && parameter.isEnclosingClassParameter();
	}

	/**
	 * Value object to represent constructor parameters.
	 * 
	 * @param <T> the type of the parameter
	 * @author Oliver Gierke
	 */
	public static class Parameter<T, P extends PersistentProperty<P>> {

		private final String name;
		private final TypeInformation<T> type;
		private final String key;
		private final PersistentEntity<T, P> entity;

		/**
		 * Creates a new {@link Parameter} with the given name, {@link TypeInformation} as well as an array of
		 * {@link Annotation}s. Will insprect the annotations for an {@link Value} annotation to lookup a key or an SpEL
		 * expression to be evaluated.
		 * 
		 * @param name the name of the parameter, can be {@literal null}
		 * @param type must not be {@literal null}
		 * @param annotations must not be {@literal null} but can be empty
		 * @param entity can be {@literal null}.
		 */
		public Parameter(String name, TypeInformation<T> type, Annotation[] annotations, PersistentEntity<T, P> entity) {

			Assert.notNull(type);
			Assert.notNull(annotations);

			this.name = name;
			this.type = type;
			this.key = getValue(annotations);
			this.entity = entity;
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
		public String getSpelExpression() {
			return key;
		}

		/**
		 * Returns whether the constructor parameter is equipped with a SpEL expression.
		 * 
		 * @return
		 */
		public boolean hasSpelExpression() {
			return StringUtils.hasText(getSpelExpression());
		}

		/**
		 * Returns whether the {@link Parameter} maps the given {@link PersistentProperty}.
		 * 
		 * @param property
		 * @return
		 */
		boolean maps(P property) {

			P referencedProperty = entity == null ? null : entity.getPersistentProperty(name);
			return property == null ? false : property.equals(referencedProperty);
		}

		private boolean isEnclosingClassParameter() {

			Class<T> owningType = entity.getType();
			return owningType.isMemberClass() && type.getType().equals(owningType.getEnclosingClass());
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {

			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Parameter)) {
				return false;
			}

			Parameter<?, ?> that = (Parameter<?, ?>) obj;

			boolean nameEquals = this.name == null ? that.name == null : this.name.equals(that.name);
			boolean keyEquals = this.key == null ? that.key == null : this.key.equals(that.key);
			boolean typeEquals = nullSafeEquals(this.type, that.type);
			boolean entityEquals = this.entity == null ? that.entity == null : this.entity.equals(that.entity);

			return nameEquals && keyEquals && typeEquals && entityEquals;
		}

		/* 
		 * (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {

			int result = 17;

			result += 31 * nullSafeHashCode(this.name);
			result += 31 * nullSafeHashCode(this.key);
			result += 31 * nullSafeHashCode(this.type);
			result += 31 * nullSafeHashCode(this.entity);

			return result;
		}
	}
}
