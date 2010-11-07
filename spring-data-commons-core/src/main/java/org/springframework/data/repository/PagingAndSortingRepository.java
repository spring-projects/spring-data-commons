package org.springframework.data.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


/**
 * Extension of {@link Repository} to provide additional methods to retrieve
 * entities using the pagination and sorting abstraction.
 * 
 * @see Sort
 * @see Pageable
 * @see Page
 * @author Oliver Gierke
 */
public interface PagingAndSortingRepository<T, ID extends Serializable> extends
        Repository<T, ID> {

    /**
     * Returns all entities sorted by the given options.
     * 
     * @param sort
     * @return all entities sorted by the given options
     */
    List<T> findAll(Sort sort);


    /**
     * Returns a {@link Page} of entities meeting the paging restriction
     * provided in the {@code Pageable} object.
     * 
     * @param pageable
     * @return a page of entities
     */
    Page<T> findAll(Pageable pageable);
}
