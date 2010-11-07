package org.springframework.data.repository.config;

import static org.springframework.util.ClassUtils.*;
import static org.springframework.util.StringUtils.*;

import org.springframework.util.Assert;


/**
 * A {@link SingleRepositoryConfigInformation} implementation that is not backed
 * by an XML element but by a scanned interface. As this is derived from the
 * parent, most of the lookup logic is delegated to the parent as well.
 * 
 * @author Oliver Gierke
 */
public class AutomaticRepositoryConfigInformation<S extends CommonRepositoryConfigInformation>
        extends ParentDelegatingRepositoryConfigInformation<S> {

    private final String interfaceName;


    /**
     * Creates a new {@link AutomaticRepositoryConfigInformation} for the given
     * interface name and {@link CommonRepositoryConfigInformation} parent.
     * 
     * @param interfaceName
     * @param parent
     */
    public AutomaticRepositoryConfigInformation(String interfaceName, S parent) {

        super(parent);
        Assert.notNull(interfaceName);
        this.interfaceName = interfaceName;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.SingleRepositoryConfigInformation
     * #getBeanId()
     */
    public String getBeanId() {

        return uncapitalize(getShortName(interfaceName));
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.config.SingleRepositoryConfigInformation
     * #getInterfaceName()
     */
    public String getInterfaceName() {

        return interfaceName;
    }
}
