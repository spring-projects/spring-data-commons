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

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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

		var information = new ResultProcessor(getQueryMethod("findAll"), new SpelAwareProxyProjectionFactory());

		var sample = new Sample("Dave", "Matthews");
		List<Sample> result = new ArrayList<>(Collections.singletonList(sample));
		List<Sample> converted = information.processResult(result);

		assertThat(converted).contains(sample);
	}

	@Test // DATACMNS-89
	void createsProjectionFromProperties() throws Exception {

		var information = getProcessor("findOneProjection");

		SampleProjection result = information.processResult(Collections.singletonList("Matthews"));

		assertThat(result.getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-89
	void createsListOfProjectionsFormNestedLists() throws Exception {

		var information = getProcessor("findAllProjection");

		var columns = Collections.singletonList("Matthews");
		List<List<String>> source = new ArrayList<>(Collections.singletonList(columns));

		List<SampleProjection> result = information.processResult(source);

		assertThat(result).extracting(SampleProjection::getLastname).containsExactly("Matthews");
	}

	@Test // DATACMNS-89
	@SuppressWarnings("unchecked")
	void createsListOfProjectionsFromMaps() throws Exception {

		var information = getProcessor("findAllProjection");

		List<Map<String, Object>> source = new ArrayList<>(
				Collections.singletonList(Collections.singletonMap("lastname", "Matthews")));

		List<SampleProjection> result = information.processResult(source);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-89
	void createsListOfProjectionsFromEntity() throws Exception {

		var information = getProcessor("findAllProjection");

		List<Sample> source = new ArrayList<>(Collections.singletonList(new Sample("Dave", "Matthews")));
		List<SampleProjection> result = information.processResult(source);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-89
	void createsPageOfProjectionsFromEntity() throws Exception {

		var information = getProcessor("findPageProjection", Pageable.class);

		Page<Sample> source = new PageImpl<>(Collections.singletonList(new Sample("Dave", "Matthews")));
		Page<SampleProjection> result = information.processResult(source);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getLastname()).isEqualTo("Matthews");
	}

	@Test // DATACMNS-89
	void createsDynamicProjectionFromEntity() throws Exception {

		var information = getProcessor("findOneOpenProjection");

		OpenProjection result = information.processResult(new Sample("Dave", "Matthews"));

		assertThat(result.getLastname()).isEqualTo("Matthews");
		assertThat(result.getFullName()).isEqualTo("Dave Matthews");
	}

	@Test // DATACMNS-89
	void findsDynamicProjection() throws Exception {

		var accessor = mock(ParameterAccessor.class);

		var factory = getProcessor("findOneDynamic", Class.class);
		assertThat(factory.withDynamicProjection(accessor)).isEqualTo(factory);

		doReturn(SampleProjection.class).when(accessor).findDynamicProjection();

		var processor = factory.withDynamicProjection(accessor);
		assertThat(processor.getReturnedType().getReturnedType()).isEqualTo(SampleProjection.class);
	}

	@Test // DATACMNS-89
	void refrainsFromProjectingIfThePreparingConverterReturnsACompatibleInstance() throws Exception {

		var result = getProcessor("findAllDtos").processResult(new Sample("Dave", "Matthews"),
				source -> new SampleDto());

		assertThat(result).isInstanceOf(SampleDto.class);
	}

	@Test // DATACMNS-828
	void returnsNullResultAsIs() throws Exception {
		var result = getProcessor("findOneDto").processResult(null);
		assertThat(result).isNull();
	}

	@Test // DATACMNS-842
	void supportsSlicesAsReturnWrapper() throws Exception {

		Slice<Sample> slice = new SliceImpl<>(Collections.singletonList(new Sample("Dave", "Matthews")));

		var result = getProcessor("findSliceProjection", Pageable.class).processResult(slice);

		assertThat(result).isInstanceOf(Slice.class);

		var content = ((Slice<?>) result).getContent();

		assertThat(content).hasSize(1).hasOnlyElementsOfType(SampleProjection.class);
	}

	@Test // DATACMNS-859
	@SuppressWarnings("unchecked")
	void supportsStreamAsReturnWrapper() throws Exception {

		var samples = Collections.singletonList(new Sample("Dave", "Matthews")).stream();

		var result = getProcessor("findStreamProjection").processResult(samples);

		assertThat(result).isInstanceOf(Stream.class);
		var content = ((Stream<Object>) result).collect(Collectors.toList());

		assertThat(content).hasSize(1).hasOnlyElementsOfType(SampleProjection.class);
	}

	@Test // DATACMNS-860
	void supportsWrappingDto() throws Exception {

		var result = getProcessor("findOneWrappingDto").processResult(new Sample("Dave", "Matthews"));

		assertThat(result).isInstanceOf(WrappingDto.class);
	}

	@Test // DATACMNS-921
	void fallsBackToApproximateCollectionIfNecessary() throws Exception {

		var processor = getProcessor("findAllProjection");

		var specialList = new SpecialList<Sample>(new Object());
		specialList.add(new Sample("Dave", "Matthews"));

		processor.processResult(specialList);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void supportsMonoWrapper() throws Exception {

		var samples = Mono.just(new Sample("Dave", "Matthews"));

		var result = getProcessor("findMonoSample").processResult(samples);

		assertThat(result).isInstanceOf(Mono.class);

		var content = ((Mono<Object>) result).block();

		assertThat(content).isInstanceOf(Sample.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void supportsSingleWrapper() throws Exception {

		var samples = Single.just(new Sample("Dave", "Matthews"));

		var result = getProcessor("findSingleSample").processResult(samples);

		assertThat(result).isInstanceOf(Single.class);

		var content = ((Single<Object>) result).blockingGet();

		assertThat(content).isInstanceOf(Sample.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void refrainsFromProjectingUsingReactiveWrappersIfThePreparingConverterReturnsACompatibleInstance()
			throws Exception {

		var processor = getProcessor("findMonoSampleDto");

		var result = processor.processResult(Mono.just(new Sample("Dave", "Matthews")), source -> new SampleDto());

		assertThat(result).isInstanceOf(Mono.class);

		var content = ((Mono<Object>) result).block();

		assertThat(content).isInstanceOf(SampleDto.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void supportsFluxProjections() throws Exception {

		var samples = Flux.just(new Sample("Dave", "Matthews"));

		var result = getProcessor("findFluxProjection").processResult(samples);

		assertThat(result).isInstanceOf(Flux.class);

		var content = ((Flux<Object>) result).collectList().block();

		assertThat(content).isNotEmpty();
		assertThat(content.get(0)).isInstanceOf(SampleProjection.class);
	}

	@Test // DATACMNS-836
	@SuppressWarnings("unchecked")
	void supportsObservableProjections() throws Exception {

		var samples = Observable.just(new Sample("Dave", "Matthews"));

		var result = getProcessor("findObservableProjection").processResult(samples);

		assertThat(result).isInstanceOf(Observable.class);

		var content = ((Observable<Object>) result).toList().blockingGet();

		assertThat(content).isNotEmpty();
		assertThat(content.get(0)).isInstanceOf(SampleProjection.class);
	}

	@Test // DATACMNS-988
	@SuppressWarnings("unchecked")
	void supportsFlowableProjections() throws Exception {

		var samples = Flowable.just(new Sample("Dave", "Matthews"));

		var result = getProcessor("findFlowableProjection").processResult(samples);

		assertThat(result).isInstanceOf(Flowable.class);

		var content = ((Flowable<Object>) result).toList().blockingGet();

		assertThat(content).isNotEmpty();
		assertThat(content.get(0)).isInstanceOf(SampleProjection.class);
	}

	@Test // GH-2347
	void findByListSkipsConversionIfTypeAlreadyMatches() throws Exception {

		List<AbstractDto> result = getProcessor("findAllAbstractDtos")
				.processResult(Collections.singletonList(new ConcreteDto("Walter", "White")));

		assertThat(result.get(0)).isInstanceOf(ConcreteDto.class);
	}

	@Test // GH-2347
	void streamBySkipsConversionIfTypeAlreadyMatches() throws Exception {

		Stream<AbstractDto> result = getProcessor("streamAllAbstractDtos")
				.processResult(Stream.of(new ConcreteDto("Walter", "White")));

		assertThat(result.findFirst().get()).isInstanceOf(ConcreteDto.class);
	}

	@Test // GH-2347
	void findFluxSkipsConversionIfTypeAlreadyMatches() throws Exception {

		Flux<AbstractDto> result = getProcessor("findFluxOfAbstractDtos")
				.processResult(Flux.just(new ConcreteDto("Walter", "White")));

		result.as(StepVerifier::create) //
				.consumeNextWith(actual -> {

					assertThat(actual).isInstanceOf(ConcreteDto.class);
				}).verifyComplete();
	}

	private static ResultProcessor getProcessor(String methodName, Class<?>... parameters) throws Exception {
		return getQueryMethod(methodName, parameters).getResultProcessor();
	}

	private static QueryMethod getQueryMethod(String name, Class<?>... parameters) throws Exception {

		var method = SampleRepository.class.getMethod(name, parameters);
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

		List<AbstractDto> findAllAbstractDtos();

		Stream<AbstractDto> streamAllAbstractDtos();

		Flux<AbstractDto> findFluxOfAbstractDtos();
	}

	static class Sample {
		public String firstname, lastname;

		public Sample(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}
	}

	@Getter
	static abstract class AbstractDto {
		final String firstname, lastname;

		public AbstractDto(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}
	}

	static class ConcreteDto extends AbstractDto {

		public ConcreteDto(String firstname, String lastname) {
			super(firstname, lastname);
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
