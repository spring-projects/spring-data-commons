/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.data.repository.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Maybe;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Unit tests for {@link ReactiveWrapperConverters}.
 *
 * @author Mark Paluch
 */
public class ReactiveWrapperConvertersUnitTests {

	@Test // DATACMNS-836
	public void shouldSupportReactorTypes() {

		assertThat(ReactiveWrapperConverters.supports(Mono.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Flux.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Publisher.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Object.class)).isFalse();
	}

	@Test // DATACMNS-836
	public void shouldSupportRxJava1Types() {

		assertThat(ReactiveWrapperConverters.supports(Single.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Observable.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Completable.class)).isTrue();
	}

	@Test // DATACMNS-836
	public void shouldSupportRxJava2Types() {

		assertThat(ReactiveWrapperConverters.supports(io.reactivex.Single.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.Maybe.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.Observable.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.Flowable.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(io.reactivex.Completable.class)).isTrue();
	}

	@Test // DATACMNS-836
	public void toWrapperShouldCastMonoToMono() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Mono.class)).isSameAs(foo);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertMonoToRxJava1Single() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Single.class)).isInstanceOf(Single.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertMonoToRxJava2Single() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, io.reactivex.Single.class))
				.isInstanceOf(io.reactivex.Single.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2SingleToMono() {

		io.reactivex.Single<String> foo = io.reactivex.Single.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Mono.class)).isInstanceOf(Mono.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2SingleToPublisher() {

		io.reactivex.Single<String> foo = io.reactivex.Single.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Publisher.class)).isInstanceOf(Publisher.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2MaybeToMono() {

		io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Mono.class)).isInstanceOf(Mono.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2MaybeToFlux() {

		io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Flux.class)).isInstanceOf(Flux.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2MaybeToPublisher() {

		io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Publisher.class)).isInstanceOf(Publisher.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2FlowableToMono() {

		io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Mono.class)).isInstanceOf(Mono.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2FlowableToFlux() {

		io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Flux.class)).isInstanceOf(Flux.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldCastRxJava2FlowableToPublisher() {

		io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Publisher.class)).isSameAs(foo);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2ObservableToMono() {

		io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Mono.class)).isInstanceOf(Mono.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2ObservableToFlux() {

		io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Flux.class)).isInstanceOf(Flux.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2ObservableToSingle() {

		io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, io.reactivex.Single.class))
				.isInstanceOf(io.reactivex.Single.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2ObservableToMaybe() {

		io.reactivex.Observable<String> foo = io.reactivex.Observable.empty();
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Maybe.class)).isInstanceOf(Maybe.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertRxJava2ObservableToPublisher() {

		io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Publisher.class)).isInstanceOf(Publisher.class);
	}

	@Test // DATACMNS-988
	public void toWrapperShouldConvertPublisherToRxJava2Observable() {

		Flux<String> foo = Flux.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, io.reactivex.Observable.class))
				.isInstanceOf(io.reactivex.Observable.class);
	}

	@Test // DATACMNS-988
	public void toWrapperShouldConvertPublisherToRxJava2Flowable() {

		Flux<String> foo = Flux.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, io.reactivex.Flowable.class))
				.isInstanceOf(io.reactivex.Flowable.class);
	}

	@Test // DATACMNS-836
	public void toWrapperShouldConvertMonoToFlux() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Flux.class)).isInstanceOf(Flux.class);
	}

	@Test // DATACMNS-836
	public void shouldMapMono() {

		Mono<String> foo = Mono.just("foo");
		Mono<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.block()).isEqualTo(1L);
	}

	@Test // DATACMNS-836
	public void shouldMapFlux() {

		Flux<String> foo = Flux.just("foo");
		Flux<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.next().block()).isEqualTo(1L);
	}

	@Test // DATACMNS-836
	public void shouldMapRxJava1Single() {

		Single<String> foo = Single.just("foo");
		Single<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.toBlocking().value()).isEqualTo(1L);
	}

	@Test // DATACMNS-836
	public void shouldMapRxJava1Observable() {

		Observable<String> foo = Observable.just("foo");
		Observable<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.toBlocking().first()).isEqualTo(1L);
	}

	@Test // DATACMNS-836
	public void shouldMapRxJava2Single() {

		io.reactivex.Single<String> foo = io.reactivex.Single.just("foo");
		io.reactivex.Single<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.blockingGet()).isEqualTo(1L);
	}

	@Test // DATACMNS-836
	public void shouldMapRxJava2Maybe() {

		io.reactivex.Maybe<String> foo = io.reactivex.Maybe.just("foo");
		io.reactivex.Maybe<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.toSingle().blockingGet()).isEqualTo(1L);
	}

	@Test // DATACMNS-836
	public void shouldMapRxJava2Observable() {

		io.reactivex.Observable<String> foo = io.reactivex.Observable.just("foo");
		io.reactivex.Observable<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.blockingFirst()).isEqualTo(1L);
	}

	@Test // DATACMNS-836
	public void shouldMapRxJava2Flowable() {

		io.reactivex.Flowable<String> foo = io.reactivex.Flowable.just("foo");
		io.reactivex.Flowable<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.blockingFirst()).isEqualTo(1L);
	}
}
