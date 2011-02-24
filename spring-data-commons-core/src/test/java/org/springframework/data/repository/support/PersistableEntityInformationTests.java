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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.data.domain.Persistable;


/**
 * Unit test for {@link PersistableEntityMetadata}.
 * 
 * @author Oliver Gierke
 */
public class PersistableEntityInformationTests {

    @Test
    public void detectsPersistableCorrectly() throws Exception {

        PersistableEntityMetadata info = new PersistableEntityMetadata();

        assertNewAndNoId(info, new PersistableEntity(null));
        assertNotNewAndId(info, new PersistableEntity(1L), 1L);
    }


    @SuppressWarnings("rawtypes")
    private <S extends EntityMetadata<Persistable>> void assertNewAndNoId(
            S info, Persistable entity) {

        assertThat(info.isNew(entity), is(true));
        assertThat(info.getId(entity), is(nullValue()));
    }


    @SuppressWarnings("rawtypes")
    private <S extends EntityMetadata<Persistable>> void assertNotNewAndId(
            S info, Persistable entity, Object id) {

        assertThat(info.isNew(entity), is(false));
        assertThat(info.getId(entity), is(id));
    }

    static class PersistableEntity implements Persistable<Long> {

        private static final long serialVersionUID = -5898780128204716452L;

        private final Long id;


        public PersistableEntity(Long id) {

            this.id = id;
        }


        public Long getId() {

            return id;
        }


        public boolean isNew() {

            return id == null;
        }
    }
}
