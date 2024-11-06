/*
 * Copyright 2015-2024 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.PropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;

/**
 * Builder assembling {@link Predicate} out of {@link PropertyValues}.
 *
 * @author Christoph Strobl
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Johannes Englmeier
 * @since 1.11
 */
public class QuerydslPredicateBuilder {

	private final ConversionService conversionService;
	private final MultiValueBinding<Path<? extends Object>, Object> defaultBinding;
	private final Map<PathInformation, Path<?>> paths;
	private final EntityPathResolver resolver;

	/**
	 * Creates a new {@link QuerydslPredicateBuilder} for the given {@link ConversionService} and
	 * {@link EntityPathResolver}.
	 *
	 * @param conversionService must not be {@literal null}.
	 * @param resolver can be {@literal null}.
	 */
	public QuerydslPredicateBuilder(ConversionService conversionService, EntityPathResolver resolver) {

		Assert.notNull(conversionService, "ConversionService must not be null");

		this.defaultBinding = new QuerydslDefaultBinding();
		this.conversionService = conversionService;
		this.paths = new ConcurrentHashMap<>();
		this.resolver = resolver;
	}

	/**
	 * Creates a Querydsl {@link Predicate} for the given values, {@link QuerydslBindings} on the given
	 * {@link TypeInformation}.
	 *
	 * @param type the type to create a predicate for.
	 * @param values the values to bind.
	 * @param bindings the {@link QuerydslBindings} for the predicate.
	 * @return the {@link Predicate}.
	 */
	public Predicate getPredicate(TypeInformation<?> type, MultiValueMap<String, ?> values, QuerydslBindings bindings) {

		Assert.notNull(bindings, "Context must not be null");

		BooleanBuilder builder = new BooleanBuilder();

		if (values.isEmpty()) {
			return getPredicate(builder);
		}

		for (var entry : values.entrySet()) {

			if (isSingleElementCollectionWithEmptyItem(entry.getValue())) {
				continue;
			}

			String path = entry.getKey();

			if (!bindings.isPathAvailable(path, type)) {
				continue;
			}

			PathInformation propertyPath = bindings.getPropertyPath(path, type);

			if (propertyPath == null) {
				continue;
			}

			Collection<Object> value = convertToPropertyPathSpecificType(entry.getValue(), propertyPath);
			Optional<Predicate> predicate = invokeBinding(propertyPath, bindings, value);

			predicate.ifPresent(builder::and);
		}

		return getPredicate(builder);
	}

	/**
	 * Returns whether the given {@link Predicate} represents an empty predicate instance.
	 *
	 * @param predicate
	 * @return
	 * @since 2.5.3
	 * @see BooleanBuilder
	 */
	public static boolean isEmpty(Predicate predicate) {
		return new BooleanBuilder().equals(predicate);
	}

	/**
	 * Invokes the binding of the given values, for the given {@link PropertyPath} and {@link QuerydslBindings}.
	 *
	 * @param dotPath must not be {@literal null}.
	 * @param bindings must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 * @return
	 */
	private Optional<Predicate> invokeBinding(PathInformation dotPath, QuerydslBindings bindings,
			Collection<Object> values) {

		Path<?> path = getPath(dotPath, bindings);

		return bindings.getBindingForPath(dotPath).orElse(defaultBinding).bind(path, values);
	}

	/**
	 * Returns the {@link Path} for the given {@link PropertyPath} and {@link QuerydslBindings}. Will try to obtain the
	 * {@link Path} from the bindings first but fall back to reifying it from the PropertyPath in case no specific binding
	 * has been configured.
	 *
	 * @param path must not be {@literal null}.
	 * @param bindings must not be {@literal null}.
	 * @return
	 */
	private Path<?> getPath(PathInformation path, QuerydslBindings bindings) {

		Optional<Path<?>> resolvedPath = bindings.getExistingPath(path);

		return resolvedPath.orElseGet(() -> paths.computeIfAbsent(path, it -> it.reifyPath(resolver)));
	}

	/**
	 * Converts the given source values into a collection of elements that are of the given {@link PropertyPath}'s type.
	 * Considers a single element list with an empty object an empty collection because this basically indicates the
	 * property having been submitted but no value provided.
	 *
	 * @param source must not be {@literal null}.
	 * @param path must not be {@literal null}.
	 * @return
	 */
	private Collection<Object> convertToPropertyPathSpecificType(List<?> source, PathInformation path) {

		if (source.isEmpty() || isSingleElementCollectionWithEmptyItem(source)) {
			return Collections.emptyList();
		}

		TypeDescriptor targetType = getTargetTypeDescriptor(path);
		Collection<Object> target = new ArrayList<>(source.size());

		for (Object value : source) {
			target.add(getValue(targetType, value));
		}

		return target;
	}

	@Nullable
	private Object getValue(TypeDescriptor targetType, Object value) {

		if (ClassUtils.isAssignableValue(targetType.getType(), value)) {
			return value;
		}

		if (conversionService.canConvert(value.getClass(), targetType.getType())) {
			return conversionService.convert(value, TypeDescriptor.forObject(value), targetType);
		}

		return value;
	}

	/**
	 * Returns the target {@link TypeDescriptor} for the given {@link PathInformation} by either inspecting the field or
	 * property (the latter preferred) to pick up annotations potentially defined for formatting purposes.
	 *
	 * @param path must not be {@literal null}.
	 * @return
	 */
	private static TypeDescriptor getTargetTypeDescriptor(PathInformation path) {

		PropertyDescriptor descriptor = path.getLeafPropertyDescriptor();

		Class<?> owningType = path.getLeafParentType();
		String leafProperty = path.getLeafProperty();

		TypeDescriptor result = descriptor == null //
				? TypeDescriptor
						.nested(org.springframework.data.util.ReflectionUtils.getRequiredField(owningType, leafProperty), 0)
				: TypeDescriptor
						.nested(new Property(owningType, descriptor.getReadMethod(), descriptor.getWriteMethod(), leafProperty), 0);

		if (result == null) {
			throw new IllegalStateException(String.format("Could not obtain TypeDescriptor for PathInformation %s", path));
		}

		return result;
	}

	/**
	 * Returns whether the given collection has exactly one element that is empty (i.e. doesn't contain text). This is
	 * basically an indicator that a request parameter has been submitted but no value for it.
	 *
	 * @param source must not be {@literal null}.
	 * @return
	 */
	private static boolean isSingleElementCollectionWithEmptyItem(List<?> source) {
		return source.size() == 1 && ObjectUtils.isEmpty(source.get(0));
	}

	/**
	 * Returns the {@link Predicate} from {@link BooleanBuilder}.
	 *
	 * @param builder
	 * @return
	 */
	private static Predicate getPredicate(BooleanBuilder builder) {

		Predicate predicate = builder.getValue();
		return predicate == null ? new BooleanBuilder() : predicate;
	}
}
