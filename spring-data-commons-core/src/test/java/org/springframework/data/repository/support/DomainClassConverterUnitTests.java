/*
 * Copyright 2008-2012 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;

/**
 * Unit test for {@link DomainClassConverter}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DomainClassConverterUnitTests {

	static final User USER = new User();

	@SuppressWarnings("rawtypes")
	DomainClassConverter converter;

	TypeDescriptor sourceDescriptor;
	TypeDescriptor targetDescriptor;

	@SuppressWarnings("rawtypes")
	Map<String, RepositoryFactoryInformation> providers;

	@Mock
	ApplicationContext context, parent;
	@Mock
	UserRepository repository;
	@Mock
	DefaultConversionService service;
	@Mock
	EntityInformation<User, Long> information;
	@Mock
	RepositoryFactoryInformation<User, Long> provider;

	@Before
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setUp() {

		converter = new DomainClassConverter(service);
		providers = new HashMap<String, RepositoryFactoryInformation>();

		sourceDescriptor = TypeDescriptor.valueOf(String.class);
		targetDescriptor = TypeDescriptor.valueOf(User.class);

		when(provider.getEntityInformation()).thenReturn(information);
		when(provider.getRepositoryInterface()).thenReturn((Class) UserRepository.class);
		when(information.getJavaType()).thenReturn(User.class);
		when(information.getIdType()).thenReturn(Long.class);
	}

	@Test
	public void matchFailsIfNoDaoAvailable() throws Exception {

		converter.setApplicationContext(context);
		assertMatches(false);
	}

	@Test
	public void matchesIfConversionInBetweenIsPossible() throws Exception {

		letContextContain(context, provider);
		converter.setApplicationContext(context);

		when(service.canConvert(String.class, Long.class)).thenReturn(true);

		assertMatches(true);
	}

	@Test
	public void matchFailsIfNoIntermediateConversionIsPossible() throws Exception {

		letContextContain(context, provider);
		converter.setApplicationContext(context);

		when(service.canConvert(String.class, Long.class)).thenReturn(false);

		assertMatches(false);
	}

	private void assertMatches(boolean matchExpected) {

		assertThat(converter.matches(sourceDescriptor, targetDescriptor), is(matchExpected));
	}

	@Test
	public void convertsStringToUserCorrectly() throws Exception {

		letContextContain(context, provider);
		converter.setApplicationContext(context);

		when(service.canConvert(String.class, Long.class)).thenReturn(true);
		when(service.convert(anyString(), eq(Long.class))).thenReturn(1L);
		when(repository.findOne(1L)).thenReturn(USER);

		Object user = converter.convert("1", sourceDescriptor, targetDescriptor);
		assertThat(user, is(instanceOf(User.class)));
		assertThat(user, is((Object) USER));
	}

	/**
	 * @see DATACMNS-133
	 */
	@Test
	public void discoversFactoryAndRepoFromParentApplicationContext() {

		letContextContain(parent, provider);
		when(context.getParentBeanFactory()).thenReturn(parent);
		when(service.canConvert(String.class, Long.class)).thenReturn(true);

		converter.setApplicationContext(context);
		assertThat(converter.matches(sourceDescriptor, targetDescriptor), is(true));
	}

	private void letContextContain(ApplicationContext context, Object bean) {

		configureContextToReturnBeans(context, repository, provider);

		Map<String, Object> beanMap = getBeanAsMap(bean);
		when(context.getBeansOfType(argThat(is(subtypeOf(bean.getClass()))))).thenReturn(beanMap);
	}

	private void configureContextToReturnBeans(ApplicationContext context, UserRepository repository,
			RepositoryFactoryInformation<User, Long> provider) {

		Map<String, UserRepository> map = getBeanAsMap(repository);
		when(context.getBeansOfType(UserRepository.class)).thenReturn(map);

		providers.put("provider", provider);
		when(context.getBeansOfType(RepositoryFactoryInformation.class)).thenReturn(providers);
	}

	private <T> Map<String, T> getBeanAsMap(T bean) {

		Map<String, T> beanMap = new HashMap<String, T>();
		beanMap.put(bean.getClass().getName(), bean);
		return beanMap;
	}

	private static <T> TypeSafeMatcher<Class<T>> subtypeOf(final Class<? extends T> type) {

		return new TypeSafeMatcher<Class<T>>() {

			public void describeTo(Description arg0) {

				arg0.appendText("not a subtype of");
			}

			@Override
			public boolean matchesSafely(Class<T> arg0) {

				return arg0.isAssignableFrom(type);
			}
		};
	}

	private static class User {

	}

	private static interface UserRepository extends CrudRepository<User, Long> {

	}
}
