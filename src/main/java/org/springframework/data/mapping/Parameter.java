/*
 * Copyright 2021-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Value object to represent constructor parameters.
 *
 * @param <T> the type of the parameter
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Chris Bono
 */
public class Parameter<T, P extends PersistentProperty<P>> {

	private final @Nullable String name;
	private final TypeInformation<T> type;
	private final MergedAnnotations annotations;
	private final @Nullable String expression;
	private final @Nullable PersistentEntity<T, P> entity;

	private final Lazy<Boolean> enclosingClassCache;
	private final Lazy<Boolean> hasExpression;

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
		this.expression = getValue(this.annotations);
		this.entity = entity;

		this.enclosingClassCache = Lazy.of(() -> {

			if (entity == null) {
				throw new IllegalStateException();
			}

			Class<T> owningType = entity.getType();
			return ClassUtils.isInnerClass(owningType) && type.getType().equals(owningType.getEnclosingClass());
		});

		this.hasExpression = Lazy.of(() -> StringUtils.hasText(getValueExpression()));
	}

	private static @Nullable String getValue(MergedAnnotations annotations) {

		return annotations.get(Value.class) //
				.getValue("value", String.class) //
				.filter(StringUtils::hasText) //
				.orElse(null);
	}

	/**
	 * Returns the name of the parameter (through constructor/method parameter naming).
	 *
	 * @return the name of the parameter.
	 * @see org.springframework.core.ParameterNameDiscoverer
	 */
	public @Nullable String getName() {
		return name;
	}

	/**
	 * Returns whether the parameter has a name.
	 *
	 * @return whether the parameter has a name.
	 * @since 3.5
	 */
	public boolean hasName() {
		return this.name != null;
	}

	/**
	 * Returns the required name of the parameter (through constructor/method parameter naming) or throws
	 * {@link IllegalStateException} if the parameter has no name.
	 *
	 * @return the parameter name or throws {@link IllegalStateException} if the parameter does not have a name.
	 * @since 3.5
	 * @see org.springframework.core.ParameterNameDiscoverer
	 */
	@SuppressWarnings("NullAway")
	public String getRequiredName() {

		if (!hasName()) {
			throw new IllegalStateException("No name associated with this parameter");
		}

		return getName();
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
	 * Returns the expression to be used when looking up a source data structure to populate the actual parameter value.
	 *
	 * @return the expression to be used when looking up a source data structure.
	 */
	@Nullable
	public String getSpelExpression() {
		return getValueExpression();
	}

	/**
	 * Returns the expression to be used when looking up a source data structure to populate the actual parameter value.
	 *
	 * @return the expression to be used when looking up a source data structure.
	 * @since 3.3
	 */
	@Nullable
	public String getValueExpression() {
		return expression;
	}

	/**
	 * Returns the required expression to be used when looking up a source data structure to populate the actual parameter
	 * value or throws {@link IllegalStateException} if there's no expression.
	 *
	 * @return the expression to be used when looking up a source data structure.
	 * @since 3.3
	 */
	@SuppressWarnings({ "DataFlowIssue", "NullAway" })
	public String getRequiredValueExpression() {

		if (!hasValueExpression()) {
			throw new IllegalStateException("No expression associated with this parameter");
		}

		return getValueExpression();
	}

	/**
	 * Returns whether the constructor parameter is equipped with a value expression.
	 *
	 * @return {@literal true}} if the parameter is equipped with a value expression.
	 * @since 3.3
	 */
	public boolean hasValueExpression() {
		return this.hasExpression.get();
	}

	@Override
	public boolean equals(@Nullable Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Parameter<?, ?> that)) {
			return false;
		}

		return Objects.equals(this.name, that.name) && Objects.equals(this.type, that.type)
				&& Objects.equals(this.expression, that.expression) && Objects.equals(this.entity, that.entity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type, expression, entity);
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

	/**
	 * Returns whether this parameter is a candidate for the enclosing class by checking the parameter type against
	 * {@link Class#getEnclosingClass()} and whether the defining class is an inner non-static one.
	 * <p>
	 * Note that for a proper check the parameter position must be compared to ensure that only the first parameter (at
	 * index {@code 0}/zero) qualifies as enclosing class instance parameter.
	 *
	 * @return {@literal true} if this parameter is a candidate for the enclosing class instance.
	 */
	boolean isEnclosingClassParameter() {
		return enclosingClassCache.get();
	}
}
