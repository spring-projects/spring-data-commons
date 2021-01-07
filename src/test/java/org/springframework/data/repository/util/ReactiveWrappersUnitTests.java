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

import static org.assertj.core.api.Assertions.*;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * Unit tests for {@link ReactiveWrappers}.
 *
 * @author Mark Paluch
 * @author Gerrit Meier
 */
class ReactiveWrappersUnitTests {

	@Test // DATACMNS-836, DATACMNS-1653, DATACMNS-1753
	void isSingleLikeShouldReportCorrectNoTypes() {

		assertThat(ReactiveWrappers.isNoValueType(Mono.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Flux.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Single.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Completable.class)).isTrue();
		assertThat(ReactiveWrappers.isNoValueType(CompletableFuture.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Observable.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Publisher.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.Single.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.Maybe.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(Flowable.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.Observable.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.rxjava3.core.Single.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.rxjava3.core.Maybe.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.rxjava3.core.Flowable.class)).isFalse();
		assertThat(ReactiveWrappers.isNoValueType(io.reactivex.rxjava3.core.Observable.class)).isFalse();
	}

	@Test // DATACMNS-836, DATACMNS-1653, DATACMNS-1753
	void isSingleLikeShouldReportCorrectSingleTypes() {

		assertThat(ReactiveWrappers.isSingleValueType(Mono.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(Flux.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(Single.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(CompletableFuture.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(Observable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(Publisher.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Single.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Maybe.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(Flowable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Observable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.rxjava3.core.Single.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.rxjava3.core.Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.rxjava3.core.Maybe.class)).isTrue();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.rxjava3.core.Flowable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.rxjava3.core.Observable.class)).isFalse();
	}

	@Test // DATACMNS-836, DATACMNS-1653, DATACMNS-1753
	void isCollectionLikeShouldReportCorrectCollectionTypes() {

		assertThat(ReactiveWrappers.isMultiValueType(Mono.class)).isFalse();
		assertThat(ReactiveWrappers.isMultiValueType(Flux.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(Single.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(CompletableFuture.class)).isFalse();
		assertThat(ReactiveWrappers.isMultiValueType(Observable.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(Publisher.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(io.reactivex.Single.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isMultiValueType(Flowable.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(io.reactivex.Observable.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(io.reactivex.rxjava3.core.Single.class)).isFalse();
		assertThat(ReactiveWrappers.isSingleValueType(io.reactivex.rxjava3.core.Completable.class)).isFalse();
		assertThat(ReactiveWrappers.isMultiValueType(io.reactivex.rxjava3.core.Flowable.class)).isTrue();
		assertThat(ReactiveWrappers.isMultiValueType(io.reactivex.rxjava3.core.Observable.class)).isTrue();
	}
}
