/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.data.repository;

import java.io.Serializable;
import java.util.List;


/**
 * Interface for generic CRUD operations on a repository for a specific type.
 * 
 * @author Oliver Gierke
 * @author Eberhard Wolff
 */
public interface Repository<T, ID extends Serializable> {

    /**
     * Saves a given entity. Use the returned instance for further operations as
     * the save operation might have changed the entity instance completely.
     * 
     * @param entity
     * @return the saved entity
     */
    T save(T entity);


    /**
     * Saves all given entities.
     * 
     * @param entities
     * @return
     */
    List<T> save(Iterable<? extends T> entities);


    /**
     * Retrives an entity by its primary key.
     * 
     * @param id
     * @return the entity with the given primary key or {@code null} if none
     *         found
     * @throws IllegalArgumentException if primaryKey is {@code null}
     */
    T findById(ID id);


    /**
     * Returns whether an entity with the given id exists.
     * 
     * @param id
     * @return true if an entity with the given id exists, alse otherwise
     * @throws IllegalArgumentException if primaryKey is {@code null}
     */
    boolean exists(ID id);


    /**
     * Returns all instances of the type.
     * 
     * @return all entities
     */
    List<T> findAll();


    /**
     * Returns the number of entities available.
     * 
     * @return the number of entities
     */
    Long count();


    /**
     * Deletes a given entity.
     * 
     * @param entity
     */
    void delete(T entity);


    /**
     * Deletes the given entities.
     * 
     * @param entities
     */
    void delete(Iterable<? extends T> entities);


    /**
     * Deletes all entities managed by the repository.
     */
    void deleteAll();
}
