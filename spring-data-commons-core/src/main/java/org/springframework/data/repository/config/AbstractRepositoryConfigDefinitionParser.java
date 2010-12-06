/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.data.repository.config;

import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.*;
import static org.springframework.data.repository.util.ClassUtils.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.repository.NoRepositoryBean;
import org.w3c.dom.Element;


/**
 * Base class to implement repository namespaces. These will typically consist
 * of a main XML element potentially having child elements. The parser will wrap
 * the XML element into a {@link GlobalRepositoryConfigInformation} object and
 * allow either manual configuration or automatic detection of repository
 * interfaces.
 * 
 * @author Oliver Gierke
 */
public abstract class AbstractRepositoryConfigDefinitionParser<S extends GlobalRepositoryConfigInformation<T>, T extends SingleRepositoryConfigInformation<S>>
        implements BeanDefinitionParser {

    private static final Logger LOG = LoggerFactory
            .getLogger(AbstractRepositoryConfigDefinitionParser.class);

    private static final Class<?> PET_POST_PROCESSOR =
            PersistenceExceptionTranslationPostProcessor.class;
    private static final String DAO_INTERFACE_POST_PROCESSOR =
            "org.springframework.data.repository.support.RepositoryInterfaceAwareBeanPostProcessor";


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.
     * w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
     */
    public BeanDefinition parse(Element element, ParserContext parser) {

        try {
            S configContext = getGlobalRepositoryConfigInformation(element);

            if (configContext.configureManually()) {
                doManualConfiguration(configContext, parser);
            } else {
                doAutoConfiguration(configContext, parser);
            }

            Object beanSource = parser.extractSource(element);
            registerBeansForRoot(parser.getRegistry(), beanSource);

        } catch (RuntimeException e) {
            handleError(e, element, parser.getReaderContext());
        }

        return null;
    }


    /**
     * Executes repository auto configuration by scanning the provided base
     * package for repository interfaces.
     * 
     * @param config
     * @param parser
     */
    private void doAutoConfiguration(S config, ParserContext parser) {

        LOG.debug("Triggering auto repository detection");

        ResourceLoader resourceLoader =
                parser.getReaderContext().getResourceLoader();

        // Detect available DAO interfaces
        Set<String> repositoryInterfaces =
                getRepositoryInterfacesForAutoConfig(config, resourceLoader,
                        parser.getReaderContext());

        for (String daoInterface : repositoryInterfaces) {
            registerGenericRepositoryFactoryBean(parser,
                    config.getAutoconfigRepositoryInformation(daoInterface));
        }
    }


    private Set<String> getRepositoryInterfacesForAutoConfig(S config,
            ResourceLoader loader, ReaderContext reader) {

        ClassPathScanningCandidateComponentProvider scanner =
                new RepositoryComponentProvider(
                        config.getRepositoryBaseInterface());
        scanner.setResourceLoader(loader);

        TypeFilterParser parser =
                new TypeFilterParser(loader.getClassLoader(), reader);
        parser.parseFilters(config.getSource(), scanner);

        Set<BeanDefinition> findCandidateComponents =
                scanner.findCandidateComponents(config.getBasePackage());

        Set<String> interfaceNames = new HashSet<String>();
        for (BeanDefinition definition : findCandidateComponents) {
            interfaceNames.add(definition.getBeanClassName());
        }

        return interfaceNames;
    }


    /**
     * Returns a {@link GlobalRepositoryConfigInformation} implementation for
     * the given element.
     * 
     * @param element
     * @return
     */
    protected abstract S getGlobalRepositoryConfigInformation(Element element);


    /**
     * Proceeds manual configuration by traversing the context's
     * {@link SingleRepositoryConfigInformation}s.
     * 
     * @param context
     * @param parser
     */
    private void doManualConfiguration(S context, ParserContext parser) {

        LOG.debug("Triggering manual repository detection");

        for (T daoContext : context.getSingleRepositoryConfigInformations()) {
            registerGenericRepositoryFactoryBean(parser, daoContext);
        }
    }


    private void handleError(Exception e, Element source, ReaderContext reader) {

        reader.error(e.getMessage(), reader.extractSource(source), e.getCause());
    }


    /**
     * Registers a generic repository factory bean for a bean with the given
     * name and the provided configuration context.
     * 
     * @param parser
     * @param name
     * @param context
     */
    private void registerGenericRepositoryFactoryBean(ParserContext parser,
            T context) {

        try {

            Object beanSource = parser.extractSource(context.getSource());

            BeanDefinitionBuilder builder =
                    BeanDefinitionBuilder.rootBeanDefinition(context
                            .getRepositoryFactoryBeanClassName());

            builder.addPropertyValue("repositoryInterface",
                    context.getInterfaceName());
            builder.addPropertyValue("queryLookupStrategyKey",
                    context.getQueryLookupStrategyKey());

            String customImplementationBeanName =
                    registerCustomImplementation(context, parser, beanSource);

            if (customImplementationBeanName != null) {
                builder.addPropertyReference("customImplementation",
                        customImplementationBeanName);
            }

            postProcessBeanDefinition(context, builder, beanSource);

            AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
            beanDefinition.setSource(beanSource);

            LOG.debug(
                    "Registering repository: %s - Interface: %s - Factory: %s, - Custom implementation: %s",
                    new Object[] { context.getBeanId(),
                            context.getInterfaceName(),
                            context.getRepositoryFactoryBeanClassName(),
                            customImplementationBeanName });

            BeanComponentDefinition definition =
                    new BeanComponentDefinition(beanDefinition,
                            context.getBeanId());
            parser.registerBeanComponent(definition);
        } catch (RuntimeException e) {
            handleError(e, context.getSource(), parser.getReaderContext());
        }
    }


    /**
     * Callback to post process a repository bean definition prior to actual
     * registration.
     * 
     * @param context
     * @param builder
     * @param beanSource
     */
    protected void postProcessBeanDefinition(T context,
            BeanDefinitionBuilder builder, Object beanSource) {

    }


    /**
     * Registers a possibly available custom repository implementation on the
     * repository bean. Tries to find an already registered bean to reference or
     * tries to detect a custom implementation itself.
     * 
     * @param config
     * @param parser
     * @param source
     * @return the bean name of the custom implementation or {@code null} if
     *         none available
     */
    private String registerCustomImplementation(T config, ParserContext parser,
            Object source) {

        String beanName = config.getImplementationBeanName();

        // Already a bean configured?
        if (parser.getRegistry().containsBeanDefinition(beanName)) {
            return beanName;
        }

        // Autodetect implementation
        if (config.autodetectCustomImplementation()) {

            AbstractBeanDefinition beanDefinition =
                    detectCustomImplementation(config, parser);

            if (null == beanDefinition) {
                return null;
            }

            LOG.debug("Registering custom repository implementation: %s %s",
                    config.getImplementationBeanName(),
                    beanDefinition.getBeanClassName());

            beanDefinition.setSource(source);
            parser.registerBeanComponent(new BeanComponentDefinition(
                    beanDefinition, beanName));

        } else {
            beanName = config.getCustomImplementationRef();
        }

        return beanName;
    }


    /**
     * Tries to detect a custom implementation for a repository bean by
     * classpath scanning.
     * 
     * @param config
     * @param parser
     * @return the {@code AbstractBeanDefinition} of the custom implementation
     *         or {@literal null} if none found
     */
    private AbstractBeanDefinition detectCustomImplementation(T config,
            ParserContext parser) {

        // Build pattern to lookup implementation class
        Pattern pattern =
                Pattern.compile(".*" + config.getImplementationClassName());

        // Build classpath scanner and lookup bean definition
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        provider.setResourceLoader(parser.getReaderContext()
                .getResourceLoader());
        provider.addIncludeFilter(new RegexPatternTypeFilter(pattern));
        Set<BeanDefinition> definitions =
                provider.findCandidateComponents(config.getBasePackage());

        return (0 == definitions.size() ? null
                : (AbstractBeanDefinition) definitions.iterator().next());
    }


    /**
     * Callback to register additional bean definitions for a
     * {@literal repositories} root node. This usually includes beans you have
     * to set up once independently of the number of repositories to be created.
     * Will be called before any repositories bean definitions have been
     * registered.
     * 
     * @param registry
     * @param source
     */
    protected void registerBeansForRoot(BeanDefinitionRegistry registry,
            Object source) {

        // Create PersistenceExceptionTranslationPostProcessor definition
        if (!hasBean(PET_POST_PROCESSOR, registry)) {

            AbstractBeanDefinition definition =
                    BeanDefinitionBuilder
                            .rootBeanDefinition(PET_POST_PROCESSOR)
                            .getBeanDefinition();

            registerWithSourceAndGeneratedBeanName(registry, definition, source);
        }

        AbstractBeanDefinition definition =
                BeanDefinitionBuilder.rootBeanDefinition(
                        DAO_INTERFACE_POST_PROCESSOR).getBeanDefinition();

        registerWithSourceAndGeneratedBeanName(registry, definition, source);
    }


    /**
     * Returns whether the given {@link BeanDefinitionRegistry} already contains
     * a bean of the given type assuming the bean name has been autogenerated.
     * 
     * @param type
     * @param registry
     * @return
     */
    protected static boolean hasBean(Class<?> type,
            BeanDefinitionRegistry registry) {

        String name =
                String.format("%s%s0", type.getName(),
                        GENERATED_BEAN_NAME_SEPARATOR);
        return registry.containsBeanDefinition(name);
    }


    /**
     * Sets the given source on the given {@link AbstractBeanDefinition} and
     * registers it inside the given {@link BeanDefinitionRegistry}.
     * 
     * @param registry
     * @param bean
     * @param source
     * @return
     */
    protected static String registerWithSourceAndGeneratedBeanName(
            BeanDefinitionRegistry registry, AbstractBeanDefinition bean,
            Object source) {

        bean.setSource(source);

        String beanName = generateBeanName(bean, registry);
        registry.registerBeanDefinition(beanName, bean);

        return beanName;
    }

    /**
     * Custom {@link ClassPathScanningCandidateComponentProvider} scanning for
     * interfaces extending the given base interface. Skips interfaces annotated
     * with {@link NoRepositoryBean}.
     * 
     * @author Oliver Gierke
     */
    static class RepositoryComponentProvider extends
            ClassPathScanningCandidateComponentProvider {

        /**
         * Creates a new {@link RepositoryComponentProvider}.
         * 
         * @param repositoryInterface the interface to scan for
         */
        public RepositoryComponentProvider(Class<?> repositoryInterface) {

            super(false);
            addIncludeFilter(new InterfaceTypeFilter(repositoryInterface));
            addExcludeFilter(new AnnotationTypeFilter(NoRepositoryBean.class));
        }


        /*
         * (non-Javadoc)
         * 
         * @seeorg.springframework.context.annotation.
         * ClassPathScanningCandidateComponentProvider
         * #isCandidateComponent(org.springframework
         * .beans.factory.annotation.AnnotatedBeanDefinition)
         */
        @Override
        protected boolean isCandidateComponent(
                AnnotatedBeanDefinition beanDefinition) {

            boolean isNonHadesInterfaces =
                    !isGenericRepositoryInterface(beanDefinition
                            .getBeanClassName());
            boolean isTopLevelType =
                    !beanDefinition.getMetadata().hasEnclosingClass();

            return isNonHadesInterfaces && isTopLevelType;
        }

        /**
         * {@link org.springframework.core.type.filter.TypeFilter} that only
         * matches interfaces. Thus setting this up makes only sense providing
         * an interface type as {@code targetType}.
         * 
         * @author Oliver Gierke
         */
        private static class InterfaceTypeFilter extends AssignableTypeFilter {

            /**
             * Creates a new {@link InterfaceTypeFilter}.
             * 
             * @param targetType
             */
            public InterfaceTypeFilter(Class<?> targetType) {

                super(targetType);
            }


            /*
             * (non-Javadoc)
             * 
             * @seeorg.springframework.core.type.filter.
             * AbstractTypeHierarchyTraversingFilter
             * #match(org.springframework.core.type.classreading.MetadataReader,
             * org.springframework.core.type.classreading.MetadataReaderFactory)
             */
            @Override
            public boolean match(MetadataReader metadataReader,
                    MetadataReaderFactory metadataReaderFactory)
                    throws IOException {

                return metadataReader.getClassMetadata().isInterface()
                        && super.match(metadataReader, metadataReaderFactory);
            }
        }
    }
}
