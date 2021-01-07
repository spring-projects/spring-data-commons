/*
 * Copyright 2018-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.With;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;

/**
 * Unit tests for {@link PersistentPropertyAccessor}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class PersistentPropertyAccessorUnitTests {

	SampleMappingContext context = new SampleMappingContext();

	PersistentPropertyPath<? extends PersistentProperty<?>> path;
	PersistentPropertyAccessor accessor;

	private void setUp(Order order, String path) {

		this.accessor = context.getPersistentEntity(Order.class).getPropertyAccessor(order);
		this.path = context.getPersistentPropertyPath(path, Order.class);
	}

	@Test // DATACMNS-1275
	public void looksUpValueForPropertyPath() {

		Order order = new Order(new Customer("Dave"));

		setUp(order, "customer.firstname");

		assertThat(accessor.getProperty(path)).isEqualTo("Dave");
	}

	@Test // DATACMNS-1275
	public void setsPropertyOnNestedPath() {

		Customer customer = new Customer("Dave");
		Order order = new Order(customer);

		setUp(order, "customer.firstname");

		accessor.setProperty(path, "Oliver August");

		assertThat(customer.firstname).isEqualTo("Oliver August");
	}

	@Test // DATACMNS-1275
	public void rejectsEmptyPathToSetValues() {

		setUp(new Order(null), "");

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> accessor.setProperty(path, "Oliver August"));
	}

	@Test // DATACMNS-1275
	public void rejectsIntermediateNullValuesForRead() {

		setUp(new Order(null), "customer.firstname");

		assertThatExceptionOfType(MappingException.class)//
				.isThrownBy(() -> accessor.getProperty(path));
	}

	@Test // DATACMNS-1275
	public void rejectsIntermediateNullValuesForWrite() {

		setUp(new Order(null), "customer.firstname");

		assertThatExceptionOfType(MappingException.class)//
				.isThrownBy(() -> accessor.setProperty(path, "Oliver August"));
	}

	@Test // DATACMNS-1322
	public void correctlyReplacesObjectInstancesWhenSettingPropertyPathOnImmutableObjects() {

		PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(Outer.class);
		PersistentPropertyPath<SamplePersistentProperty> path = context.getPersistentPropertyPath("immutable.value",
				entity.getType());

		NestedImmutable immutable = new NestedImmutable("foo");
		Outer outer = new Outer(immutable);

		PersistentPropertyAccessor accessor = entity.getPropertyAccessor(outer);
		accessor.setProperty(path, "bar");

		Object result = accessor.getBean();

		assertThat(result).isInstanceOfSatisfying(Outer.class, it -> {
			assertThat(it.immutable).isNotSameAs(immutable);
			assertThat(it).isNotSameAs(outer);
		});
	}

	@Test // DATACMNS-1377
	public void shouldConvertToPropertyPathLeafType() {

		Order order = new Order(new Customer("1"));

		PersistentPropertyAccessor<Order> accessor = context.getPersistentEntity(Order.class).getPropertyAccessor(order);
		ConvertingPropertyAccessor<Order> convertingAccessor = new ConvertingPropertyAccessor<>(accessor,
				new DefaultConversionService());

		PersistentPropertyPath<SamplePersistentProperty> path = context.getPersistentPropertyPath("customer.firstname",
				Order.class);

		convertingAccessor.setProperty(path, 2);

		assertThat(convertingAccessor.getBean().getCustomer().getFirstname()).isEqualTo("2");
	}

	@Test // DATACMNS-1555
	public void usesTraversalContextToTraverseCollections() {

		WithContext withContext = WithContext.builder() //
				.collection(Collections.singleton("value")) //
				.list(Collections.singletonList("value")) //
				.set(Collections.singleton("value")) //
				.map(Collections.singletonMap("key", "value")) //
				.string(" value ") //
				.build();

		Spec collectionHelper = Spec.of("collection",
				(context, property) -> context.registerCollectionHandler(property, it -> it.iterator().next()));
		Spec listHelper = Spec.of("list", (context, property) -> context.registerListHandler(property, it -> it.get(0)));
		Spec setHelper = Spec.of("set",
				(context, property) -> context.registerSetHandler(property, it -> it.iterator().next()));
		Spec mapHelper = Spec.of("map", (context, property) -> context.registerMapHandler(property, it -> it.get("key")));
		Spec stringHelper = Spec.of("string",
				(context, property) -> context.registerHandler(property, String.class, it -> it.trim()));

		Stream.of(collectionHelper, listHelper, setHelper, mapHelper, stringHelper).forEach(it -> {

			PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(WithContext.class);
			PersistentProperty<?> property = entity.getRequiredPersistentProperty(it.name);
			PersistentPropertyAccessor<WithContext> accessor = entity.getPropertyAccessor(withContext);

			TraversalContext traversalContext = it.registrar.apply(new TraversalContext(), property);

			PersistentPropertyPath<SamplePersistentProperty> propertyPath = context.getPersistentPropertyPath(it.name,
					WithContext.class);

			assertThat(accessor.getProperty(propertyPath, traversalContext)).isEqualTo("value");
		});
	}

	@Test // DATACMNS-1555
	public void traversalContextRejectsInvalidPropertyHandler() {

		PersistentEntity<Object, SamplePersistentProperty> entity = context.getPersistentEntity(WithContext.class);
		PersistentProperty<?> property = entity.getRequiredPersistentProperty("collection");

		TraversalContext traversal = new TraversalContext();

		assertThatIllegalArgumentException() //
				.isThrownBy(() -> traversal.registerHandler(property, Map.class, Function.identity()));
	}

	@Value
	static class Order {
		Customer customer;
	}

	@Data
	@AllArgsConstructor
	static class Customer {
		String firstname;
	}

	// DATACMNS-1322

	@Value
	@With(AccessLevel.PACKAGE)
	static class NestedImmutable {
		String value;
	}

	@Value
	@With(AccessLevel.PACKAGE)
	static class Outer {
		NestedImmutable immutable;
	}

	// DATACMNS-1555

	@Builder
	static class WithContext {

		Collection<String> collection;
		List<String> list;
		Set<String> set;
		Map<String, String> map;
		String string;
	}

	@Value(staticConstructor = "of")
	static class Spec {

		String name;
		BiFunction<TraversalContext, PersistentProperty<?>, TraversalContext> registrar;
	}
}
