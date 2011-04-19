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
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
public class PreferredConstructor<T> {

	protected Constructor<T> constructor;
	protected List<Parameter> parameters = new LinkedList<Parameter>();

	public PreferredConstructor(Constructor<T> constructor) {
		this.constructor = constructor;
	}

	public Constructor<T> getConstructor() {
		return constructor;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void addParameter(String name, Class<?> type, Class<?> rawType, Annotation[] annotations) {
		parameters.add(new Parameter(name, type, rawType, annotations));
	}

	public static class Parameter {
		protected final String name;
		protected final Class<?> type;
		protected final Class<?> rawType;
		protected final Annotation[] annotations;
		protected Value value;

		public Parameter(String name, Class<?> type, Class<?> rawType, Annotation[] annotations) {
			this.name = name;
			this.type = type;
			this.rawType = rawType;
			this.annotations = annotations;
			for (Annotation anno : annotations) {
				if (anno.annotationType() == Value.class) {
					this.value = (Value) anno;
					break;
				}
			}
		}

		public String getName() {
			return name;
		}

		public Class<?> getType() {
			return type;
		}

		public Class<?> getRawType() {
			return rawType;
		}

		public Annotation[] getAnnotations() {
			return annotations;
		}

		public Value getValue() {
			return value;
		}
	}

	public static interface ParameterValueProvider {
		Object getParameterValue(Parameter parameter);
	}

}
