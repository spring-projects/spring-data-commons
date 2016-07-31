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
package org.springframework.data.repository.util;

import static org.assertj.core.api.Assertions.assertThat;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

/**
 * Unit tests for {@link ReactiveWrappers}.
 * 
 * @author Mark Paluch
 */
public class ReactiveWrappersUnitTests {
	
	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void isSingleLikeShouldReportCorrectNoTypes() {

		assertThat(ReactiveWrappers.isNoValueType(Mono.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Flux.class)).isTrue();
		assertThat(ReactiveWrappers.isNoValueType(Single.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Completable.class)).isTrue();
		assertThat(ReactiveWrappers.isNoValueType(Observable.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Publisher.class)).isTrue();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.Single.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.Maybe.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Flowable.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.Observable.class)).isFalse();
	}
	
	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void isSingleLikeShouldReportCorrectSingleTypes() {

		assertThat(ReactiveWrappers.isSingleValueType(Mono.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(Flux.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(Single.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(Observable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(Publisher.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Single.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Maybe.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(Flowable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Observable.class)).isFalse();
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void isCollectionLikeShouldReportCorrectCollectionTypes() {

		assertThat(ReactiveWrappers.isMultiValueType(Mono.class)).isFalse();
		assertThat(ReactiveWrappers.isMultiValueType(Flux.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(Single.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isMultiValueType(Observable.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(Publisher.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(io.reactivex.Single.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isMultiValueType(Flowable.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(io.reactivex.Observable.class)).isTrue();
	}
}
