package org.springframework.data.repository.support;

/**
 * Interface to abstract the ways to determine if the given entity is to be
 * considered as new.
 * 
 * @author Oliver Gierke
 */
public interface IsNewAware {

    /**
     * Returns whether the given entity is considered to be new.
     * 
     * @param entity
     * @return
     */
    boolean isNew(Object entity);
}