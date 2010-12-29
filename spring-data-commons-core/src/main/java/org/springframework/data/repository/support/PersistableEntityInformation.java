package org.springframework.data.repository.support;

import org.springframework.data.domain.Persistable;


/**
 * Implementation of {@link IsNewAware} that assumes the entity handled
 * implements {@link Persistable} and uses {@link Persistable#isNew()} for the
 * {@link #isNew(Object)} check.
 * 
 * @author Oliver Gierke
 */
public class PersistableEntityInformation implements IsNewAware, IdAware {

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.IsNewAware#isNew(java.lang
     * .Object)
     */
    public boolean isNew(Object entity) {

        return ((Persistable<?>) entity).isNew();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.IdAware#getId(java.lang.Object
     * )
     */
    public Object getId(Object entity) {

        return ((Persistable<?>) entity).getId();
    }
}