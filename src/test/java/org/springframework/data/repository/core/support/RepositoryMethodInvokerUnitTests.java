/*
 * Copyright 2020-2021 the original author or authors.
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
import static org.mockito.Mockito.*;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.reactor.ReactorContext;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Subscription;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.support.CoroutineRepositoryMetadataUnitTests.MyCoroutineRepository;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult.State;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Christoph Strobl
 */
@ExtendWith(MockitoExtension.class)
class RepositoryMethodInvokerUnitTests {

	@Mock RepositoryQuery query;
	CapturingRepositoryInvocationMulticaster multicaster;

	@BeforeEach
	void beforeEach() {
		multicaster = new CapturingRepositoryInvocationMulticaster();
	}

	@Test // DATACMNS-1764
	void usesRepositoryInterfaceNameForMethodsDefinedOnCrudRepository() throws Exception {

		when(query.execute(any())).thenReturn(new TestDummy());

		repositoryMethodInvoker("findAll").invoke();

		assertThat(multicaster.first().getMethod().getName()).isEqualTo("findAll");
		assertThat(multicaster.first().getRepositoryInterface()).isEqualTo(DummyRepository.class);
	}

	@Test // DATACMNS-1764
	void usesRepositoryInterfaceNameForMethodsOnTheInterface() throws Exception {

		when(query.execute(any())).thenReturn(new TestDummy());

		repositoryMethodInvoker("findByName").invoke();

		assertThat(multicaster.first().getMethod().getName()).isEqualTo("findByName");
		assertThat(multicaster.first().getRepositoryInterface()).isEqualTo(DummyRepository.class);
	}

	@Test // DATACMNS-1764
	void usesRepositoryInterfaceNameForMethodsDefinedOnReactiveCrudRepository() throws Exception {

		when(query.execute(any())).thenReturn(Flux.just(new TestDummy()));

		repositoryMethodInvokerForReactive("findAll").<Flux<TestDummy>> invoke().subscribe();

		assertThat(multicaster.first().getMethod().getName()).isEqualTo("findAll");
		assertThat(multicaster.first().getRepositoryInterface()).isEqualTo(ReactiveDummyRepository.class);
	}

	@Test // DATACMNS-1764
	void capturesImperativeDurationCorrectly() throws Exception {

		when(query.execute(any())).thenAnswer(new AnswersWithDelay(250, new Returns(new TestDummy())));

		repositoryMethodInvoker("findAll").invoke();

		assertThat(multicaster.first().getDuration(TimeUnit.MILLISECONDS)).isCloseTo(250, Percentage.withPercentage(10));
	}

	@Test // DATACMNS-1764
	void capturesReactiveDurationCorrectly() throws Exception {

		when(query.execute(any())).thenReturn(Flux.just(new TestDummy()).doOnSubscribe(delays(250)::delay));

		repositoryMethodInvokerForReactive("findAll").<Flux<TestDummy>> invoke().subscribe();

		assertThat(multicaster.first().getDuration(TimeUnit.MILLISECONDS)).isCloseTo(250, Percentage.withPercentage(10));
	}

	@Test // DATACMNS-1764
	void capturesReactiveExecutionOnSubscribe() throws Exception {

		when(query.execute(any())).thenReturn(Flux.just(new TestDummy()));

		Flux<TestDummy> findAll = repositoryMethodInvokerForReactive("findAll").invoke();

		assertThat(multicaster).isEmpty();

		findAll.subscribe();

		assertThat(multicaster).hasSize(1);
	}

	@Test // DATACMNS-1764
	void capturesReactiveDurationPerSubscriptionCorrectly() throws Exception {

		when(query.execute(any())).thenReturn(Flux.just(new TestDummy()).doOnSubscribe(delays(250, 100)::delay));

		Flux<TestDummy> findAll = repositoryMethodInvokerForReactive("findAll").invoke();

		findAll.subscribe();
		findAll.subscribe();

		assertThat(multicaster.first().getDuration(TimeUnit.MILLISECONDS)).isCloseTo(250, Percentage.withPercentage(10));
		assertThat(multicaster.last().getDuration(TimeUnit.MILLISECONDS)).isCloseTo(100, Percentage.withPercentage(10));
	}

