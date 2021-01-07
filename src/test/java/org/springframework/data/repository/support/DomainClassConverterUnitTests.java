/*
 * Copyright 2008-2021 the original author or authors.
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
package org.springframework.data.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.support.DummyRepositoryFactoryBean;
import org.springframework.data.repository.support.DomainClassConverter.ToIdConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Unit test for {@link DomainClassConverter}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomainClassConverterUnitTests {

	static final User USER = new User();
	static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);
	static final TypeDescriptor USER_TYPE = TypeDescriptor.valueOf(User.class);
	static final TypeDescriptor SUB_USER_TYPE = TypeDescriptor.valueOf(SubUser.class);
	static final TypeDescriptor LONG_TYPE = TypeDescriptor.valueOf(Long.class);

	@SuppressWarnings("rawtypes") DomainClassConverter converter;

	@Mock DefaultConversionService service;

	@BeforeEach
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void setUp() {
		converter = new DomainClassConverter(service);
	}

	@Test
	void matchFailsIfNoDaoAvailable() {

		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.refresh();
		converter.setApplicationContext(ctx);
		assertMatches(false);
	}

	@Test
	void matchesIfConversionInBetweenIsPossible() {

		converter.setApplicationContext(initContextWithRepo());

		when(service.canConvert(String.class, Long.class)).thenReturn(true);

		assertMatches(true);
	}

	@Test
	void matchFailsIfNoIntermediateConversionIsPossible() {

		converter.setApplicationContext(initContextWithRepo());

		when(service.canConvert(String.class, Long.class)).thenReturn(false);

		assertMatches(false);
	}

	@Test // DATACMNS-233
	void returnsNullForNullSource() {
		assertThat(converter.convert(null, STRING_TYPE, USER_TYPE)).isNull();
	}

	@Test // DATACMNS-233
	void returnsNullForEmptyStringSource() {
		assertThat(converter.convert("", STRING_TYPE, USER_TYPE)).isNull();
	}

	@Test
	void convertsStringToUserCorrectly() throws Exception {

		ApplicationContext context = initContextWithRepo();
		converter.setApplicationContext(context);

		doReturn(1L).when(service).convert(any(), eq(Long.class));

		converter.convert("1", STRING_TYPE, USER_TYPE);

		UserRepository bean = context.getBean(UserRepository.class);
		UserRepository repo = (UserRepository) ((Advised) bean).getTargetSource().getTarget();

		verify(repo, times(1)).findById(1L);
	}

	@Test // DATACMNS-133
	void discoversFactoryAndRepoFromParentApplicationContext() {

		ApplicationContext parent = initContextWithRepo();
		GenericApplicationContext context = new GenericApplicationContext(parent);
		context.refresh();

		when(service.canConvert(String.class, Long.class)).thenReturn(true);

		converter.setApplicationContext(context);
		assertThat(converter.matches(STRING_TYPE, USER_TYPE)).isTrue();
	}

	@Test // DATACMNS-583
	void converterDoesntMatchIfTargetTypeIsAssignableFromSource() {

		converter.setApplicationContext(initContextWithRepo());

		assertThat(converter.matches(SUB_USER_TYPE, USER_TYPE)).isFalse();
		assertThat((User) converter.convert(USER, USER_TYPE, USER_TYPE)).isEqualTo(USER);
	}

	@Test // DATACMNS-627
	void supportsConversionFromIdType() {

		converter.setApplicationContext(initContextWithRepo());

		assertThat(converter.matches(LONG_TYPE, USER_TYPE)).isTrue();
	}

	@Test // DATACMNS-627
	void supportsConversionFromEntityToIdType() {

		converter.setApplicationContext(initContextWithRepo());

		assertThat(converter.matches(USER_TYPE, LONG_TYPE)).isTrue();
	}

	@Test // DATACMNS-627
	void supportsConversionFromEntityToString() {

		converter.setApplicationContext(initContextWithRepo());

		when(service.canConvert(Long.class, String.class)).thenReturn(true);
		assertThat(converter.matches(USER_TYPE, STRING_TYPE)).isTrue();
	}

	@Test // DATACMNS-683
	void toIdConverterDoesNotMatchIfTargetTypeIsAssignableFromSource() throws NoSuchMethodException {

		converter.setApplicationContext(initContextWithRepo());
		assertMatches(false);

		@SuppressWarnings("rawtypes")
		Optional<ToIdConverter> toIdConverter = (Optional<ToIdConverter>) ReflectionTestUtils.getField(converter,
				"toIdConverter");

		Method method = Wrapper.class.getMethod("foo", User.class);
		TypeDescriptor target = TypeDescriptor.nested(new MethodParameter(method, 0), 0);

		assertThat(toIdConverter).map(it -> it.matches(SUB_USER_TYPE, target)).hasValue(false);
	}

	@Test // DATACMNS-1743
	void registersConvertersOnConversionService() {

		ConfigurableConversionService conversionService = new DefaultConversionService();
		DomainClassConverter<?> converter = new DomainClassConverter<>(conversionService);
		converter.setApplicationContext(initContextWithRepo());

		assertThat(conversionService.canConvert(String.class, User.class)).isTrue();
	}

	@Test // DATACMNS-1743
	void returnsNullForFailedLookup() {

		ApplicationContext context = initContextWithRepo();
		converter.setApplicationContext(context);

		// Expect ID conversion
		doReturn(4711L).when(service).convert("4711", Long.class);

		// Configure aggregate lookup to fail
		UserRepository users = context.getBean(UserRepository.class);
		users = (UserRepository) AopProxyUtils.getSingletonTarget(users);
		doReturn(Optional.empty()).when(users).findById(any());

		assertThat(converter.convert("4711", STRING_TYPE, USER_TYPE)).isNull();
	}

	private ApplicationContext initContextWithRepo() {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(DummyRepositoryFactoryBean.class);
		builder.addConstructorArgValue(UserRepository.class);

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("provider", builder.getBeanDefinition());

		GenericApplicationContext ctx = new GenericApplicationContext(factory);
		ctx.refresh();
		return ctx;
	}

	private void assertMatches(boolean matchExpected) {
		assertThat(converter.matches(STRING_TYPE, USER_TYPE)).isEqualTo(matchExpected);
	}

	static interface Wrapper {

		void foo(@ModelAttribute User user);
	}

	private static class User {

	}

	private static class SubUser extends User {}

	private static interface UserRepository extends CrudRepository<User, Long> {

	}
}
