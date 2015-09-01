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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.StringPath;

/**
 * Unit tests for {@link QuerydslBindingsFactory}.
 * 
 * @author Oliver Gierke
 * @soundtrack Miles Davis - All Blues (Kind of Blue)
 */
public class QuerydslBindingsFactoryUnitTests {

	static final TypeInformation<?> USER_TYPE = ClassTypeInformation.from(User.class);

	public @Rule ExpectedException exception = ExpectedException.none();

	QuerydslBindingsFactory factory;

	@Before
	public void setUp() {
		this.factory = new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void createBindingsShouldHonorQuerydslBinderCustomizerHookWhenPresent() {

		Repositories repositories = mock(Repositories.class);

		when(repositories.hasRepositoryFor(User.class)).thenReturn(true);
		when(repositories.getRepositoryFor(User.class)).thenReturn(new SampleRepo());

		QuerydslBindingsFactory factory = new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
		ReflectionTestUtils.setField(factory, "repositories", repositories);

		QuerydslBindings bindings = factory.createBindingsFor(null, USER_TYPE);
		MultiValueBinding<Path<Object>, Object> binding = bindings
				.getBindingForPath(PropertyPath.from("firstname", User.class));

		assertThat(binding.bind((Path) QUser.user.firstname, Collections.singleton("rand")),
				is((Predicate) QUser.user.firstname.contains("rand")));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void shouldReuseExistingQuerydslBinderCustomizer() {

		AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
		when(beanFactory.getBean(SpecificBinding.class)).thenReturn(new SpecificBinding());

		QuerydslBindingsFactory factory = new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE);
		ReflectionTestUtils.setField(factory, "beanFactory", beanFactory);

		QuerydslBindings bindings = factory.createBindingsFor(SpecificBinding.class, USER_TYPE);
		MultiValueBinding<Path<Object>, Object> binding = bindings
				.getBindingForPath(PropertyPath.from("firstname", User.class));

		assertThat(binding.bind((Path) QUser.user.firstname, Collections.singleton("rand")),
				is((Predicate) QUser.user.firstname.eq("RAND")));
	}

	/**
	 * @see DATACMNS-669
	 */
	@Test
	public void rejectsPredicateResolutionIfDomainTypeCantBeAutoDetected() {

		exception.expect(IllegalStateException.class);
		exception.expectMessage(QuerydslPredicate.class.getSimpleName());
		exception.expectMessage("root");

		factory.createBindingsFor(null, ClassTypeInformation.from(ModelAndView.class));
	}

	static class SpecificBinding implements QuerydslBinderCustomizer<QUser> {

		public void customize(QuerydslBindings bindings, QUser user) {

			bindings.bind(user.firstname).first(new SingleValueBinding<StringPath, String>() {

				@Override
				public Predicate bind(StringPath path, String value) {
					return path.eq(value.toUpperCase());
				}
			});

			bindings.bind(user.lastname).first(new SingleValueBinding<StringPath, String>() {

				@Override
				public Predicate bind(StringPath path, String value) {
					return path.toLowerCase().eq(value);
				}
			});

			bindings.excluding(user.address);
		}
	}

	public static class SampleRepo implements QuerydslBinderCustomizer<QUser> {

		@Override
		public void customize(QuerydslBindings bindings, QUser user) {

			bindings.bind(QUser.user.firstname).first(new SingleValueBinding<StringPath, String>() {

				@Override
				public Predicate bind(StringPath path, String value) {
					return path.contains(value);
				}
			});
		}
	}
}