	@Test // DATACMNS-1764
	void capturesStreamDurationOnClose() throws Exception {

		when(query.execute(any())).thenReturn(Stream.generate(TestDummy::new));

		Stream<TestDummy> stream = repositoryMethodInvoker("streamAll").invoke();
		assertThat(multicaster).isEmpty();

		stream.iterator().next();
		assertThat(multicaster).isEmpty();

		stream.close();
		assertThat(multicaster).hasSize(1);
	}

	@Test // DATACMNS-1764
	void capturesStreamDurationAsSumOfDelayTillCancel() throws Exception {

		Delays delays = delays(250, 100);
		when(query.execute(any())).thenReturn(Stream.generate(() -> {

			delays.delay();
			return new TestDummy();
		}));

		Stream<TestDummy> stream = repositoryMethodInvoker("streamAll").invoke();
		stream.limit(2).forEach(it -> {});
		stream.close();

		assertThat(multicaster.first().getDuration(TimeUnit.MILLISECONDS)).isCloseTo(350, Percentage.withPercentage(10));
	}

	@Test // DATACMNS-1764
	void capturesImperativeSuccessCorrectly() throws Exception {

		repositoryMethodInvoker("findAll").invoke();

		assertThat(multicaster.first().getResult().getState()).isEqualTo(State.SUCCESS);
		assertThat(multicaster.first().getResult().getError()).isNull();
	}

	@Test // DATACMNS-1764
	void capturesReactiveCompletionCorrectly() throws Exception {

		when(query.execute(any())).thenReturn(Mono.just(new TestDummy()));

		repositoryMethodInvokerForReactive("findByName").<Mono<TestDummy>> invoke().subscribe();

		assertThat(multicaster.first().getResult().getState()).isEqualTo(State.SUCCESS);
		assertThat(multicaster.first().getResult().getError()).isNull();
	}

	@Test // DATACMNS-1764
	void capturesImperativeErrorCorrectly() {

		when(query.execute(any())).thenThrow(new IllegalStateException("I'll be back!"));
		assertThatIllegalStateException().isThrownBy(() -> repositoryMethodInvoker("findAll").invoke());

		assertThat(multicaster.first().getResult().getState()).isEqualTo(State.ERROR);
		assertThat(multicaster.first().getResult().getError()).isInstanceOf(IllegalStateException.class);
	}

	@Test // DATACMNS-1764
	void capturesReactiveErrorCorrectly() throws Exception {

		when(query.execute(any())).thenReturn(Mono.fromSupplier(() -> {
			throw new IllegalStateException("I'll be back!");
		}));

		repositoryMethodInvokerForReactive("findByName").<Mono<TestDummy>> invoke().as(StepVerifier::create).verifyError();

		assertThat(multicaster.first().getResult().getState()).isEqualTo(State.ERROR);
		assertThat(multicaster.first().getResult().getError()).isInstanceOf(IllegalStateException.class);
	}

	@Test // DATACMNS-1764
	void capturesReactiveCancellationCorrectly() throws Exception {

		when(query.execute(any())).thenReturn(Flux.just(new TestDummy(), new TestDummy()));

		repositoryMethodInvokerForReactive("findAll").<Flux<TestDummy>> invoke().take(1).subscribe();

		assertThat(multicaster.first().getResult().getState()).isEqualTo(State.CANCELED);
		assertThat(multicaster.first().getResult().getError()).isNull();
	}

	@Test // DATACMNS-1764
	void capturesKotlinSuspendFunctionsCorrectly() throws Exception {

		Flux<TestDummy> result = Flux.just(new TestDummy());
		when(query.execute(any())).thenReturn(result);

		Flow<TestDummy> flow = new RepositoryMethodInvokerStub(MyCoroutineRepository.class, multicaster,
				"suspendedQueryMethod", query::execute).invoke(mock(Continuation.class));

		assertThat(multicaster).isEmpty();

		FlowKt.toCollection(flow, new ArrayList<>(), new Continuation<ArrayList<? extends Object>>() {

			ReactorContext ctx = new ReactorContext(reactor.util.context.Context.empty());

			@NotNull
			@Override
			public CoroutineContext getContext() {
				return ctx;
			}

			@Override
			public void resumeWith(@NotNull Object o) {

			}
		});

		assertThat(multicaster.first().getResult().getState()).isEqualTo(State.SUCCESS);
		assertThat(multicaster.first().getResult().getError()).isNull();
	}

