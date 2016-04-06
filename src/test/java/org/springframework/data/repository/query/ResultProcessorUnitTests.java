/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

/**
 * Unit tests for {@link ResultProcessor}.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 */
public class ResultProcessorUnitTests {

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void leavesNonProjectingResultUntouched() throws Exception {

		ResultProcessor information = new ResultProcessor(getQueryMethod("findAll"), new SpelAwareProxyProjectionFactory());

		Sample sample = new Sample("Dave", "Matthews");
		List<Sample> result = new ArrayList<Sample>(Arrays.asList(sample));
		List<Sample> converted = information.processResult(result);

		assertThat(converted, contains(sample));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void createsProjectionFromProperties() throws Exception {

		ResultProcessor information = getProcessor("findOneProjection");

		SampleProjection result = information.processResult(Arrays.asList("Matthews"));

		assertThat(result.getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void createsListOfProjectionsFormNestedLists() throws Exception {

		ResultProcessor information = getProcessor("findAllProjection");

		List<String> columns = Arrays.asList("Matthews");
		List<List<String>> source = new ArrayList<List<String>>(Arrays.asList(columns));

		List<SampleProjection> result = information.processResult(source);

		assertThat(result, hasSize(1));
		assertThat(result.get(0).getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void createsListOfProjectionsFromMaps() throws Exception {

		ResultProcessor information = getProcessor("findAllProjection");

		List<Map<String, Object>> source = new ArrayList<Map<String, Object>>(
				Arrays.asList(Collections.<String, Object> singletonMap("lastname", "Matthews")));

		List<SampleProjection> result = information.processResult(source);

		assertThat(result, hasSize(1));
		assertThat(result.get(0).getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void createsListOfProjectionsFromEntity() throws Exception {

		ResultProcessor information = getProcessor("findAllProjection");

		List<Sample> source = new ArrayList<Sample>(Arrays.asList(new Sample("Dave", "Matthews")));
		List<SampleProjection> result = information.processResult(source);

		assertThat(result, hasSize(1));
		assertThat(result.get(0).getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void createsPageOfProjectionsFromEntity() throws Exception {

		ResultProcessor information = getProcessor("findPageProjection", Pageable.class);

		Page<Sample> source = new PageImpl<Sample>(Arrays.asList(new Sample("Dave", "Matthews")));
		Page<SampleProjection> result = information.processResult(source);

		assertThat(result.getContent(), hasSize(1));
		assertThat(result.getContent().get(0).getLastname(), is("Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void createsDynamicProjectionFromEntity() throws Exception {

		ResultProcessor information = getProcessor("findOneOpenProjection");

		OpenProjection result = information.processResult(new Sample("Dave", "Matthews"));

		assertThat(result.getLastname(), is("Matthews"));
		assertThat(result.getFullName(), is("Dave Matthews"));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void findsDynamicProjection() throws Exception {

		ParameterAccessor accessor = mock(ParameterAccessor.class);

		ResultProcessor factory = getProcessor("findOneDynamic", Class.class);
		assertThat(factory.withDynamicProjection(null), is(factory));
		assertThat(factory.withDynamicProjection(accessor), is(factory));

		doReturn(SampleProjection.class).when(accessor).getDynamicProjection();

		ResultProcessor processor = factory.withDynamicProjection(accessor);
		assertThat(processor.getReturnedType().getReturnedType(), is(typeCompatibleWith(SampleProjection.class)));
	}

	/**
	 * @see DATACMNS-89
	 */
	@Test
	public void refrainsFromProjectingIfThePreparingConverterReturnsACompatibleInstance() throws Exception {

		ResultProcessor processor = getProcessor("findAllDtos");

		Object result = processor.processResult(new Sample("Dave", "Matthews"), new Converter<Object, Object>() {

			@Override
			public Object convert(Object source) {
				return new SampleDto();
			}
		});

		assertThat(result, is(instanceOf(SampleDto.class)));
	}

	/**
	 * @see DATACMNS-828
	 */
	@Test
	public void returnsNullResultAsIs() throws Exception {
		assertThat(getProcessor("findOneDto").processResult(null), is(nullValue()));
	}

	/**
	 * @see DATACMNS-842
	 */
	@Test
	public void supportsSlicesAsReturnWrapper() throws Exception {

		Slice<Sample> slice = new SliceImpl<Sample>(Collections.singletonList(new Sample("Dave", "Matthews")));

		Object result = getProcessor("findSliceProjection", Pageable.class).processResult(slice);

		assertThat(result, is(instanceOf(Slice.class)));

		List<?> content = ((Slice<?>) result).getContent();
		assertThat(content, is(not(empty())));
		assertThat(content.get(0), is(instanceOf(SampleProjection.class)));
	}

	/**
	 * @see DATACMNS-859
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void supportsStreamAsReturnWrapper() throws Exception {

		Stream<Sample> samples = Arrays.asList(new Sample("Dave", "Matthews")).stream();

		Object result = getProcessor("findStreamProjection").processResult(samples);

		assertThat(result, is(instanceOf(Stream.class)));
		List<Object> content = ((Stream<Object>) result).collect(Collectors.toList());

		assertThat(content, is(not(empty())));
		assertThat(content.get(0), is(instanceOf(SampleProjection.class)));
	}

	/**
	 * @see DATACMNS-860
	 */
	@Test
	public void supportsWrappingDto() throws Exception {

		Object result = getProcessor("findOneWrappingDto").processResult(new Sample("Dave", "Matthews"));

		assertThat(result, is(instanceOf(WrappingDto.class)));
	}

	/**
	 * @see DATACMNS-921
	 */
	@Test
	public void fallsBackToApproximateCollectionIfNecessary() throws Exception {

		ResultProcessor processor = getProcessor("findAllProjection");

		SpecialList<Sample> specialList = new SpecialList<Sample>(new Object());
		specialList.add(new Sample("Dave", "Matthews"));

		processor.processResult(specialList);
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void supportsMonoWrapper() throws Exception {

		Mono<Sample> samples = Mono.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findMonoSample").processResult(samples);

		assertThat(result, is(instanceOf(Mono.class)));
		Object content = ((Mono<Object>) result).block();

		assertThat(content, is(instanceOf(Sample.class)));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void supportsSingleWrapper() throws Exception {

		Single<Sample> samples = Single.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findSingleSample").processResult(samples);

		assertThat(result, is(instanceOf(Single.class)));
		Object content = ((Single<Object>) result).toBlocking().value();

		assertThat(content, is(instanceOf(Sample.class)));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	public void refrainsFromProjectingUsingReactiveWrappersIfThePreparingConverterReturnsACompatibleInstance()
			throws Exception {

		ResultProcessor processor = getProcessor("findMonoSampleDto");

		Object result = processor.processResult(Mono.just(new Sample("Dave", "Matthews")), new Converter<Object, Object>() {

			@Override
			public Object convert(Object source) {
				return new SampleDto();
			}
		});

		assertThat(result, is(instanceOf(Mono.class)));
		Object content = ((Mono<Object>) result).block();

		assertThat(content, is(instanceOf(SampleDto.class)));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void supportsFluxProjections() throws Exception {

		Flux<Sample> samples = Flux.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findFluxProjection").processResult(samples);

		assertThat(result, is(instanceOf(Flux.class)));
		List<Object> content = ((Flux<Object>) result).collectList().block();

		assertThat(content, is(not(empty())));
		assertThat(content.get(0), is(instanceOf(SampleProjection.class)));
	}

	/**
	 * @see DATACMNS-836
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void supportsObservableProjections() throws Exception {

		Observable<Sample> samples = Observable.just(new Sample("Dave", "Matthews"));

		Object result = getProcessor("findObservableProjection").processResult(samples);

		assertThat(result, is(instanceOf(Observable.class)));
		List<Object> content = ((Observable<Object>) result).toList().toBlocking().single();

		assertThat(content, is(not(empty())));
		assertThat(content.get(0), is(instanceOf(SampleProjection.class)));
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
