/*
 * Copyright 2008-2010 the original author or authors.
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
package org.springframework.data.repository.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.util.TxUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Class defining access to the repository configuration abstracting the content
 * of the {@code repositories} element in XML namespcae configuration. Defines
 * default values to populate resulting repository beans with.
 * 
 * @author Oliver Gierke
 */
public abstract class RepositoryConfig<T extends SingleRepositoryConfigInformation<S>, S extends CommonRepositoryConfigInformation>
        implements GlobalRepositoryConfigInformation<T> {

    public static final String DEFAULT_REPOSITORY_IMPL_POSTFIX = "Impl";
    public static final String QUERY_LOOKUP_STRATEGY = "query-lookup-strategy";
    public static final String BASE_PACKAGE = "base-package";
    public static final String REPOSITORY_IMPL_POSTFIX =
            "repository-impl-postfix";
    public static final String REPOSITORY_FACTORY_CLASS_NAME = "factory-class";
    public static final String TRANSACTION_MANAGER_REF =
            "transaction-manager-ref";

    private final Element element;
    private final String defaultRepositoryFactoryBeanClassName;


    /**
     * Creates an instance of {@code RepositoryConfig}.
     * 
     * @param repositoriesElement
     */
    protected RepositoryConfig(Element repositoriesElement,
            String defaultRepositoryFactoryBeanClassName) {

        Assert.notNull(repositoriesElement, "Element must not be null!");
        Assert.notNull(defaultRepositoryFactoryBeanClassName,
                "Default repository factory bean class name must not be null!");

        this.element = repositoriesElement;
        this.defaultRepositoryFactoryBeanClassName =
                defaultRepositoryFactoryBeanClassName;

    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.CommonRepositoryConfigInformation
     * #getSource()
     */
    public Element getSource() {

        return element;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.GlobalRepositoryConfigInformation
     * #configureManually()
     */
    public boolean configureManually() {

        return getRepositoryElements().size() > 0;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.CommonRepositoryConfigInformation
     * #getQueryLookupStrategyKey()
     */
    public Key getQueryLookupStrategyKey() {

        String createFinderQueries =
                element.getAttribute(QUERY_LOOKUP_STRATEGY);

        return StringUtils.hasText(createFinderQueries) ? Key
                .create(createFinderQueries) : null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.CommonRepositoryConfigInformation
     * #getBasePackage()
     */
    public String getBasePackage() {

        return element.getAttribute(BASE_PACKAGE);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.CommonRepositoryConfigInformation
     * #getRepositoryFactoryClassName()
     */
    public String getRepositoryFactoryBeanClassName() {

        String factoryClassName =
                getSource().getAttribute(REPOSITORY_FACTORY_CLASS_NAME);
        return StringUtils.hasText(factoryClassName) ? factoryClassName
                : defaultRepositoryFactoryBeanClassName;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.CommonRepositoryConfigInformation
     * #getRepositoryImplementationSuffix()
     */
    public String getRepositoryImplementationSuffix() {

        String postfix = element.getAttribute(REPOSITORY_IMPL_POSTFIX);
        return StringUtils.hasText(postfix) ? postfix
                : DEFAULT_REPOSITORY_IMPL_POSTFIX;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.CommonRepositoryConfigInformation
     * #getTransactionManagerRef()
     */
    public String getTransactionManagerRef() {

        String ref = element.getAttribute(TRANSACTION_MANAGER_REF);
        return StringUtils.hasText(ref) ? ref
                : TxUtils.DEFAULT_TRANSACTION_MANAGER;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.GlobalRepositoryInformation
     * #getManualRepositoryInformations()
     */
    public Iterable<T> getSingleRepositoryConfigInformations() {

        Set<T> infos = new HashSet<T>();
        for (Element repositoryElement : getRepositoryElements()) {
            infos.add(createSingleRepositoryConfigInformationFor(repositoryElement));
        }

        return infos;
    }


    private Collection<Element> getRepositoryElements() {

        NodeList nodes = element.getChildNodes();
        Set<Element> result = new HashSet<Element>();

        for (int i = 0; i < nodes.getLength(); i++) {

            Node node = nodes.item(i);

            boolean isElement = Node.ELEMENT_NODE == node.getNodeType();
            boolean isRepository = "repository".equals(node.getLocalName());

            if (isElement && isRepository) {
                result.add((Element) node);
            }
        }

        return result;
    }


    /**
     * Creates a {@link SingleRepositoryConfigInformation} for the given
     * {@link Element}.
     * 
     * @param element
     * @return
     */
    protected abstract T createSingleRepositoryConfigInformationFor(
            Element element);
}
