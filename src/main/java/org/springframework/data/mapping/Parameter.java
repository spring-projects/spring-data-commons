/*
 * Copyright 2021-2023 the original author or authors.
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
package org.springframework.data.mapping;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to represent constructor parameters.
 *
 * @param <T> the type of the parameter
 * @author Oliver Gierke
 */
public class Parameter<T, P extends PersistentProperty<P>> {

	private final @Nullable String name;
	private final TypeInformation<T> type;
	private final MergedAnnotations annotations;
	private final String key;
	private final @Nullable PersistentEntity<T, P> entity;

	private final Lazy<Boolean> enclosingClassCache;
	private final Lazy<Boolean> hasSpelExpression;

	/**
	 * Creates a new {@link Parameter} with the given name, {@link TypeInformation} as well as an array of
	 * {@link Annotation}s. Will inspect the annotations for an {@link Value} annotation to lookup a key or an SpEL
	 * expression to be evaluated.
	 *
	 * @param name the name of the parameter, can be {@literal null}
	 * @param type must not be {@literal null}
	 * @param annotations must not be {@literal null} but can be empty
	 * @param entity must not be {@literal null}.
	 */
	public Parameter(@Nullable String name, TypeInformation<T> type, Annotation[] annotations,
			@Nullable PersistentEntity<T, P> entity) {

		Assert.notNull(type, "Type must not be null");
		Assert.notNull(annotations, "Annotations must not be null");

		this.name = name;
		this.type = type;
		this.annotations = MergedAnnotations.from(annotations);
		this.key = getValue(this.annotations);
		this.entity = entity;

		this.enclosingClassCache = Lazy.of(() -> {

			if (entity == null) {
				throw new IllegalStateException();
			}

			Class<T> owningType = entity.getType();
			return owningType.isMemberClass() && type.getType().equals(owningType.getEnclosingClass());
		});

		this.hasSpelExpression = Lazy.of(() -> StringUtils.hasText(getSpelExpression()));
	}

	@Nullable
	private static String getValue(MergedAnnotations annotations) {

		return annotations.get(Value.class) //
				.getValue("value", String.class) //
				.filter(StringUtils::hasText) //
				.orElse(null);
	}

	/**
	 * Returns the name of the parameter.
	 *
	 * @return
	 */
	@Nullable
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
	 * Merged annotations that this parameter is annotated with.
	 *
	 * @return
	 * @since 2.5
	 */
	public MergedAnnotations getAnnotations() {
		return annotations;
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
		return this.hasSpelExpression.get();
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Parameter<?, ?> that)) {
			return false;
		}

		return Objects.equals(this.name, that.name)
				&& Objects.equals(this.type, that.type)
				&& Objects.equals(this.key, that.key)
				&& Objects.equals(this.entity, that.entity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, key, entity);
	}

	/**
	 * Returns whether the {@link Parameter} maps the given {@link PersistentProperty}.
	 *
	 * @param property
	 * @return
	 */
	boolean maps(PersistentProperty<?> property) {

		PersistentEntity<T, P> entity = this.entity;
		String name = this.name;

		P referencedProperty = entity == null ? null : name == null ? null : entity.getPersistentProperty(name);

		return property.equals(referencedProperty);
	}

	boolean isEnclosingClassParameter() {
		return enclosingClassCache.get();
	}
}
