/*
 * Copyright 2011-2018 the original author or authors.
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

import static org.springframework.util.ObjectUtils.*;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Special {@link TypeDiscoverer} to determine the actual type for a {@link TypeVariable}. Will consider the context the
 * {@link TypeVariable} is being used in.
 *
 * @author Oliver Gierke
 */
class TypeVariableTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

	private final TypeVariable<?> variable;

	/**
	 * Creates a new {@link TypeVariableTypeInformation} for the given {@link TypeVariable} owning {@link Type} and parent
	 * {@link TypeDiscoverer}.
	 *
	 * @param variable must not be {@literal null}
	 * @param owningType must not be {@literal null}
	 * @param parent
	 */
	public TypeVariableTypeInformation(TypeVariable<?> variable, TypeDiscoverer<?> parent) {

		super(variable, parent);

		Assert.notNull(variable, "TypeVariable must not be null!");

		this.variable = variable;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(@Nullable Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof TypeVariableTypeInformation)) {
			return false;
		}

		TypeVariableTypeInformation<?> that = (TypeVariableTypeInformation<?>) obj;

		return getType().equals(that.getType());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.util.ParentTypeAwareTypeInformation#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;

		result += 31 * nullSafeHashCode(getType());

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return variable.getName();
	}
}
