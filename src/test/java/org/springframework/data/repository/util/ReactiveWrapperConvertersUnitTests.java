/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.repository.util;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.reactive.ReactiveFlowKt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * Unit tests for {@link ReactiveWrapperConverters}.
 *
 * @author Mark Paluch
 * @author Hantsy Bai
 */
class ReactiveWrapperConvertersUnitTests {

	@Test // DATACMNS-836
	void shouldSupportReactorTypes() {

		assertThat(ReactiveWrapperConverters.supports(Mono.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Flux.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Publisher.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Object.class)).isFalse();
	}

	@Test // DATACMNS-1653
	void shouldSupportRxJava3Types() {

		assertThat(ReactiveWrapperConverters.supports(io.reactivex.rxjava3.core.Single.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.rxjava3.core.Maybe.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.rxjava3.core.Observable.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.rxjava3.core.Flowable.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.rxjava3.core.Completable.class)).isTrue();
	}

	@Test // DATACMNS-1763
	void shouldSupportKotlinFlow() {

		assertThat(ReactiveWrapperConverters.supports(Flow.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.rxjava3.core.Completable.class)).isTrue();
	}

	@Test // GH-2471
	void shouldSupportMutinyTypes() {

		assertThat(ReactiveWrapperConverters.supports(Uni.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Multi.class)).isTrue();
	}

	@Test // DATACMNS-836
	void toWrapperShouldCastMonoToMono() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Mono.class)).isSameAs(foo);
	}

	@Test // DATACMNS-836
	void toWrapperShouldConvertMonoToFlux() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Flux.class)).isInstanceOf(Flux.class);
	}

	@Test // DATACMNS-836
	void shouldMapMono() {

		Mono<String> foo = Mono.just("foo");
		Mono<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.block()).isEqualTo(1L);
	}

	@Test // DATACMNS-836
	void shouldMapFlux() {

		Flux<String> foo = Flux.just("foo");
		Flux<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.next().block()).isEqualTo(1L);
	}

	@Test // DATACMNS-1653
	void shouldMapRxJava3Single() {

		io.reactivex.rxjava3.core.Single<String> foo = io.reactivex.rxjava3.core.Single.just("foo");
		io.reactivex.rxjava3.core.Single<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.blockingGet()).isEqualTo(1L);
	}

	@Test // DATACMNS-1653
	void shouldMapRxJava3Maybe() {

		io.reactivex.rxjava3.core.Maybe<String> foo = io.reactivex.rxjava3.core.Maybe.just("foo");
		io.reactivex.rxjava3.core.Maybe<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.toSingle().blockingGet()).isEqualTo(1L);
	}

	@Test // DATACMNS-1653
	void shouldMapRxJava3Observable() {

		io.reactivex.rxjava3.core.Observable<String> foo = io.reactivex.rxjava3.core.Observable.just("foo");
		io.reactivex.rxjava3.core.Observable<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.blockingFirst()).isEqualTo(1L);
	}

	@Test // DATACMNS-1653
	void shouldMapRxJava3Flowable() {

		io.reactivex.rxjava3.core.Flowable<String> foo = io.reactivex.rxjava3.core.Flowable.just("foo");
		io.reactivex.rxjava3.core.Flowable<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.blockingFirst()).isEqualTo(1L);
	}

	@Test // DATACMNS-1763
	@SuppressWarnings("deprecation")
	void shouldMapKotlinFlow() {

		Flow<String> flow = FlowKt.asFlow(new String[] { "foo" });
		Flow<Long> map = ReactiveWrapperConverters.map(flow, source -> 1L);
		StepVerifier.create(ReactiveFlowKt.asPublisher(map)).expectNext(1L).verifyComplete();
	}
}
