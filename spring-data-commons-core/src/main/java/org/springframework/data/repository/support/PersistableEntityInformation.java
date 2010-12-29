/*
 * Copyright 2008-2010 the original author or authors.
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