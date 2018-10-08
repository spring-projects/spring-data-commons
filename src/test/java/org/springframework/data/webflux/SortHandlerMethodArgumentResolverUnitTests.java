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

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.web.SortDefault;
import org.springframework.data.web.SortDefault.SortDefaults;
import org.springframework.data.web.SortDefaultUnitTests;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.webflux.SortDefaultUnitTests.SORT_3;

/**
 * Unit tests for {@link SortHandlerMethodArgumentResolver}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Nick Williams
 * @since 1.6
 */
public class SortHandlerMethodArgumentResolverUnitTests extends SortDefaultUnitTests {

    static MethodParameter PARAMETER;

    @BeforeClass
    public static void setUp() throws Exception {
        PARAMETER = new MethodParameter(Controller.class.getMethod("supportedMethod", Sort.class), 0);
    }

    @Test
    public void fallbackToGivenDefaultSort() throws Exception {

        MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
        SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
        Sort fallbackSort = Sort.by(Direction.ASC, "ID");
        resolver.setFallbackSort(fallbackSort);

        Mono<Object> sortMono = resolver.resolveArgument(parameter, TestUtils.getMockBindingContext(), TestUtils.getMockServerWebExchange());
        StepVerifier
                .create(sortMono)
                .expectNext(fallbackSort)
                .verifyComplete();
    }

    @Test
    public void fallbackToDefaultDefaultSort() throws Exception {

        MethodParameter parameter = TestUtils.getParameterOfMethod(getControllerClass(), "unsupportedMethod", String.class);
        SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();

        Mono<Object> sortMono = resolver.resolveArgument(parameter, TestUtils.getMockBindingContext(), TestUtils.getMockServerWebExchange());
        StepVerifier
                .create(sortMono.cast(Sort.class))
                .assertNext(sort -> assertThat(sort.isSorted()).isFalse())
                .verifyComplete();
    }

    @Test
    public void discoversSimpleSortFromRequest() {

        MethodParameter parameter = getParameterOfMethod("simpleDefault");
        Sort reference = Sort.by("bar", "foo");
        MockServerHttpRequest request = getRequestWithSort(reference);

        assertSupportedAndResolvedTo(request, parameter, reference);
    }

    @Test
    public void discoversComplexSortFromRequest() {

        MethodParameter parameter = getParameterOfMethod("simpleDefault");
        Sort reference = Sort.by("bar", "foo").and(Sort.by("fizz", "buzz"));

        assertSupportedAndResolvedTo(getRequestWithSort(reference), parameter, reference);
    }

    @Test
    public void discoversQualifiedSortFromRequest() {

        MethodParameter parameter = getParameterOfMethod("qualifiedSort");
        Sort reference = Sort.by("bar", "foo");

        assertSupportedAndResolvedTo(getRequestWithSort(reference, "qual"), parameter, reference);
    }

    @Test
    public void returnsNullForSortParameterSetToNothing() throws Exception {

        MethodParameter parameter = getParameterOfMethod("supportedMethod");

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/")
                .queryParam("sort", (String) null)
                .build();

        SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
        Mono<Object> result = resolver.resolveArgument(parameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(request));
        StepVerifier
                .create(result.cast(Sort.class))
                .assertNext(sort -> assertThat(sort.isSorted()).isFalse())
                .verifyComplete();
    }

    @Test
    public void requestForMultipleSortPropertiesIsUnmarshalledCorrectly() throws Exception {

        MethodParameter parameter = getParameterOfMethod("supportedMethod");

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/")
                .queryParam("sort", SORT_3)
                .build();


        SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
        Mono<Object> result = resolver.resolveArgument(parameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(request));
        StepVerifier
                .create(result)
                .assertNext(sort -> assertThat(sort).isEqualTo(Sort.by(Direction.ASC, "firstname", "lastname")))
                .verifyComplete();
    }

