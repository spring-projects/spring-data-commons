/*
 * Copyright 2014-2016 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.repository.Repository;

/**
 * Unit tests for {@link QueryExecutionResultHandler}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class QueryExecutionResultHandlerUnitTests {

	QueryExecutionResultHandler handler = new QueryExecutionResultHandler();

	/**
	 * @see DATACMNS-610
	 */
	@Test
	public void convertsListsToSet() throws Exception {

		TypeDescriptor descriptor = getTypeDescriptorFor("set");
		List<Entity> source = Collections.singletonList(new Entity());

		assertThat(handler.postProcessInvocationResult(source, descriptor), is(instanceOf(Set.class)));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	public void turnsNullIntoJdk8Optional() throws Exception {

		Object result = handler.postProcessInvocationResult(null, getTypeDescriptorFor("jdk8Optional"));
		assertThat(result, is((Object) Optional.empty()));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void wrapsValueIntoJdk8Optional() throws Exception {

		Entity entity = new Entity();

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("jdk8Optional"));
		assertThat(result, is(instanceOf(Optional.class)));

		Optional<Entity> optional = (Optional<Entity>) result;
		assertThat(optional, is(Optional.of(entity)));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	public void turnsNullIntoGuavaOptional() throws Exception {

		Object result = handler.postProcessInvocationResult(null, getTypeDescriptorFor("guavaOptional"));
		assertThat(result, is((Object) com.google.common.base.Optional.absent()));
	}

	/**
	 * @see DATACMNS-483
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void wrapsValueIntoGuavaOptional() throws Exception {

		Entity entity = new Entity();

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("guavaOptional"));
		assertThat(result, is(instanceOf(com.google.common.base.Optional.class)));

		com.google.common.base.Optional<Entity> optional = (com.google.common.base.Optional<Entity>) result;
		assertThat(optional, is(com.google.common.base.Optional.of(entity)));
	}

	/**
	 * @see DATACMNS-917
	 */
	@Test
	public void defaultsNullToEmptyMap() throws Exception {
		assertThat(handler.postProcessInvocationResult(null, getTypeDescriptorFor("map")), is(instanceOf(Map.class)));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaSingleIntoPublisher() throws Exception {

		Single<Entity> entity = Single.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("publisher"));
		assertThat(result, is(instanceOf(Publisher.class)));

		Mono<Entity> mono = Mono.from((Publisher<Entity>) result);
		assertThat(mono.block(), is(entity.toBlocking().value()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaSingleIntoMono() throws Exception {

		Single<Entity> entity = Single.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result, is(instanceOf(Mono.class)));

		Mono<Entity> mono = (Mono<Entity>) result;
		assertThat(mono.block(), is(entity.toBlocking().value()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaSingleIntoFlux() throws Exception {

		Single<Entity> entity = Single.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("flux"));
		assertThat(result, is(instanceOf(Flux.class)));

		Flux<Entity> flux = (Flux<Entity>) result;
		assertThat(flux.next().block(), is(entity.toBlocking().value()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaObservableIntoPublisher() throws Exception {

		Observable<Entity> entity = Observable.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("publisher"));
		assertThat(result, is(instanceOf(Publisher.class)));

		Mono<Entity> mono = Mono.from((Publisher<Entity>) result);
		assertThat(mono.block(), is(entity.toBlocking().first()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaObservableIntoMono() throws Exception {

		Observable<Entity> entity = Observable.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result, is(instanceOf(Mono.class)));

		Mono<Entity> mono = (Mono<Entity>) result;
		assertThat(mono.block(), is(entity.toBlocking().first()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaObservableIntoFlux() throws Exception {

		Observable<Entity> entity = Observable.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("flux"));
		assertThat(result, is(instanceOf(Flux.class)));

		Flux<Entity> flux = (Flux<Entity>) result;
		assertThat(flux.next().block(), is(entity.toBlocking().first()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaObservableIntoSingle() throws Exception {

		Observable<Entity> entity = Observable.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("single"));
		assertThat(result, is(instanceOf(Single.class)));

		Single<Entity> single = (Single<Entity>) result;
		assertThat(single.toBlocking().value(), is(entity.toBlocking().first()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaSingleIntoObservable() throws Exception {

		Single<Entity> entity = Single.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("observable"));
		assertThat(result, is(instanceOf(Observable.class)));

		Observable<Entity> observable = (Observable<Entity>) result;
		assertThat(observable.toBlocking().first(), is(entity.toBlocking().value()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoSingle() throws Exception {

		Mono<Entity> entity = Mono.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("single"));
		assertThat(result, is(instanceOf(Single.class)));

		Single<Entity> single = (Single<Entity>) result;
		assertThat(single.toBlocking().value(), is(entity.block()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoCompletable() throws Exception {

		Mono<Entity> entity = Mono.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("completable"));
		assertThat(result, is(instanceOf(Completable.class)));

		Completable completable = (Completable) result;
		assertThat(completable.get(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoCompletableWithException() throws Exception {

		Mono<Entity> entity = Mono.error(new InvalidDataAccessApiUsageException("err"));

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("completable"));
		assertThat(result, is(instanceOf(Completable.class)));

		Completable completable = (Completable) result;
		assertThat(completable.get(), is(instanceOf(InvalidDataAccessApiUsageException.class)));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsRxJavaCompletableIntoMono() throws Exception {

		Completable entity = Completable.complete();

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result, is(instanceOf(Mono.class)));

		Mono mono = (Mono) result;
		assertThat(mono.block(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test(expected = InvalidDataAccessApiUsageException.class)
	@SuppressWarnings("unchecked")
	public void convertsRxJavaCompletableIntoMonoWithException() throws Exception {

		Completable entity = Completable.error(new InvalidDataAccessApiUsageException("err"));

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result, is(instanceOf(Mono.class)));

		Mono mono = (Mono) result;
		mono.block();
		fail("Missing InvalidDataAccessApiUsageException");
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoObservable() throws Exception {

		Mono<Entity> entity = Mono.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("observable"));
		assertThat(result, is(instanceOf(Observable.class)));

		Observable<Entity> observable = (Observable<Entity>) result;
		assertThat(observable.toBlocking().first(), is(entity.block()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsReactorFluxIntoSingle() throws Exception {

		Flux<Entity> entity = Flux.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("single"));
		assertThat(result, is(instanceOf(Single.class)));

		Single<Entity> single = (Single<Entity>) result;
		assertThat(single.toBlocking().value(), is(entity.next().block()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsReactorFluxIntoObservable() throws Exception {

		Flux<Entity> entity = Flux.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("observable"));
		assertThat(result, is(instanceOf(Observable.class)));

		Observable<Entity> observable = (Observable<Entity>) result;
		assertThat(observable.toBlocking().first(), is(entity.next().block()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsReactorFluxIntoMono() throws Exception {

		Flux<Entity> entity = Flux.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("mono"));
		assertThat(result, is(instanceOf(Mono.class)));

		Mono<Entity> mono = (Mono<Entity>) result;
		assertThat(mono.block(), is(entity.next().block()));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void convertsReactorMonoIntoFlux() throws Exception {

		Mono<Entity> entity = Mono.just(new Entity());

		Object result = handler.postProcessInvocationResult(entity, getTypeDescriptorFor("flux"));
		assertThat(result, is(instanceOf(Flux.class)));

		Flux<Entity> flux = (Flux<Entity>) result;
		assertThat(flux.next().block(), is(entity.block()));
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
