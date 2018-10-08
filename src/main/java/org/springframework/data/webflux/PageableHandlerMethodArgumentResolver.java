package org.springframework.data.webflux;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.webcommon.AbstractPageableHandlerMethodArgumentResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.data.web.SpringDataAnnotationUtils.assertPageableUniqueness;

/**
 * Extracts paging information from web requests and thus allows injecting {@link Pageable} instances into controller
 * methods. Request properties to be parsed can be configured. Default configuration uses request parameters beginning
 * with {@link #DEFAULT_PAGE_PARAMETER}{@link #DEFAULT_QUALIFIER_DELIMITER}.
 *
 * @author Eugene Utkin
 */
public class PageableHandlerMethodArgumentResolver
        extends AbstractPageableHandlerMethodArgumentResolver
        implements HandlerMethodArgumentResolver {

    private static final HandlerMethodArgumentResolver DEFAULT_SORT_RESOLVER = new SortHandlerMethodArgumentResolver();

    /**
     * Constructs an instance of this resolved with a default {@link SortHandlerMethodArgumentResolver}.
     */
    public PageableHandlerMethodArgumentResolver() {
        this(null);
    }


    /**
     * Constructs an instance of this resolver with the specified {@link SortHandlerMethodArgumentResolver}.
     *
     * @param sortResolver the sort resolver to use
     * @since 1.13
     */
    public PageableHandlerMethodArgumentResolver(@Nullable SortHandlerMethodArgumentResolver sortResolver) {
        this.sortResolver = sortResolver == null ? DEFAULT_SORT_RESOLVER : sortResolver;
    }

    /*
     * (non-Javadoc)
     * @see HandlerMethodArgumentResolver#supportsParameter(MethodParameter)
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Pageable.class.equals(parameter.getParameterType());
    }

    /*
     * (non-Javadoc)
     * @see HandlerMethodArgumentResolver#resolveArgument(MethodParameter, BindingContext, ServerWebExchange)
     */
    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
        assertPageableUniqueness(parameter);
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();

        String pageString = queryParams.getFirst(getParameterNameToUse(pageParameterName, parameter));
        String pageSizeString = queryParams.getFirst(getParameterNameToUse(sizeParameterName, parameter));

        return sortResolver.resolveArgument(parameter, bindingContext, exchange)
                .cast(Sort.class)
                .map(sort -> resolvePageableParameter(parameter, pageString, pageSizeString, sort));
    }

}
