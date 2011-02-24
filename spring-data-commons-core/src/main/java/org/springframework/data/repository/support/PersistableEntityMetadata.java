/*
 * Copyright 2008-201 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.support;

import org.springframework.data.domain.Persistable;


/**
 * Implementation of {@link EntityMetadata} that assumes the entity handled
 * implements {@link Persistable} and uses {@link Persistable#isNew()} for the
 * {@link #isNew(Object)} check.
 * 
 * @author Oliver Gierke
 */
@SuppressWarnings("rawtypes")
public class PersistableEntityMetadata extends
        AbstractEntityMetadata<Persistable> {

    /**
     * Creates a new {@link PersistableEntityMetadata}.
     * 
     * @param domainClass
     */
    public PersistableEntityMetadata() {

        super(Persistable.class);
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.IsNewAware#isNew(java.lang
     * .Object)
     */
    @Override
    public boolean isNew(Persistable entity) {

        return entity.isNew();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.IdAware#getId(java.lang.Object
     * )
     */
    public Object getId(Persistable entity) {

        return entity.getId();
    }
}