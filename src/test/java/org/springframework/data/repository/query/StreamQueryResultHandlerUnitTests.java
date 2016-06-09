/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.projection.ProjectionFactory;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Test suite of test cases testing the contract and functionality
 * of the {@link StreamQueryResultHandler} class.
 *
 * @author John Blum
 * @since 1.13.0
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamQueryResultHandlerUnitTests {

	@Mock
	private Converter mockConverter;

	@Mock
	private ProjectionFactory mockProjectionFactory;

	protected <T> Set<T> asSet(T... elements) {
		Set<T> set = new HashSet<T>(elements.length);
		Collections.addAll(set, elements);
		return set;
	}

	protected ReturnedType newReturnedType(Class<?> returnType, Class<?> domainType) {
		return ReturnedType.of(returnType, domainType, mockProjectionFactory);
	}

	protected <T> StreamQueryResultHandler<Stream<T>> newStreamQueryResultHandler(Class<T> returnType,
			Class<?> domainType) {

		return new StreamQueryResultHandler<Stream<T>>(newReturnedType(returnType, domainType), mockConverter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void handlesStreamSuccessfully() {
		StreamQueryResultHandler<Stream<Customer>> streamQueryResultHandler =
			newStreamQueryResultHandler(Customer.class, Person.class);

		when(mockConverter.convert(anyObject())).thenAnswer(new Answer<Customer>() {
			@Override
			public Customer answer(InvocationOnMock invocationOnMock) throws Throwable {

				Object argument = invocationOnMock.getArgumentAt(0, Object.class);

				return Customer.from(argument instanceof Person ? (Person) argument
					: Person.fromString(String.valueOf(argument)));
			}
		});

		final Set<?> source = asSet(Customer.as("Jon", "Doe"), "Jack Black",
			Person.as("Jack", "Handy"));

		Stream<Customer> result = streamQueryResultHandler.handle(source.stream());

		assertNotSame(result, source);
		assertThat(result, is(notNullValue(Stream.class)));

		final AtomicInteger count = new AtomicInteger(0);

		assertTrue(result.allMatch(new Predicate<Object>() {

			private final Set<String> expectedCustomerNames = source.stream().map(new Function<Object, String>() {
				@Override public String apply(Object element) {
					return String.valueOf(element);
				}
			}).collect(Collectors.<String>toSet());

			@Override public boolean test(Object element) {
				count.incrementAndGet();
				return (element instanceof Customer && expectedCustomerNames.contains(element.toString()));
			}
		}));

		assertThat(count.get(), is(equalTo(source.size())));

		verify(mockConverter, times(1)).convert(eq("Jack Black"));
		verify(mockConverter, times(1)).convert(eq(Person.as("Jack", "Handy")));
		verify(mockConverter, never()).convert(any(Customer.class));
	}

	@Test(expected = NullPointerException.class)
	public void handleWithNullThrowsNullPointerException() {
		newStreamQueryResultHandler(Person.class, Person.class).handle(null);
	}

	@RequiredArgsConstructor
	@EqualsAndHashCode
	static class Person {

		@NonNull @Getter String firstName;
		@NonNull @Getter String lastName;

		static Person as(String firstName, String lastName) {
			return new Person(firstName, lastName);
		}

		static Person fromString(String value) {
			String[] name = value.split(" ");
			return as(name[0], name[1]);
		}

		@Override
		public String toString() {
			return String.format("%1$s %2$s", getFirstName(), getLastName());
		}
	}

	static class Customer extends Person {

		static Customer as(String firstName, String lastName) {
			return new Customer(firstName, lastName);
		}

		static Customer from(Person person) {
			return new Customer(person.getFirstName(), person.getLastName());
		}

		Customer(String firstName, String lastName) {
			super(firstName, lastName);
		}
	}
}
