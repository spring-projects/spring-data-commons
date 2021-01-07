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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;

/**
 * Unit tests for {@link BeanLookup}.
 *
 * @author Oliver Gierke
 * @soundtrack Dave Matthews Band - Shotgun (DMB Live 25)
 */
@ExtendWith(MockitoExtension.class)
public class BeanLookupUnitTests {

	@Mock ListableBeanFactory beanFactory;
	Map<String, EntityPathResolver> beans;

	@BeforeEach
	public void setUp() {

		this.beans = new HashMap<>();

		doReturn(beans).when(beanFactory).getBeansOfType(EntityPathResolver.class, false, false);
	}

	@Test // DATACMNS-1235
	public void returnsUniqueBeanByType() {

		beans.put("foo", SimpleEntityPathResolver.INSTANCE);

		assertThat(BeanLookup.lazyIfAvailable(EntityPathResolver.class, beanFactory).get()) //
				.isEqualTo(SimpleEntityPathResolver.INSTANCE);
	}

	@Test // DATACMNS-1235
	public void returnsEmptyLazyIfNoBeanAvailable() {
		assertThat(BeanLookup.lazyIfAvailable(EntityPathResolver.class, beanFactory).getOptional()).isEmpty();
	}

	@Test // DATACMNS-1235
	public void throwsExceptionIfMultipleBeansAreAvailable() {

		beans.put("foo", SimpleEntityPathResolver.INSTANCE);
		beans.put("bar", SimpleEntityPathResolver.INSTANCE);

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class) //
				.isThrownBy(() -> BeanLookup.lazyIfAvailable(EntityPathResolver.class, beanFactory).get()) //
				.withMessageContaining("foo") //
				.withMessageContaining("bar") //
				.withMessageContaining(EntityPathResolver.class.getName());
	}
}
