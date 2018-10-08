package org.springframework.data.webflux.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.webflux.PageableHandlerMethodArgumentResolver;
import org.springframework.data.webflux.SortHandlerMethodArgumentResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

import java.util.Optional;

/**
 * @author Eugene Utkin
 */
@Configuration
public class PaginationWebFluxConfiguration implements WebFluxConfigurer {

    private PageableHandlerMethodArgumentResolverCustomizer pageableResolverCustomizer;
    private SortHandlerMethodArgumentResolverCustomizer sortResolverCustomizer;

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(sortResolver());
        configurer.addCustomResolver(pageableResolver());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.webflux.config.SpringDataWebConfiguration#pageableResolver()
     */
    @Bean
    public PageableHandlerMethodArgumentResolver pageableResolver() {
        PageableHandlerMethodArgumentResolver pageableResolver = new PageableHandlerMethodArgumentResolver(sortResolver());
        customizePageableResolver(pageableResolver);
        return pageableResolver;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.webflux.config.SpringDataWebConfiguration#sortResolver()
     */
    @Bean
    public SortHandlerMethodArgumentResolver sortResolver() {

        SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver();
        customizeSortResolver(sortResolver);
        return sortResolver;
    }

    protected void customizePageableResolver(PageableHandlerMethodArgumentResolver pageableResolver) {
        getPageableResolverCustomizer().ifPresent(c -> c.customize(pageableResolver));
    }

    protected void customizeSortResolver(SortHandlerMethodArgumentResolver sortResolver) {
        getSortResolverCustomizer().ifPresent(c -> c.customize(sortResolver));
    }


    private Optional<PageableHandlerMethodArgumentResolverCustomizer> getPageableResolverCustomizer() {
        return Optional.ofNullable(pageableResolverCustomizer);
    }

    @Autowired(required = false)
    public void setPageableResolverCustomizer(PageableHandlerMethodArgumentResolverCustomizer pageableResolverCustomizer) {
        this.pageableResolverCustomizer = pageableResolverCustomizer;
    }

    private Optional<SortHandlerMethodArgumentResolverCustomizer> getSortResolverCustomizer() {
        return Optional.ofNullable(sortResolverCustomizer);
    }

    @Autowired(required = false)
    public void setSortResolverCustomizer(SortHandlerMethodArgumentResolverCustomizer sortResolverCustomizer) {
        this.sortResolverCustomizer = sortResolverCustomizer;
    }
}
