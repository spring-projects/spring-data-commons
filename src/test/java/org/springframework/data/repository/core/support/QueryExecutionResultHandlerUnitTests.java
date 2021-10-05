/*
 * Copyright 2014-2021 the original author or authors.
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

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

/**
 * Unit tests for {@link QueryExecutionResultHandler}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
class QueryExecutionResultHandlerUnitTests {

	QueryExecutionResultHandler handler = new QueryExecutionResultHandler(RepositoryFactorySupport.CONVERSION_SERVICE);

	@Test // DATACMNS-610
	void convertsListsToSet() throws Exception {

		var method = getMethod("set");
		var source = Collections.singletonList(new Entity());

		assertThat(handler.postProcessInvocationResult(source, method)).isInstanceOf(Set.class);
	}

	@Test // DATACMNS-483
	void turnsNullIntoJdk8Optional() throws Exception {

		var result = handler.postProcessInvocationResult(null, getMethod("jdk8Optional"));
		assertThat(result).isEqualTo(Optional.empty());
	}

	@Test // DATACMNS-483
	@SuppressWarnings("unchecked")
	void wrapsValueIntoJdk8Optional() throws Exception {

		var entity = new Entity();

		var result = handler.postProcessInvocationResult(entity, getMethod("jdk8Optional"));
		assertThat(result).isInstanceOf(Optional.class);

		var optional = (Optional<Entity>) result;
		assertThat(optional).isEqualTo(Optional.of(entity));
	}

	@Test // DATACMNS-483
	void turnsNullIntoGuavaOptional() throws Exception {

		var result = handler.postProcessInvocationResult(null, getMethod("guavaOptional"));
		assertThat(result).isEqualTo(com.google.common.base.Optional.absent());
	}

	@Test // DATACMNS-483
	@SuppressWarnings("unchecked")
	void wrapsValueIntoGuavaOptional() throws Exception {

		var entity = new Entity();

		var result = handler.postProcessInvocationResult(entity, getMethod("guavaOptional"));
		assertThat(result).isInstanceOf(com.google.common.base.Optional.class);

		var optional = (com.google.common.base.Optional<Entity>) result;
		assertThat(optional).isEqualTo(com.google.common.base.Optional.of(entity));
	}

	@Test // DATACMNS-917
	void defaultsNullToEmptyMap() throws Exception {
		assertThat(handler.postProcessInvocationResult(null, getMethod("map"))).isInstanceOf(Map.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaSingleIntoPublisher() throws Exception {

		var entity = Single.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("publisher"));
		assertThat(result).isInstanceOf(Publisher.class);

		var mono = Mono.from((Publisher<Entity>) result);
		assertThat(mono.block()).isEqualTo(entity.blockingGet());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaSingleIntoMono() throws Exception {

		var entity = Single.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		var mono = (Mono<Entity>) result;
		assertThat(mono.block()).isEqualTo(entity.blockingGet());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaSingleIntoFlux() throws Exception {

		var entity = Single.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("flux"));
		assertThat(result).isInstanceOf(Flux.class);

		var flux = (Flux<Entity>) result;
		assertThat(flux.next().block()).isEqualTo(entity.blockingGet());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaObservableIntoPublisher() throws Exception {

		var entity = Observable.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("publisher"));
		assertThat(result).isInstanceOf(Publisher.class);

		var mono = Mono.from((Publisher<Entity>) result);
		assertThat(mono.block()).isEqualTo(entity.blockingFirst());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaObservableIntoMono() throws Exception {

		var entity = Observable.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		var mono = (Mono<Entity>) result;
		assertThat(mono.block()).isEqualTo(entity.blockingFirst());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaObservableIntoFlux() throws Exception {

		var entity = Observable.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("flux"));
		assertThat(result).isInstanceOf(Flux.class);

		var flux = (Flux<Entity>) result;
		assertThat(flux.next().block()).isEqualTo(entity.blockingFirst());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaObservableIntoSingle() throws Exception {

		var entity = Observable.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("single"));
		assertThat(result).isInstanceOf(Single.class);

		var single = (Single<Entity>) result;
		assertThat(single.blockingGet()).isEqualTo(entity.blockingFirst());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaSingleIntoObservable() throws Exception {

		var entity = Single.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("observable"));
		assertThat(result).isInstanceOf(Observable.class);

		var observable = (Observable<Entity>) result;
		assertThat(observable.blockingFirst()).isEqualTo(entity.blockingGet());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsReactorMonoIntoSingle() throws Exception {

		var entity = Mono.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("single"));
		assertThat(result).isInstanceOf(Single.class);

		var single = (Single<Entity>) result;
		assertThat(single.blockingGet()).isEqualTo(entity.block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsReactorMonoIntoCompletable() throws Exception {

		var entity = Mono.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("completable"));
		assertThat(result).isInstanceOf(Completable.class);

		var completable = (Completable) result;
		completable.blockingAwait();
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsReactorMonoIntoCompletableWithException() throws Exception {

		Mono<Entity> entity = Mono.error(new InvalidDataAccessApiUsageException("err"));

		var result = handler.postProcessInvocationResult(entity, getMethod("completable"));
		assertThat(result).isInstanceOf(Completable.class);

		var completable = (Completable) result;
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(completable::blockingAwait);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaCompletableIntoMono() throws Exception {

		var entity = Completable.complete();

		var result = handler.postProcessInvocationResult(entity, getMethod("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		var mono = (Mono) result;
		assertThat(mono.block()).isNull();
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsRxJavaCompletableIntoMonoWithException() throws Exception {

		var entity = Completable.error(new InvalidDataAccessApiUsageException("err"));

		var result = handler.postProcessInvocationResult(entity, getMethod("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		var mono = (Mono) result;

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(mono::block);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsReactorMonoIntoObservable() throws Exception {

		var entity = Mono.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("observable"));
		assertThat(result).isInstanceOf(Observable.class);

		var observable = (Observable<Entity>) result;
		assertThat(observable.blockingFirst()).isEqualTo(entity.block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsReactorFluxIntoSingle() throws Exception {

		var entity = Flux.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("single"));
		assertThat(result).isInstanceOf(Single.class);

		var single = (Single<Entity>) result;
		assertThat(single.blockingGet()).isEqualTo(entity.next().block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsReactorFluxIntoObservable() throws Exception {

		var entity = Flux.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("observable"));
		assertThat(result).isInstanceOf(Observable.class);

		var observable = (Observable<Entity>) result;
		assertThat(observable.blockingFirst()).isEqualTo(entity.next().block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsReactorFluxIntoMono() throws Exception {

		var entity = Flux.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		var mono = (Mono<Entity>) result;
		assertThat(mono.block()).isEqualTo(entity.next().block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void convertsReactorMonoIntoFlux() throws Exception {

		var entity = Mono.just(new Entity());

		var result = handler.postProcessInvocationResult(entity, getMethod("flux"));
		assertThat(result).isInstanceOf(Flux.class);

		var flux = (Flux<Entity>) result;
		assertThat(flux.next().block()).isEqualTo(entity.block());
	}

	@Test // DATACMNS-1056
	void convertsOptionalToThirdPartyOption() throws Exception {

		var value = new Entity();
		var entity = Optional.of(value);

		var result = handler.postProcessInvocationResult(entity, getMethod("option"));

		assertThat(result).isInstanceOfSatisfying(Option.class, it -> assertThat(it.get()).isEqualTo(value));
	}

	@Test // DATACMNS-1165
	@SuppressWarnings("unchecked")
	void convertsIterableIntoStreamable() throws Exception {

		Iterable<?> source = asList(new Object());

		var result = handler.postProcessInvocationResult(source, getMethod("streamable"));

		assertThat(result).isInstanceOfSatisfying(Streamable.class,
				it -> assertThat(it.stream().collect(Collectors.toList())).isEqualTo(source));
	}

	@Test // DATACMNS-938
	void resolvesNestedWrapperIfOuterDoesntNeedConversion() throws Exception {

		var entity = new Entity();

		var result = handler.postProcessInvocationResult(entity, getMethod("tryOfOption"));

		assertThat(result).isInstanceOfSatisfying(Option.class, it -> assertThat(it.get()).isEqualTo(entity));
	}

	@Test // DATACMNS-1430
	void convertsElementsAndValueIntoCustomStreamable() throws Exception {

		var result = handler.postProcessInvocationResult(Arrays.asList("foo"), getMethod("customStreamable"));

		assertThat(result).isInstanceOfSatisfying(CustomStreamableWrapper.class, it -> {
			assertThat(it).containsExactly("foo");
		});
	}

	@Test // DATACMNS-1482
	void nestedConversion() throws Exception {

		var result = handler.postProcessInvocationResult(asList(BigDecimal.ZERO, BigDecimal.ONE),
				getMethod("listOfInteger"));

		assertThat(result).isInstanceOfSatisfying(List.class, list -> {

			SoftAssertions.assertSoftly(s -> {

				// for making the test failure more obvious:
				s.assertThat(list).allMatch(it -> Integer.class.isInstance(it));
				s.assertThat(list).containsExactly(0, 1);
			});
		});
	}

	@Test // DATACMNS-1552
	void keepsVavrOptionType() throws Exception {

		var source = Option.of(new Entity());

		assertThat(handler.postProcessInvocationResult(source, getMethod("option"))).isSameAs(source);
	}

	private static Method getMethod(String methodName) throws Exception {
		return Sample.class.getMethod(methodName);
	}

	static interface Sample extends Repository<Entity, Long> {

		Set<Entity> set();

		Optional<Entity> jdk8Optional();

		com.google.common.base.Optional<Entity> guavaOptional();

		Map<Integer, Entity> map();

		Publisher<Entity> publisher();

		Mono<Entity> mono();

		Flux<Entity> flux();

		Observable<Entity> observable();

		Single<Entity> single();

		Completable completable();

		// DATACMNS-1056
		Option<Entity> option();

		// DATACMNS-1165
		Streamable<Entity> streamable();

		// DATACMNS-938
		Try<Option<Entity>> tryOfOption();

		// DATACMNS-1430
		CustomStreamableWrapper<String> customStreamable();

		// DATACMNS-1482
		List<Integer> listOfInteger();
	}

	static class Entity {}

	// DATACMNS-1430

	@Value
	static class CustomStreamableWrapper<T> implements Streamable<T> {

		Streamable<T> source;

		@Override
		public Iterator<T> iterator() {
			return source.iterator();
		}
	}
}
