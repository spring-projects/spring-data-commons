package org.springframework.data.repository.support;

/**
 * Interface to abstract the ways to retrieve the id of the given entity.
 * 
 * @author Oliver Gierke
 */
public interface IdAware {

    /**
     * Returns the id of the given entity.
     * 
     * @param entity
     * @return
     */
    Object getId(Object entity);
}