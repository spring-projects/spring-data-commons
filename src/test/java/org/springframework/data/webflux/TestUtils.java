package org.springframework.data.webflux;

import org.springframework.core.MethodParameter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.BindingContext;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;

/**
 * @author Eugene Utkin
 */
class TestUtils {

    static BindingContext getMockBindingContext() {
        return mock(BindingContext.class);
    }

    static MockServerWebExchange getMockServerWebExchange() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        return MockServerWebExchange.from(request);
    }

    static MethodParameter getParameterOfMethod(Class<?> controller, String name, Class<?>... argumentTypes) {

        Method method = getMethod(controller, name, argumentTypes);
        return new MethodParameter(method, 0);
    }

    static Method getMethod(Class<?> controller, String name, Class<?>... argumentTypes) {

        try {
            return controller.getMethod(name, argumentTypes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
