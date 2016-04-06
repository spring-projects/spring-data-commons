/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import org.junit.Test;
import org.reactivestreams.Publisher;

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

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void shouldSupportReactorTypes() {

		assertThat(ReactiveWrapperConverters.supports(Mono.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Flux.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Publisher.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Object.class)).isFalse();
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void shouldSupportRxJavaTypes() {

		assertThat(ReactiveWrapperConverters.supports(Single.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Observable.class)).isTrue();
		assertThat(ReactiveWrapperConverters.supports(Completable.class)).isFalse();
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void isSingleLikeShouldReportCorrectSingleTypes() {

		assertThat(ReactiveWrapperConverters.isSingleLike(Mono.class)).isTrue();
		assertThat(ReactiveWrapperConverters.isSingleLike(Flux.class)).isFalse();
		assertThat(ReactiveWrapperConverters.isSingleLike(Single.class)).isTrue();
		assertThat(ReactiveWrapperConverters.isSingleLike(Observable.class)).isFalse();
		assertThat(ReactiveWrapperConverters.isSingleLike(Publisher.class)).isFalse();
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void isCollectionLikeShouldReportCorrectCollectionTypes() {

		assertThat(ReactiveWrapperConverters.isCollectionLike(Mono.class)).isFalse();
		assertThat(ReactiveWrapperConverters.isCollectionLike(Flux.class)).isTrue();
		assertThat(ReactiveWrapperConverters.isCollectionLike(Single.class)).isFalse();
		assertThat(ReactiveWrapperConverters.isCollectionLike(Observable.class)).isTrue();
		assertThat(ReactiveWrapperConverters.isCollectionLike(Publisher.class)).isTrue();
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void toWrapperShouldCastMonoToMono() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Mono.class)).isSameAs(foo);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void toWrapperShouldConvertMonoToSingle() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Single.class)).isInstanceOf(Single.class);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void toWrapperShouldConvertMonoToFlux() {

		Mono<String> foo = Mono.just("foo");
		assertThat(ReactiveWrapperConverters.toWrapper(foo, Flux.class)).isInstanceOf(Flux.class);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void shouldMapMono() {

		Mono<String> foo = Mono.just("foo");
		Mono<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.block()).isEqualTo(1L);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void shouldMapFlux() {

		Flux<String> foo = Flux.just("foo");
		Flux<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.next().block()).isEqualTo(1L);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void shouldMapSingle() {

		Single<String> foo = Single.just("foo");
		Single<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.toBlocking().value()).isEqualTo(1L);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void shouldMapObservable() {

		Observable<String> foo = Observable.just("foo");
		Observable<Long> map = ReactiveWrapperConverters.map(foo, source -> 1L);
		assertThat(map.toBlocking().first()).isEqualTo(1L);
	}
}
