package org.springframework.data.webflux;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.webcommon.AbstractSortHandlerMethodArgumentResolver;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * {@link HandlerMethodArgumentResolver} to automatically create {@link Sort} instances from request parameters or
 * {@link org.springframework.data.web.SortDefault} annotations.
 *
 * @author Eugene Utkin
 */
public class SortHandlerMethodArgumentResolver
        extends AbstractSortHandlerMethodArgumentResolver
        implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Sort.class.equals(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
        List<String> directionParameter = exchange.getRequest().getQueryParams().get(getSortParameter(parameter));
        Sort sort = resolveSortArgument(directionParameter, parameter);
        return sort == null ? Mono.empty() : Mono.just(sort);
    }

}
