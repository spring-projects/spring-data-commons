package org.springframework.data.repository.config;

import static org.springframework.util.StringUtils.*;

import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.util.Assert;
import org.w3c.dom.Element;


/**
 * Base class for {@link SingleRepositoryConfigInformation} implementations. So
 * these implementations will capture information for XML elements manually
 * configuring a single repository bean.
 * 
 * @author Oliver Gierke
 */
public abstract class ParentDelegatingRepositoryConfigInformation<T extends CommonRepositoryConfigInformation>
        implements SingleRepositoryConfigInformation<T> {

    private final T parent;


    /**
     * Creates a new {@link ParentDelegatingRepositoryConfigInformation} with
     * the given {@link CommonRepositoryConfigInformation} as parent.
     * 
     * @param parent
     */
    public ParentDelegatingRepositoryConfigInformation(T parent) {

        Assert.notNull(parent);
        this.parent = parent;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.RepositoryInformation#
     * getParent()
     */
    protected T getParent() {

        return parent;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.CommonRepositoryInformation
     * #getBasePackage()
     */
    public String getBasePackage() {

        return parent.getBasePackage();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.RepositoryInformation#
     * getImplementationClassName()
     */
    public String getImplementationClassName() {

        return capitalize(getBeanId()) + getRepositoryImplementationSuffix();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.RepositoryInformation#
     * getImplementationBeanName()
     */
    public String getImplementationBeanName() {

        return getBeanId() + getRepositoryImplementationSuffix();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.RepositoryInformation#
     * autodetectCustomImplementation()
     */
    public boolean autodetectCustomImplementation() {

        return true;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.RepositoryInformation#
     * getCustomImplementationRef()
     */
    public String getCustomImplementationRef() {

        return getBeanId() + getRepositoryImplementationSuffix();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.CommonRepositoryInformation
     * #getSource()
     */
    public Element getSource() {

        return parent.getSource();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.CommonRepositoryConfigInformation
     * #getRepositoryImplementationSuffix()
     */
    public String getRepositoryImplementationSuffix() {

        return parent.getRepositoryImplementationSuffix();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.CommonRepositoryConfigInformation
     * #getRepositoryFactoryBeanClassName()
     */
    public String getRepositoryFactoryBeanClassName() {

        return parent.getRepositoryFactoryBeanClassName();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.RepositoryInformation#
     * getTransactionManagerRef()
     */
    public String getTransactionManagerRef() {

        return parent.getTransactionManagerRef();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.jpa.repository.config.RepositoryInformation#
     * getQueryLookupStrategyKey()
     */
    public Key getQueryLookupStrategyKey() {

        return parent.getQueryLookupStrategyKey();
    }
}
