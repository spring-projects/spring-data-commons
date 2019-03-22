/*
 * Copyright 2018 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.ResultPostProcessor;
import org.springframework.data.repository.core.support.DummyRepositoryFactory;

/**
 * @author Oliver Gierke
 */
public class FooRepositoryProxyPostProcessorUnitTests {

	@Test
	public void testname() {

		MyAggregate aggregate = new MyAggregate();

		MyAggregateRepository mock = mock(MyAggregateRepository.class);
		doReturn(Collections.singleton(aggregate)).when(mock).findAll();

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("processor", new ResultPostProcessor.ForAggregate() {

			@Override
			public Object postProcess(Object source) {
				ProxyFactory factory = new ProxyFactory(source);
				return factory.getProxy();
			}
		});

		DummyRepositoryFactory factory = new DummyRepositoryFactory(mock);
		factory.setBeanFactory(beanFactory);

		MyAggregateRepository repository = factory.getRepository(MyAggregateRepository.class);

		Iterable<MyAggregate> findAll = repository.findAll();

		MyAggregate myAggregate = findAll.iterator().next();

		assertThat(myAggregate).isInstanceOf(Advised.class);
	}

	static class MyAggregate {}

	interface MyAggregateRepository extends CrudRepository<MyAggregate, Long> {}
}
