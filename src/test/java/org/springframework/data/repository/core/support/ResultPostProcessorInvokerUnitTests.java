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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.repository.core.ResultPostProcessor;
import org.springframework.data.repository.core.ResultPostProcessor.ByType;

/**
 * Unit tests for {@link ResultPostProcessorInvoker}.
 * 
 * @author Oliver Gierke
 */
public class ResultPostProcessorInvokerUnitTests {

	ResultPostProcessor.ForAggregate aggregateProcessor = it -> {
		ProxyFactory factory = new ProxyFactory(it);
		return factory.getProxy();
	};

	ResultPostProcessor.ByType<MyDto> dtoProcessor = new ByType<ResultPostProcessorInvokerUnitTests.MyDto>() {

		@Override
		public MyDto postProcess(MyDto source) {
			source.firstname = "Firstname";
			return source;
		}
	};

	ResultPostProcessor.ByType<MySpecialDto> specialDtoProcessor = new ByType<MySpecialDto>() {

		@Override
		public MySpecialDto postProcess(MySpecialDto source) {
			source.lastname = "Lastname";
			return source;
		}
	};

	@Test
	public void doesNotTouchSourceIfNoProcessorsRegistered() {

		MyAggregate source = new MyAggregate();

		assertThat(ResultPostProcessorInvoker.NONE.postProcess(source)).isSameAs(source);
	}

	@Test
	public void invokesAggregateProcessorForAggregate() {

		ResultPostProcessorInvoker invoker = createInvoker(aggregateProcessor);

		Object result = invoker.postProcess(new MyAggregate());

		assertThat(result).isInstanceOf(MyAggregate.class);
		assertThat(result).isInstanceOf(Advised.class);
	}

	@Test
	public void doesNotInvokeAggregatePostProcessorForNonAggregate() {

		ResultPostProcessorInvoker invoker = createInvoker(aggregateProcessor);

		Object result = invoker.postProcess(new MyDto());

		assertThat(result).isNotInstanceOf(Advised.class);
	}

	@Test
	public void invokesTypePostProcessor() {

		ResultPostProcessorInvoker invoker = createInvoker(dtoProcessor, specialDtoProcessor, aggregateProcessor);

		assertThat(invoker.postProcess(new MySpecialDto())).isInstanceOfSatisfying(MySpecialDto.class, it -> {

			assertThat(it.firstname).isEqualTo("Firstname");
			assertThat(it.lastname).isEqualTo("Lastname");

			assertThat(it).isNotInstanceOf(Advised.class);
		});
	}

	@Test
	public void invokesProcessorForCollectionElements() {

		ResultPostProcessorInvoker invoker = createInvoker(aggregateProcessor);

		assertThat(invoker.postProcess(Collections.singleton(new MyAggregate()))) //
				.isInstanceOfSatisfying(Collection.class, it -> {
					assertThat(it).allMatch(Advised.class::isInstance);
				});
	}

	@Test
	public void invokesProcessorsForOptional() {

		ResultPostProcessorInvoker invoker = createInvoker(aggregateProcessor);

		assertThat(invoker.postProcess(Optional.of(new MyAggregate()))) //
				.isInstanceOfSatisfying(Optional.class, it -> assertThat(it).hasValueSatisfying(Advised.class::isInstance));
	}

	@Test
	public void rejectsLambdaDefinedProcessor() {

		ResultPostProcessor.ByType<MyDto> processor = it -> it;

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> createInvoker(processor));
	}

	private static ResultPostProcessorInvoker createInvoker(ResultPostProcessor.ByType<?>... processor) {
		return new ResultPostProcessorInvoker(MyAggregate.class, Arrays.asList(processor));
	}

	static class MyAggregate {}

	static class MyDto {
		String firstname;
	}

	static class MySpecialDto extends MyDto {
		String lastname;
	}
}
