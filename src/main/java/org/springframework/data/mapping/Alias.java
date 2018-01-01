/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A container object which may or may not contain a type alias value. If a value is present, {@code isPresent()} will
 * return {@code true} and {@link #getValue()} will return the value.
 * <p/>
 * Additional methods that depend on the presence or absence of a contained value are provided, such as
 * {@link #hasValue(Object)} or {@link #isPresent()}
 * <p/>
 * Aliases are immutable once created.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Alias {

	/**
	 * Common instance for {@code empty()}.
	 */
	@SuppressWarnings("null") //
	public static final Alias NONE = new Alias(null);

	private final Object value;

	/**
	 * Create an {@link Alias} given the {@code alias} object.
	 *
	 * @param alias must not be {@literal null}.
	 * @return the {@link Alias} for {@code alias}.
	 */
	public static Alias of(Object alias) {

		Assert.notNull(alias, "Alias must not be null!");

		return new Alias(alias);
	}

	/**
	 * Create an {@link Alias} from a possibly present {@code alias} object. Using a {@literal null} alias will return
	 * {@link #empty()}.
	 *
	 * @param alias may be {@literal null}.
	 * @return the {@link Alias} for {@code alias} or {@link #empty()} if the given alias was {@literal null}.
	 */
	public static Alias ofNullable(@Nullable Object alias) {
		return alias == null ? NONE : new Alias(alias);
	}

	/**
	 * Returns an empty {@code Alias} instance. No value is present for this Alias.
	 *
	 * @return an empty {@link Alias}.
	 */
	public static Alias empty() {
		return NONE;
	}

	/**
	 * Checks whether this {@link Alias} has a value but is different from the {@code other} value.
	 *
	 * @param other must not be {@literal null}.
	 * @return {@literal true} if this value is present but different from the {@code other} value.
	 */
	public boolean isPresentButDifferent(Alias other) {

		Assert.notNull(other, "Other alias must not be null!");

		return isPresent() && !this.value.equals(other.value);
	}

	/**
	 * Checks whether this {@link Alias} contains the value {@code that}.
	 *
	 * @param that the other value, may be {@literal null}.
	 * @return {@literal true} if this alias has a value and it equals to {@code that}.
	 */
	public boolean hasValue(Object that) {
		return value != null && value.equals(that);
	}

	/**
	 * Returns whether the the current alias is present and has the same value as the given {@link Alias}.
	 *
	 * @param other the other {@link Alias}
	 * @return {@literal true} if there's an alias value present and its equal to the one in the given {@link Alias}.
	 */
	public boolean hasSamePresentValueAs(Alias other) {
		return isPresent() && value.equals(other.value);
	}

	/**
	 * @return {@literal true} if this {@link Alias} contains a value.
	 */
	public boolean isPresent() {
		return value != null;
	}

	/**
	 * Return the value typed to {@code type} if the value is present and assignable to {@code type}.
	 *
	 * @param type must not be {@literal null}.
	 * @return
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T mapTyped(Class<T> type) {

		Assert.notNull(type, "Type must not be null");

		return isPresent() && type.isInstance(value) ? (T) value : null;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return isPresent() ? value.toString() : "NONE";
	}
}
