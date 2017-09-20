/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.repository.core.support;

import static org.assertj.core.api.Assertions.*;

import javaslang.control.Option;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

/**
 * Unit tests for {@link QueryExecutionResultHandler}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class QueryExecutionResultHandlerUnitTests {

	QueryExecutionResultHandler handler = new QueryExecutionResultHandler();

	@Test // DATACMNS-610
	public void convertsListsToSet() throws Exception {

		TypeDescriptor descriptor = getTypeDescriptorFor("set");
		List<Entity> source = Collections.singletonList(new Entity());

		assertThat(handler.postProcessInvocationResult(source, descriptor)).isInstanceOf(Set.class);
	}

	@Test // DATACMNS-483
	public void turnsNullIntoJdk8Optional() throws Exception {

		Object result = handler.postProcessInvocationResult(null, getTypeDescriptorFor("jdk8Optional"));
		assertThat(result).isEqualTo(Optional.empty());
	}

	@Test // DATACMNS-483
	@SuppressWarnings("unchecked")
	public void wrapsValueIntoJdk8Optional() throws Exception {

		Entity entity = new Entity();

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("jdk8Optional"));
		assertThat(result).isInstanceOf(Optional.class);

		Optional<Entity> optional = (Optional<Entity>) result;
		assertThat(optional).isEqualTo(Optional.of(entity));
	}

	@Test // DATACMNS-483
	public void turnsNullIntoGuavaOptional() throws Exception {

		Object result = handler.postProcessInvocationResult(null, getTypeDescriptorFor("guavaOptional"));
		assertThat(result).isEqualTo(com.google.common.base.Optional.absent());
	}

	@Test // DATACMNS-483
	@SuppressWarnings("unchecked")
	public void wrapsValueIntoGuavaOptional() throws Exception {

		Entity entity = new Entity();

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("guavaOptional"));
		assertThat(result).isInstanceOf(com.google.common.base.Optional.class);

		com.google.common.base.Optional<Entity> optional = (com.google.common.base.Optional<Entity>) result;
		assertThat(optional).isEqualTo(com.google.common.base.Optional.of(entity));
	}

	@Test // DATACMNS-917
	public void defaultsNullToEmptyMap() throws Exception {
		assertThat(handler.postProcessInvocationResult(null, getTypeDescriptorFor("map"))).isInstanceOf(Map.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaSingleIntoPublisher() throws Exception {

		Single<Entity> entity = Single.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("publisher"));
		assertThat(result).isInstanceOf(Publisher.class);

		Mono<Entity> mono = Mono.from((Publisher<Entity>) result);
		assertThat(mono.block()).isEqualTo(entity.toBlocking().value());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaSingleIntoMono() throws Exception {

		Single<Entity> entity = Single.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		Mono<Entity> mono = (Mono<Entity>) result;
		assertThat(mono.block()).isEqualTo(entity.toBlocking().value());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaSingleIntoFlux() throws Exception {

		Single<Entity> entity = Single.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("flux"));
		assertThat(result).isInstanceOf(Flux.class);

		Flux<Entity> flux = (Flux<Entity>) result;
		assertThat(flux.next().block()).isEqualTo(entity.toBlocking().value());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaObservableIntoPublisher() throws Exception {

		Observable<Entity> entity = Observable.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("publisher"));
		assertThat(result).isInstanceOf(Publisher.class);

		Mono<Entity> mono = Mono.from((Publisher<Entity>) result);
		assertThat(mono.block()).isEqualTo(entity.toBlocking().first());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaObservableIntoMono() throws Exception {

		Observable<Entity> entity = Observable.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		Mono<Entity> mono = (Mono<Entity>) result;
		assertThat(mono.block()).isEqualTo(entity.toBlocking().first());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaObservableIntoFlux() throws Exception {

		Observable<Entity> entity = Observable.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("flux"));
		assertThat(result).isInstanceOf(Flux.class);

		Flux<Entity> flux = (Flux<Entity>) result;
		assertThat(flux.next().block()).isEqualTo(entity.toBlocking().first());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaObservableIntoSingle() throws Exception {

		Observable<Entity> entity = Observable.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("single"));
		assertThat(result).isInstanceOf(Single.class);

		Single<Entity> single = (Single<Entity>) result;
		assertThat(single.toBlocking().value()).isEqualTo(entity.toBlocking().first());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaSingleIntoObservable() throws Exception {

		Single<Entity> entity = Single.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("observable"));
		assertThat(result).isInstanceOf(Observable.class);

		Observable<Entity> observable = (Observable<Entity>) result;
		assertThat(observable.toBlocking().first()).isEqualTo(entity.toBlocking().value());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoSingle() throws Exception {

		Mono<Entity> entity = Mono.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("single"));
		assertThat(result).isInstanceOf(Single.class);

		Single<Entity> single = (Single<Entity>) result;
		assertThat(single.toBlocking().value()).isEqualTo(entity.block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoCompletable() throws Exception {

		Mono<Entity> entity = Mono.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("completable"));
		assertThat(result).isInstanceOf(Completable.class);

		Completable completable = (Completable) result;
		assertThat(completable.get()).isNull();
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoCompletableWithException() throws Exception {

		Mono<Entity> entity = Mono.error(new InvalidDataAccessApiUsageException("err"));

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("completable"));
		assertThat(result).isInstanceOf(Completable.class);

		Completable completable = (Completable) result;
		assertThat(completable.get()).isInstanceOf(InvalidDataAccessApiUsageException.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaCompletableIntoMono() throws Exception {

		Completable entity = Completable.complete();

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		Mono mono = (Mono) result;
		assertThat(mono.block()).isNull();
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsRxJavaCompletableIntoMonoWithException() throws Exception {

		Completable entity = Completable.error(new InvalidDataAccessApiUsageException("err"));

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		Mono mono = (Mono) result;
		mono.block();
		fail("Missing InvalidDataAccessApiUsageException");
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoObservable() throws Exception {

		Mono<Entity> entity = Mono.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("observable"));
		assertThat(result).isInstanceOf(Observable.class);

		Observable<Entity> observable = (Observable<Entity>) result;
		assertThat(observable.toBlocking().first()).isEqualTo(entity.block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsReactorFluxIntoSingle() throws Exception {

		Flux<Entity> entity = Flux.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("single"));
		assertThat(result).isInstanceOf(Single.class);

		Single<Entity> single = (Single<Entity>) result;
		assertThat(single.toBlocking().value()).isEqualTo(entity.next().block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsReactorFluxIntoObservable() throws Exception {

		Flux<Entity> entity = Flux.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("observable"));
		assertThat(result).isInstanceOf(Observable.class);

		Observable<Entity> observable = (Observable<Entity>) result;
		assertThat(observable.toBlocking().first()).isEqualTo(entity.next().block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsReactorFluxIntoMono() throws Exception {

		Flux<Entity> entity = Flux.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result).isInstanceOf(Mono.class);

		Mono<Entity> mono = (Mono<Entity>) result;
		assertThat(mono.block()).isEqualTo(entity.next().block());
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoFlux() throws Exception {

		Mono<Entity> entity = Mono.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("flux"));
		assertThat(result).isInstanceOf(Flux.class);

		Flux<Entity> flux = (Flux<Entity>) result;
		assertThat(flux.next().block()).isEqualTo(entity.block());
	}

	@Test // DATACMNS-1056
	public void convertsOptionalToThirdPartyOption() throws Exception {

		Entity value = new Entity();
		Optional<Entity> entity = Optional.of(value);

		Object result = handler.postProcessInvocationResult(entity, TypeDescriptor.valueOf(Option.class));

		assertThat(result).isInstanceOfSatisfying(Option.class, it -> assertThat(it.get()).isEqualTo(value));
	}

	@Test // DATACMNS-1165
	@SuppressWarnings("unchecked")
	public void convertsIterableIntoStreamable() {

		Iterable<?> source = Arrays.asList(new Object());

		Object result = handler.postProcessInvocationResult(source, TypeDescriptor.valueOf(Streamable.class));

		assertThat(result).isInstanceOfSatisfying(Streamable.class,
				it -> assertThat(it.stream().collect(Collectors.toList())).isEqualTo(source));
	}

	private static TypeDescriptor getTypeDescriptorFor(String methodName) throws Exception {

		Method method = Sample.class.getMethod(methodName);
		MethodParameter parameter = new MethodParameter(method, -1);

		return TypeDescriptor.nested(parameter, 0);
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
	}

	static class Entity {}
}
