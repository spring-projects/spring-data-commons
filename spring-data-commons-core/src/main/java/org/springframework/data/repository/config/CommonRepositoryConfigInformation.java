package org.springframework.data.repository.config;

import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.transaction.PlatformTransactionManager;
import org.w3c.dom.Element;


/**
 * Interface for shared repository information.
 * 
 * @author Oliver Gierke
 */
public interface CommonRepositoryConfigInformation {

    /**
     * Returns the element the repository information is derived from.
     * 
     * @return
     */
    Element getSource();


    /**
     * Returns the base package.
     * 
     * @return
     */
    String getBasePackage();


    /**
     * Returns the suffix to use for implementation bean lookup or class
     * detection.
     * 
     * @return
     */
    String getRepositoryImplementationSuffix();


    /**
     * Returns the configured repository factory class.
     * 
     * @return
     */
    String getRepositoryFactoryBeanClassName();


    /**
     * Returns the bean name of the {@link PlatformTransactionManager} to be
     * used.
     * 
     * @return
     */
    String getTransactionManagerRef();


    /**
     * Returns the strategy finder methods should be resolved.
     * 
     * @return
     */
    Key getQueryLookupStrategyKey();

}