/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.data.core;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class for {@link PropertyPath} and {@link PropertyReference} implementations.
 *
 * @author Mark Paluch
 * @since 4.1
 */
public class PropertyPathUtil {

	/**
	 * Resolve a {@link PropertyPath} from a {@link Serializable} lambda implementing a functional interface accepting a
	 * single method argument and returning a value. The form of the interface must follow a design aligned with
	 * {@link org.springframework.core.convert.converter.Converter} or {@link java.util.function.Function}.
	 *
	 * @param obj the serializable lambda object.
	 * @return the resolved property path.
	 */
	public static PropertyPath resolve(Object obj) {

		Assert.isInstanceOf(Serializable.class, obj, "Object must be Serializable");

		return TypedPropertyPaths.of(new SerializableWrapper((Serializable) obj));
	}

	private record SerializableWrapper(Serializable serializable) implements PropertyReference<Object, Object> {

		@Override
		public @Nullable Object get(Object obj) {
			return null;
		}

		// serializable bridge
		public SerializedLambda writeReplace() {

			Method method = ReflectionUtils.findMethod(serializable.getClass(), "writeReplace");

			if (method == null) {
				throw new InvalidDataAccessApiUsageException(
						"Cannot find writeReplace method on " + serializable.getClass().getName());
			}

			ReflectionUtils.makeAccessible(method);
			return (SerializedLambda) ReflectionUtils.invokeMethod(method, serializable);
		}

	}

	/**
	 * Compute the hash code for the given {@link PropertyPath} based on its {@link Object#toString() string}
	 * representation.
	 *
	 * @param path the property path.
	 * @return property path hash code.
	 */
	static int hashCode(PropertyPath path) {
		return path.toString().hashCode();
	}

	/**
	 * Compute the hash code for the given {@link PropertyReference} based on its {@link Object#toString() string}
	 * representation.
	 *
	 * @param property the property reference
	 * @return property reference hash code.
	 */
	static int hashCode(PropertyReference<?, ?> property) {
		return Objects.hash(property.getOwningType(), property.getName());
	}

	/**
	 * Equality check for {@link PropertyPath} implementations based on their owning type and string representation.
	 *
	 * @param self the property path.
	 * @param o the other object.
	 * @return {@literal true} if both are equal; {@literal false} otherwise.
	 */
	static boolean equals(PropertyPath self, @Nullable Object o) {

		if (self == o) {
			return true;
		}

		if (!(o instanceof PropertyPath that)) {
			return false;
		}

		return Objects.equals(self.getOwningType(), that.getOwningType())
				&& Objects.equals(self.toString(), that.toString());
	}

	/**
	 * Equality check for {@link PropertyReference} implementations based on their owning type and name.
	 *
	 * @param self the property path.
	 * @param o the other object.
	 * @return {@literal true} if both are equal; {@literal false} otherwise.
	 */
	static boolean equals(PropertyReference<?, ?> self, @Nullable Object o) {

		if (self == o) {
			return true;
		}

		if (!(o instanceof PropertyReference<?, ?> that)) {
			return false;
		}

		return Objects.equals(self.getOwningType(), that.getOwningType()) && Objects.equals(self.getName(), that.getName());
	}

}
