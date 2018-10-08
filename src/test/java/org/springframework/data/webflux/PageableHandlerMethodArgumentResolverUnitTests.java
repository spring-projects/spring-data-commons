/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.webflux;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.web.PageableHandlerMethodArgumentResolver.DEFAULT_PAGE_REQUEST;

/**
 * Unit tests for {@link org.springframework.data.web.PageableHandlerMethodArgumentResolver}. Pulls in defaulting tests from
 * {@link PageableDefaultUnitTests}.
 *
 * @author Oliver Gierke
 * @author Nick Williams
 */
public class PageableHandlerMethodArgumentResolverUnitTests extends PageableDefaultUnitTests {

    MethodParameter supportedMethodParameter;

    @Before
    public void setUp() throws Exception {
        this.supportedMethodParameter = new MethodParameter(Sample.class.getMethod("supportedMethod", Pageable.class), 0);
    }

    @Test
    public void preventsPageSizeFromExceedingMayValueIfConfigured() throws Exception {

        MockServerHttpRequest request = MockServerHttpRequest.post("/")
                .queryParam("page", "0")
                .queryParam("size", "200")
                .build();

        assertSupportedAndResult(supportedMethodParameter, PageRequest.of(0, 100), request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyPageParameterName() {
        new PageableHandlerMethodArgumentResolver().setPageParameterName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullPageParameterName() {
        new PageableHandlerMethodArgumentResolver().setPageParameterName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptySizeParameterName() {
        new PageableHandlerMethodArgumentResolver().setSizeParameterName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullSizeParameterName() {
        new PageableHandlerMethodArgumentResolver().setSizeParameterName(null);
    }

    @Test
    public void qualifierIsUsedInParameterLookup() throws Exception {

        MethodParameter parameter = new MethodParameter(Sample.class.getMethod("validQualifier", Pageable.class), 0);

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("foo_page", "2")
                .queryParam("foo_size", "10")
                .build();

        assertSupportedAndResult(parameter, PageRequest.of(2, 10), MockServerWebExchange.from(httpRequest));
    }

    @Test
    public void usesDefaultPageSizeIfRequestPageSizeIsLessThanOne() throws Exception {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("page", "0")
                .queryParam("size", "0")
                .build();
        assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, MockServerWebExchange.from(httpRequest));
    }

    @Test
    public void rejectsInvalidCustomDefaultForPageSize() throws Exception {

        MethodParameter parameter = new MethodParameter(Sample.class.getMethod("invalidDefaultPageSize", Pageable.class),
                0);


        Mono<Pageable> result = getResolver()
                .resolveArgument(parameter, TestUtils.getMockBindingContext(), TestUtils.getMockServerWebExchange())
                .cast(Pageable.class);

        StepVerifier.create(result)
                .expectError(IllegalStateException.class)
                .verify();

    }

    @Test
    public void fallsBackToFirstPageIfNegativePageNumberIsGiven() throws Exception {
        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("page", "-1")
                .build();

        assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, MockServerWebExchange.from(httpRequest));
    }

    @Test
    public void pageParamIsNotNumeric() throws Exception {

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("size", "a")
                .build();
        assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST,  MockServerWebExchange.from(httpRequest));
    }

    @Test
    public void sizeParamIsNotNumeric() throws Exception {

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("size", "a")
                .build();


        assertSupportedAndResult(supportedMethodParameter, DEFAULT_PAGE_REQUEST, MockServerWebExchange.from(httpRequest));
    }

    @Test
    public void returnsNullIfFallbackIsUnpagedAndNoParametersGiven() throws Exception {

        PageableHandlerMethodArgumentResolver resolver = getResolver();
        resolver.setFallbackPageable(Pageable.unpaged());

        assertSupportedAndResult(supportedMethodParameter, Pageable.unpaged(),
               TestUtils.getMockServerWebExchange(), resolver);

    }

    @Test
    public void returnsFallbackIfOnlyPageIsGiven() throws Exception {

        PageableHandlerMethodArgumentResolver resolver = getResolver();
        resolver.setFallbackPageable(Pageable.unpaged());


        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("page", "20")
                .build();

        Mono<Pageable> result = resolver
                .resolveArgument(supportedMethodParameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(httpRequest))
                .cast(Pageable.class);

        StepVerifier.create(result)
                .assertNext(it -> assertThat(it).isEqualTo(Pageable.unpaged()))
                .verifyComplete();
    }

    @Test
    public void returnsFallbackIfFallbackIsUnpagedAndOnlySizeIsGiven() throws Exception {

        PageableHandlerMethodArgumentResolver resolver = getResolver();
        resolver.setFallbackPageable(Pageable.unpaged());

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("size", "10")
                .build();

        Mono<Pageable> result = resolver
                .resolveArgument(supportedMethodParameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(httpRequest))
                .cast(Pageable.class);

        StepVerifier.create(result)
                .assertNext(it -> assertThat(it).isEqualTo(Pageable.unpaged()))
                .verifyComplete();
    }

    @Test
    public void considersOneIndexedParametersSetting() {

        PageableHandlerMethodArgumentResolver resolver = getResolver();
        resolver.setOneIndexedParameters(true);

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("page", "1")
                .build();

        Mono<Pageable> result = resolver
                .resolveArgument(supportedMethodParameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(httpRequest))
                .cast(Pageable.class);

        StepVerifier.create(result)
                .assertNext(it -> assertThat(it.getPageNumber()).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    public void usesNullSortIfNoDefaultIsConfiguredAndPageAndSizeAreGiven() {

        PageableHandlerMethodArgumentResolver resolver = getResolver();
        resolver.setFallbackPageable(Pageable.unpaged());

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("page", "0")
                .queryParam("size", "10")
                .build();

        Mono<Pageable> result = resolver
                .resolveArgument(supportedMethodParameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(httpRequest))
                .cast(Pageable.class);

        StepVerifier.create(result)
                .assertNext(it -> {
                    assertThat(it.getPageNumber()).isEqualTo(0);
                    assertThat(it.getPageSize()).isEqualTo(10);
                    assertThat(it.getSort().isSorted()).isFalse();
                })
                .verifyComplete();

    }

    @Test
    public void oneIndexedParametersDefaultsIndexOutOfRange() {

        PageableHandlerMethodArgumentResolver resolver = getResolver();
        resolver.setOneIndexedParameters(true);

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("page", "0")
                .build();

        Mono<Pageable> result = resolver
                .resolveArgument(supportedMethodParameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(httpRequest))
                .cast(Pageable.class);

        StepVerifier.create(result)
                .assertNext(it -> assertThat(it.getPageNumber()).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    public void returnsCorrectPageSizeForOneIndexParameters() {

        PageableHandlerMethodArgumentResolver resolver = getResolver();
        resolver.setOneIndexedParameters(true);

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .queryParam("size", "10")
                .build();

        Mono<Pageable> result = resolver
                .resolveArgument(supportedMethodParameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(httpRequest))
                .cast(Pageable.class);

        StepVerifier.create(result)
                .assertNext(it -> assertThat(it.getPageSize()).isEqualTo(10))
                .verifyComplete();
    }

    @Test
    public void detectsFallbackPageableIfNullOneIsConfigured() {

        PageableHandlerMethodArgumentResolver resolver = getResolver();
        resolver.setFallbackPageable(Pageable.unpaged());

        assertThat(resolver.isFallbackPageable(null)).isFalse();
        assertThat(resolver.isFallbackPageable(PageRequest.of(0, 10))).isFalse();
    }

    @Override
    protected PageableHandlerMethodArgumentResolver getResolver() {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(100);
        return resolver;
    }

    @Override
    protected Class<?> getControllerClass() {
        return Sample.class;
    }

    interface Sample {

        void supportedMethod(Pageable pageable);

        void unsupportedMethod(String string);

        void invalidDefaultPageSize(@PageableDefault(size = 0) Pageable pageable);

        void simpleDefault(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER) Pageable pageable);

        void simpleDefaultWithSort(
                @PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER, sort = {"firstname", "lastname"}) Pageable pageable);

        void simpleDefaultWithSortAndDirection(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER,
                sort = {"firstname", "lastname"}, direction = Direction.DESC) Pageable pageable);

        void simpleDefaultWithExternalSort(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER) //
                                           @SortDefault(sort = {"firstname", "lastname"}, direction = Direction.DESC) Pageable pageable);

        void simpleDefaultWithContaineredExternalSort(@PageableDefault(size = PAGE_SIZE, page = PAGE_NUMBER) //
                                                      @SortDefaults(@SortDefault(sort = {"firstname", "lastname"}, direction = Direction.DESC)) Pageable pageable);

        void invalidQualifiers(@Qualifier("foo") Pageable first, @Qualifier("foo") Pageable second);

        void validQualifier(@Qualifier("foo") Pageable pageable);

        void noQualifiers(Pageable first, Pageable second);
    }
}
