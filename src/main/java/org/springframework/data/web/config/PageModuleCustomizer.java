package org.springframework.data.web.config;

import org.springframework.data.web.SortHandlerMethodArgumentResolver;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link SpringDataJacksonConfiguration.PageModule} configuration.
 *
 * @author Lazar Radinović
 * @since 3.4.0
 */
public interface PageModuleCustomizer {
    /**
     * Customize the given {@link SpringDataJacksonConfiguration.PageModule}.
     *
     * @param pageModule the {@link SpringDataJacksonConfiguration.PageModule} to customize, will never be {@literal null}.
     */
    void customize(SpringDataJacksonConfiguration.PageModule pageModule);
}