    @Test
    public void parsesEmptySortToNull() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/")
                .queryParam("sort", "")
                .build();
        StepVerifier
                .create(resolveSort(request, PARAMETER))
                .assertNext(sort -> assertThat(sort.isSorted()).isFalse())
                .verifyComplete();
    }

    @Test
    public void sortParamIsInvalidProperty() throws Exception {
        MockServerHttpRequest request = MockServerHttpRequest
                .post("/")
                .queryParam("sort", ",DESC")
                .build();
        StepVerifier
                .create(resolveSort(request, PARAMETER))
                .assertNext(sort -> assertThat(sort.isSorted()).isFalse())
                .verifyComplete();
    }

    @Test
    public void sortParamIsInvalidPropertyWhenMultiProperty() throws Exception {

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/")
                .queryParam("sort", "property1,,DESC")
                .build();
        StepVerifier
                .create(resolveSort(request, PARAMETER))
                .assertNext(sort -> assertThat(sort).isEqualTo(Sort.by(DESC, "property1")))
                .verifyComplete();
    }

    @Test
    public void sortParamIsEmptyWhenMultiParams() throws Exception {

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/")
                .queryParam("sort", "property,DESC")
                .queryParam("sort", "")
                .build();
        StepVerifier
                .create(resolveSort(request, PARAMETER))
                .assertNext(sort -> assertThat(sort).isEqualTo(Sort.by(DESC, "property")))
                .verifyComplete();
    }

    @Test
    public void parsesCommaParameterForSort() throws Exception {

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/")
                .queryParam("sort", ",")
                .build();

        StepVerifier
                .create(resolveSort(request, PARAMETER))
                .assertNext(sort -> assertThat(sort.isSorted()).isFalse())
                .verifyComplete();
    }

    @Test
    public void doesNotReturnNullWhenAnnotatedWithSortDefault() throws Exception {

        MockServerHttpRequest request = MockServerHttpRequest
                .post("/")
                .queryParam("sort", "")
                .build();


        Mono<Sort> simpleDefault = resolveSort(request, getParameterOfMethod("simpleDefault"));
        StepVerifier
                .create(simpleDefault)
                .assertNext(sort -> assertThat(sort).isEqualTo(Sort.by("firstname", "lastname")))
                .verifyComplete();

        Mono<Sort> containeredDefault = resolveSort(request, getParameterOfMethod("containeredDefault"));
        StepVerifier
                .create(containeredDefault)
                .assertNext(sort -> assertThat(sort).isEqualTo(Sort.by("foo", "bar")))
                .verifyComplete();
    }

    private static Mono<Sort> resolveSort(MockServerHttpRequest request, MethodParameter parameter) throws Exception {

        SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
        return resolver.resolveArgument(parameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(request)).cast(Sort.class);
    }

    private static void assertSupportedAndResolvedTo(MockServerHttpRequest request, MethodParameter parameter, Sort sort) {
        SortHandlerMethodArgumentResolver resolver = new SortHandlerMethodArgumentResolver();
        assertThat(resolver.supportsParameter(parameter)).isTrue();
        try {
            Mono<Object> sortMono = resolver.resolveArgument(parameter, TestUtils.getMockBindingContext(), MockServerWebExchange.from(request));
            StepVerifier
                    .create(sortMono)
                    .assertNext(actualSort -> assertThat(actualSort).isEqualTo(sort))
                    .verifyComplete();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MockServerHttpRequest getRequestWithSort(Sort sort) {
        return getRequestWithSort(sort, null);
    }

    private static MockServerHttpRequest getRequestWithSort(Sort sort, String qualifier) {

        if (sort == null) {
            return MockServerHttpRequest.post("/").build();
        }

        MockServerHttpRequest.BodyBuilder builder = MockServerHttpRequest.post("/");
        for (Order order : sort) {
            String prefix = StringUtils.hasText(qualifier) ? qualifier + "_" : "";
            builder.queryParam(prefix + "sort", String.format("%s,%s", order.getProperty(), order.getDirection().name()));
        }

        return builder.build();
    }

    @Override
    protected Class<?> getControllerClass() {
        return Controller.class;
    }

    interface Controller {

        void supportedMethod(Sort sort);

        void unsupportedMethod(String string);

        void qualifiedSort(@Qualifier("qual") Sort sort);

        void simpleDefault(@SortDefault({"firstname", "lastname"}) Sort sort);

        void simpleDefaultWithDirection(
                @SortDefault(sort = {"firstname", "lastname"}, direction = Direction.DESC) Sort sort);

        void containeredDefault(@SortDefaults(@SortDefault({"foo", "bar"})) Sort sort);

        void invalid(@SortDefaults(@SortDefault({"foo", "bar"})) @SortDefault({"bar", "foo"}) Sort sort);
    }
}
