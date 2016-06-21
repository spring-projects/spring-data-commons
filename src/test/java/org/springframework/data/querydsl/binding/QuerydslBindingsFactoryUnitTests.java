/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.data.querydsl.binding;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.ModelAndView;

import com.querydsl.core.types.Path;

/**
 * Unit tests for {@link QuerydslBindingsFactory}.
 * 
 * @author Oliver Gierke
 * @soundtrack Miles Davis - All Blues (Kind of Blue)
 */
public class QuerydslBindingsFactoryUnitTests {

	static final TypeInformation<?> USER_TYPE = ClassTypeInformation.from(User.class);

	QuerydslBindingsFactory factory;

	@Before
	public void setUp() {
		this.factory = new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void createBindingsShouldHonorQuerydslBinderCustomizerHookWhenPresent() {

		Repositories repositories = mock(Repositories.class);

		when(repositories.hasRepositoryFor(User.class)).thenReturn(true);
		when(repositories.getRepositoryFor(User.class)).thenReturn(Optional.of(new SampleRepo()));

		QuerydslBindingsFactory factory = new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
		ReflectionTestUtils.setField(factory, "repositories", Optional.of(repositories));

		QuerydslBindings bindings = factory.createBindingsFor(USER_TYPE, Optional.empty());
		Optional<MultiValueBinding<Path<String>, String>> binding = bindings
				.getBindingForPath(PropertyPath.from("firstname", User.class));

		assertThat(binding).hasValueSatisfying(it -> {
			assertThat(it.bind(QUser.user.firstname, Collections.singleton("rand")))
					.hasValue(QUser.user.firstname.contains("rand"));
		});
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void shouldReuseExistingQuerydslBinderCustomizer() {

		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		when(beanFactory.getBean(SpecificBinding.class)).thenReturn(new SpecificBinding());

		QuerydslBindingsFactory factory = new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
		ReflectionTestUtils.setField(factory, "beanFactory", Optional.of(beanFactory));

		QuerydslBindings bindings = factory.createBindingsFor(USER_TYPE, Optional.of(SpecificBinding.class));
		Optional<MultiValueBinding<Path<String>, String>> binding = bindings
				.getBindingForPath(PropertyPath.from("firstname", User.class));

		assertThat(binding).hasValueSatisfying(it -> {
			assertThat(it.bind(QUser.user.firstname, Collections.singleton("rand")))
					.hasValue(QUser.user.firstname.eq("RAND"));
		});
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void rejectsPredicateResolutionIfDomainTypeCantBeAutoDetected() {

		assertThatExceptionOfType(IllegalStateException.class)//
				.isThrownBy(() -> factory.createBindingsFor(ClassTypeInformation.from(ModelAndView.class), Optional.empty()))//
				.withMessageContaining(QuerydslPredicate.class.getSimpleName())//
				.withMessageContaining("root");

	}

	static class SpecificBinding implements QuerydslBinderCustomizer<QUser> {

		public void customize(QuerydslBindings bindings, QUser user) {

			bindings.bind(user.firstname).firstOptional((path, value) -> value.map(it -> path.eq(it.toUpperCase())));
			bindings.bind(user.lastname).firstOptional((path, value) -> value.map(it -> path.toLowerCase().eq(it)));

			bindings.excluding(user.address);
		}
	}

	public static class SampleRepo implements QuerydslBinderCustomizer<QUser> {

		@Override
		public void customize(QuerydslBindings bindings, QUser user) {
			bindings.bind(QUser.user.firstname).firstOptional((path, value) -> value.map(it -> path.contains(it)));
		}
	}
}
