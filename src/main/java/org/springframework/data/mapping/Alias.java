/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.mapping;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.Optional;

/**
 * @author Oliver Gierke
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Alias {

	public static Alias NONE = new Alias(Optional.empty());

	Optional<? extends Object> value;

	public static Alias of(Object alias) {
		return ofOptional(Optional.of(alias));
	}

	public static Alias ofOptional(Optional<? extends Object> optional) {
		return optional.isPresent() ? new Alias(optional) : NONE;
	}

	public boolean isPresentButDifferent(Alias other) {
		return isPresent() && !hasValue(other.value);
	}

	public boolean hasValue(Object that) {
		return value.filter(it -> it.equals(that)).isPresent();
	}

	public boolean hasSamePresentValueAs(Alias other) {
		return isPresent() && hasValue(other.value);
	}

	public boolean isPresent() {
		return value.isPresent();
	}

	public <T> Optional<T> mapTyped(Class<T> type) {
		return value.filter(it -> type.isInstance(it)).map(it -> type.cast(it));
	}

	public String toString() {
		return value.map(Object::toString).orElse("NONE");
	}
}