	RepositoryMethodInvokerStub repositoryMethodInvoker(String methodName) {
		return new RepositoryMethodInvokerStub(DummyRepository.class, multicaster, methodName, query::execute);
	}

	RepositoryMethodInvokerStub repositoryMethodInvokerForReactive(String methodName) {
		return new RepositoryMethodInvokerStub(ReactiveDummyRepository.class, multicaster, methodName, query::execute);
	}

	static Delays delays(Integer... delays) {
		return new Delays(delays);
	}

	static class Delays {

		private final Queue<Integer> delays;

		Delays(Integer... delays) {

			this.delays = new ArrayBlockingQueue(delays.length);
			for (Integer delay : delays) {
				this.delays.add(delay);
			}
		}

		void delay() {
			delay(null);
		}

		void delay(Subscription it) {

			if (delays.isEmpty()) {
				return;
			}

			delayBy(delays.poll());
		}

		private static void delayBy(Integer ms) {

			if (ms == null) {
				return;
			}

			try {
				TimeUnit.MILLISECONDS.sleep(ms);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	static class RepositoryMethodInvokerStub extends RepositoryMethodInvoker {

		private final Class<?> repositoryInterface;
		private final RepositoryInvocationMulticaster multicaster;

		RepositoryMethodInvokerStub(Class<?> repositoryInterface, RepositoryInvocationMulticaster multicaster,
				String methodName, Invokable invokable) {

			super(methodByName(repositoryInterface, methodName), invokable);
			this.repositoryInterface = repositoryInterface;
			this.multicaster = multicaster;
		}

		public <T> T invoke(Object... args) throws Exception {
			return (T) super.invoke(repositoryInterface, multicaster, args);
		}

		@Nullable
		public <T> T invoke(RepositoryInvocationMulticaster multicaster, Object... args) throws Exception {
			return (T) super.invoke(repositoryInterface, multicaster, args);
		}

		static Method methodByName(Class<?> repository, String name) {
			return ReflectionUtils.findMethod(repository, name, null);
		}

	}

	static class CapturingRepositoryInvocationMulticaster
			implements RepositoryInvocationMulticaster, Iterable<RepositoryMethodInvocation> {

		private final List<RepositoryMethodInvocation> invocations = new ArrayList<>();

		@Override
		public void notifyListeners(Method method, Object[] args, RepositoryMethodInvocation result) {
			invocations.add(result);
		}

		RepositoryMethodInvocation first() {

			Assertions.assertThat(invocations).isNotEmpty();
			return CollectionUtils.firstElement(invocations);
		}

		RepositoryMethodInvocation last() {

			Assertions.assertThat(invocations).isNotEmpty();
			return CollectionUtils.lastElement(invocations);
		}

		@NotNull
		@Override
		public Iterator<RepositoryMethodInvocation> iterator() {
			return invocations.iterator();
		}

		@Override
		public Spliterator<RepositoryMethodInvocation> spliterator() {
			return invocations.spliterator();
		}

		@Override
		public void forEach(Consumer<? super RepositoryMethodInvocation> action) {
			invocations.forEach(action);
		}
	}

	interface DummyRepository extends CrudRepository<TestDummy, String> {

		TestDummy findByName(String name);

		Stream<TestDummy> streamAll();
	}

	interface ReactiveDummyRepository extends ReactiveCrudRepository<TestDummy, String> {

		Mono<TestDummy> findByName(String name);
	}

	@ToString
	@AllArgsConstructor
	@NoArgsConstructor
	static class TestDummy {
		String id;
		String name;
	}
}
