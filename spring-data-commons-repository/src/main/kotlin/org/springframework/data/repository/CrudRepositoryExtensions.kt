/*
 * Copyright 2018-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository

/**
 * Retrieves an entity by its id.
 *
 * @param id the entity id.
 * @return the entity with the given id or `null` if none found.
 * @author Sebastien Deleuze
 * @author Oscar Hernandez
 * @since 2.1.4
 */
fun <T: Any, ID: Any> CrudRepository<T, ID>.findByIdOrNull(id: ID): T? = findById(id).orElse(null)
