package org.springframework.data.repository.config;

/**
 * @author Oliver Gierke
 */
public interface GlobalRepositoryConfigInformation<T extends SingleRepositoryConfigInformation<?>>
        extends CommonRepositoryConfigInformation {

    /**
     * Returns the
     * 
     * @param interfaceName
     * @return
     */
    T getAutoconfigRepositoryInformation(String interfaceName);


    /**
     * Returns all {@link SingleRepositoryConfigInformation} instances used for
     * manual configuration.
     * 
     * @return
     */
    Iterable<T> getSingleRepositoryConfigInformations();


    /**
     * Returns whether to consider manual configuration. If this returns true,
     * clients should use {@link #getSingleRepositoryConfigInformations()} to
     * lookup configuration information for individual repository beans.
     * 
     * @return
     */
    boolean configureManually();


    /**
     * Returns the base interface to use
     * 
     * @return
     */
    Class<?> getRepositoryBaseInterface();
}