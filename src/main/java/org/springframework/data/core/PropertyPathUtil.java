/*
 * Copyright 2025 the original author or authors.
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

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Utility class for {@link PropertyPath} implementations.
 *
 * @author Mark Paluch
 * @since 4.1
 */
class PropertyPathUtil {

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
	 * Equality check for {@link PropertyPath} implementations based on their owning type and string representation.
	 *
	 * @param self the property path.
	 * @param o the other object.
	 * @return {@literal true} if both are equal; {@literal false} otherwise.
	 */
	public static boolean equals(PropertyPath self, @Nullable Object o) {

		if (self == o) {
			return true;
		}

		if (!(o instanceof PropertyPath that)) {
			return false;
		}

		return Objects.equals(self.getOwningType(), that.getOwningType())
				&& Objects.equals(self.toString(), that.toString());
	}

}
