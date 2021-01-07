/*
 * Copyright 2015-2021 the original author or authors.
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
package org.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.reactivex.Flowable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * Unit tests for {@link ResultProcessor}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class ResultProcessorUnitTests {

	@Test // DATACMNS-89
	void leavesNonProjectingResultUntouched() throws Exception {

		ResultProcessor information = new ResultProcessor(getQueryMethod("findAll"), new SpelAwareProxyProjectionFactory());

		Sample sample = new Sample("Dave", "Matthews");
		List<Sample> result = new ArrayList<>(Collections.singletonList(sample));
		List<Sample> converted = information.processResult(result);

		assertThat(converted).contains(sample);
	}

	@Test // DATACMNS-89
	void createsProjectionFromProperties() throws Exception {

		ResultProcessor information = getProcessor("findOneProjection");

		SampleProjection result = information.processResult(Collections.singletonList("Matthews"));

		assertThat(result.getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-89
	void createsListOfProjectionsFormNestedLists() throws Exception {

		ResultProcessor information = getProcessor("findAllProjection");

		List<String> columns = Collections.singletonList("Matthews");
		List<List<String>> source = new ArrayList<>(Collections.singletonList(columns));

		List<SampleProjection> result = information.processResult(source);

		assertThat(result).extracting(SampleProjection::getLastname).containsExactly("Matthews");
	}

	@Test // DATACMNS-89
	@SuppressWarnings("unchecked")
	void createsListOfProjectionsFromMaps() throws Exception {

		ResultProcessor information = getProcessor("findAllProjection");

		List<Map<String, Object>> source = new ArrayList<>(
				Collections.singletonList(Collections.singletonMap("lastname", "Matthews")));

		List<SampleProjection> result = information.processResult(source);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-89
	void createsListOfProjectionsFromEntity() throws Exception {

		ResultProcessor information = getProcessor("findAllProjection");

		List<Sample> source = new ArrayList<>(Collections.singletonList(new Sample("Dave", "Matthews")));
		List<SampleProjection> result = information.processResult(source);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-89
	void createsPageOfProjectionsFromEntity() throws Exception {

		ResultProcessor information = getProcessor("findPageProjection", Pageable.class);

		Page<Sample> source = new PageImpl<>(Collections.singletonList(new Sample("Dave", "Matthews")));
		Page<SampleProjection> result = information.processResult(source);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-89
	void createsDynamicProjectionFromEntity() throws Exception {

		ResultProcessor information = getProcessor("findOneOpenProjection");

		OpenProjection result = information.processResult(new Sample("Dave", "Matthews"));

		assertThat(result.getLastname()).isEqualTo("Matthews");
		assertThat(result.getFullName()).isEqualTo("Dave Matthews");
	}

	@Test // DATACMNS-89
	void findsDynamicProjection() throws Exception {

		ParameterAccessor accessor = mock(ParameterAccessor.class);

		ResultProcessor factory = getProcessor("findOneDynamic", Class.class);
		assertThat(factory.withDynamicProjection(accessor)).isEqualTo(factory);

		doReturn(SampleProjection.class).when(accessor).findDynamicProjection();

		ResultProcessor processor = factory.withDynamicProjection(accessor);
		assertThat(processor.getReturnedType().getReturnedType()).isEqualTo(SampleProjection.class);
	}

	@Test // DATACMNS-89
	void refrainsFromProjectingIfThePreparingConverterReturnsACompatibleInstance() throws Exception {

		Object result = getProcessor("findAllDtos").processResult(new Sample("Dave", "Matthews"),
				source -> new SampleDto());

		assertThat(result).isInstanceOf(SampleDto.class);
	}

	@Test // DATACMNS-828
	void returnsNullResultAsIs() throws Exception {
		Object result = getProcessor("findOneDto").processResult(null);
		assertThat(result).isNull();
	}

	@Test // DATACMNS-842
	void supportsSlicesAsReturnWrapper() throws Exception {

		Slice<Sample> slice = new SliceImpl<>(Collections.singletonList(new Sample("Dave", "Matthews")));

		Object result = getProcessor("findSliceProjection", Pageable.class).processResult(slice);

		assertThat(result).isInstanceOf(Slice.class);

		List<?> content = ((Slice<?>) result).getContent();

		assertThat(content).hasSize(1).hasOnlyElementsOfType(SampleProjection.class);
	}

	@Test // DATACMNS-859
	@SuppressWarnings("unchecked")
	void supportsStreamAsReturnWrapper() throws Exception {

		Stream<Sample> samples = Collections.singletonList(new Sample("Dave", "Matthews")).stream();

		Object result = getProcessor("findStreamProjection").processResult(samples);

		assertThat(result).isInstanceOf(Stream.class);
		List<Object> content = ((Stream<Object>) result).collect(Collectors.toList());

		assertThat(content).hasSize(1).hasOnlyElementsOfType(SampleProjection.class);
	}

	@Test // DATACMNS-860
	void supportsWrappingDto() throws Exception {

		Object result = getProcessor("findOneWrappingDto").processResult(new Sample("Dave", "Matthews"));

		assertThat(result).isInstanceOf(WrappingDto.class);
	}

	@Test // DATACMNS-921
	void fallsBackToApproximateCollectionIfNecessary() throws Exception {

		ResultProcessor processor = getProcessor("findAllProjection");

		SpecialList<Sample> specialList = new SpecialList<>(new Object());
		specialList.add(new Sample("Dave", "Matthews"));

		processor.processResult(specialList);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void supportsMonoWrapper() throws Exception {

		Mono<Sample> samples = Mono.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findMonoSample").processResult(samples);

		assertThat(result).isInstanceOf(Mono.class);

		Object content = ((Mono<Object>) result).block();

		assertThat(content).isInstanceOf(Sample.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void supportsSingleWrapper() throws Exception {

		Single<Sample> samples = Single.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findSingleSample").processResult(samples);

		assertThat(result).isInstanceOf(Single.class);

		Object content = ((Single<Object>) result).toBlocking().value();

		assertThat(content).isInstanceOf(Sample.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void refrainsFromProjectingUsingReactiveWrappersIfThePreparingConverterReturnsACompatibleInstance()
			throws Exception {

		ResultProcessor processor = getProcessor("findMonoSampleDto");

		Object result = processor.processResult(Mono.just(new Sample("Dave", "Matthews")), source -> new SampleDto());

		assertThat(result).isInstanceOf(Mono.class);

		Object content = ((Mono<Object>) result).block();

		assertThat(content).isInstanceOf(SampleDto.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void supportsFluxProjections() throws Exception {

		Flux<Sample> samples = Flux.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findFluxProjection").processResult(samples);

		assertThat(result).isInstanceOf(Flux.class);

		List<Object> content = ((Flux<Object>) result).collectList().block();

		assertThat(content).isNotEmpty();
		assertThat(content.get(0)).isInstanceOf(SampleProjection.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void supportsObservableProjections() throws Exception {

		Observable<Sample> samples = Observable.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findObservableProjection").processResult(samples);

		assertThat(result).isInstanceOf(Observable.class);

		List<Object> content = ((Observable<Object>) result).toList().toBlocking().single();

		assertThat(content).isNotEmpty();
		assertThat(content.get(0)).isInstanceOf(SampleProjection.class);
	}

	@Test // DATACMNS-988
	@SuppressWarnings("unchecked")
	void supportsFlowableProjections() throws Exception {

		Flowable<Sample> samples = Flowable.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findFlowableProjection").processResult(samples);

		assertThat(result).isInstanceOf(Flowable.class);

		List<Object> content = ((Flowable<Object>) result).toList().blockingGet();

		assertThat(content).isNotEmpty();
		assertThat(content.get(0)).isInstanceOf(SampleProjection.class);
	}

	private static ResultProcessor getProcessor(String methodName, Class<?>... parameters) throws Exception {
		return getQueryMethod(methodName, parameters).getResultProcessor();
	}

	private static QueryMethod getQueryMethod(String name, Class<?>... parameters) throws Exception {

		Method method = SampleRepository.class.getMethod(name, parameters);
		return new QueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory());
	}

	interface SampleRepository extends Repository<Sample, Long> {

		List<Sample> findAll();

		List<SampleDto> findAllDtos();

		List<SampleProjection> findAllProjection();

		Sample findOne();

		SampleDto findOneDto();

		WrappingDto findOneWrappingDto();

		SampleProjection findOneProjection();

		OpenProjection findOneOpenProjection();

		Page<SampleProjection> findPageProjection(Pageable pageable);

		Slice<SampleProjection> findSliceProjection(Pageable pageable);

		<T> T findOneDynamic(Class<T> type);

		Stream<SampleProjection> findStreamProjection();

		Mono<Sample> findMonoSample();

		Mono<SampleDto> findMonoSampleDto();

		Single<Sample> findSingleSample();

		Flux<SampleProjection> findFluxProjection();

		Observable<SampleProjection> findObservableProjection();

		Flowable<SampleProjection> findFlowableProjection();
	}

	static class Sample {
		public String firstname, lastname;

		public Sample(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}
	}

	static class SampleDto {}

	@lombok.Value
	// Needs to be public until https://jira.spring.io/browse/SPR-14304 is resolved
	public static class WrappingDto {
		Sample sample;
	}

	interface SampleProjection {
		String getLastname();
	}

	interface OpenProjection {

		String getLastname();

		@Value("#{target.firstname + ' ' + target.lastname}")
		String getFullName();
	}

	static class SpecialList<E> extends ArrayList<E> {

		private static final long serialVersionUID = -6539525376878522158L;

		public SpecialList(Object dummy) {}
	}
}
