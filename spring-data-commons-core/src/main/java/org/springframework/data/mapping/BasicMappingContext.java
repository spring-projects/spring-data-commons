/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.springframework.data.mapping.model.MappingContext;
import org.springframework.data.mapping.model.PersistentProperty;
import org.springframework.data.util.TypeInformation;

/**
 * Simple implementation of {@link MappingContext} creating {@link BasicPersistentEntity} and
 * {@link AnnotationBasedPersistentProperty} instances.
 * 
 * @author Jon Brisbin <jbrisbin@vmware.com>
 * @author Oliver Gierke
 */
public class BasicMappingContext extends AbstractMappingContext<BasicPersistentEntity<?>, PersistentProperty> {

    /* 
     * (non-Javadoc)
     * @see org.springframework.data.mapping.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
     */
    @Override
    @SuppressWarnings("rawtypes")
    protected BasicPersistentEntity<?> createPersistentEntity(
            TypeInformation typeInformation) {

        return new BasicPersistentEntity(typeInformation);
    }

    /* 
     * (non-Javadoc)
     * @see org.springframework.data.mapping.AbstractMappingContext#createPersistentProperty(java.lang.reflect.Field, java.beans.PropertyDescriptor, org.springframework.data.util.TypeInformation, org.springframework.data.mapping.BasicPersistentEntity)
     */
    @Override
    protected PersistentProperty createPersistentProperty(Field field,
            PropertyDescriptor descriptor, 
            BasicPersistentEntity<?> owner) {

        return new AnnotationBasedPersistentProperty(field, descriptor, owner);
    }
}
