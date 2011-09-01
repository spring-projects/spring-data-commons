/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Base class for all types that include parameterization of some kind. Crucial as we have to take note of the parent
 * class we will have to resolve generic parameters against.
 * 
 * @author Oliver Gierke
 */
class ParameterizedTypeInformation<T> extends TypeDiscoverer<T> {

	private final TypeDiscoverer<?> parent;

	/**
	 * Creates a new {@link ParameterizedTypeInformation} for the given {@link Type} and parent {@link TypeDiscoverer}.
	 * 
	 * @param type must not be {@literal null}
	 * @param parent must not be {@literal null}
	 */
	public ParameterizedTypeInformation(Type type, TypeDiscoverer<?> parent) {
		super(type, null);
		Assert.notNull(parent);
		this.parent = parent;
	}

	/**
	 * Considers the parent's type variable map before invoking the super class method.
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected Map<TypeVariable, Type> getTypeVariableMap() {

		return parent != null ? parent.getTypeVariableMap() : super.getTypeVariableMap();
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#createInfo(java.lang.reflect.Type)
	 */
	@Override
	protected TypeInformation<?> createInfo(Type fieldType) {
		if (parent.getType().equals(fieldType)) {
			return parent;
		}

		return super.createInfo(fieldType);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (!super.equals(obj)) {
			return false;
		}

		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}

		ParameterizedTypeInformation<?> that = (ParameterizedTypeInformation<?>) obj;
		return this.parent == null ? that.parent == null : this.parent.equals(that.parent);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.util.TypeDiscoverer#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode() + 31 * ObjectUtils.nullSafeHashCode(parent);
	}
}
