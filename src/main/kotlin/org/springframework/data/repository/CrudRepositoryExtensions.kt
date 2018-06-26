package org.springframework.data.repository

/**
 * Retrieves an entity by its id.
 *
 * @param id the entity id.
 * @return the entity with the given id or `null` if none found
 * @author Sebastien Deleuze
 */
fun <T, ID> CrudRepository<T, ID>.findByIdOrNull(id: ID): T? = findById(id).orElse(null)
