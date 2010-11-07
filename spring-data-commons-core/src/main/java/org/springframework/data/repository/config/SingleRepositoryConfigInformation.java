package org.springframework.data.repository.config;

/**
 * Interface to capture configuration information necessary to set up a single
 * repository instance.
 * 
 * @author Oliver Gierke
 */
public interface SingleRepositoryConfigInformation<T extends CommonRepositoryConfigInformation>
        extends CommonRepositoryConfigInformation {

    /**
     * Returns the bean name to be used for the repository.
     * 
     * @return
     */
    String getBeanId();


    /**
     * Returns the name of the repository interface.
     * 
     * @return
     */
    String getInterfaceName();


    /**
     * Returns the class name of a possible custom repository implementation
     * class to detect.
     * 
     * @return
     */
    String getImplementationClassName();


    /**
     * Returns the bean name a possibly found custom implementation shall be
     * registered under.
     * 
     * @return
     */
    String getImplementationBeanName();


    /**
     * Returns the bean reference to the custom repository implementation.
     * 
     * @return
     */
    String getCustomImplementationRef();


    /**
     * Returns whether to try to autodetect a custom implementation.
     * 
     * @return
     */
    boolean autodetectCustomImplementation();
}