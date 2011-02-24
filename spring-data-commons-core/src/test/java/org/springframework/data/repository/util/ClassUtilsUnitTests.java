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
package org.springframework.data.repository.util;

import static org.junit.Assert.*;
import static org.springframework.data.repository.util.ClassUtils.*;

import java.io.Serializable;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;


/**
 * Unit test for {@link ClassUtils}.
 * 
 * @author Oliver Gierke
 */
public class ClassUtilsUnitTests {

    @Test(expected = IllegalStateException.class)
    public void rejectsInvalidReturnType() throws Exception {

        assertReturnType(SomeDao.class.getMethod("findByFirstname",
                Pageable.class, String.class), User.class);
    }


    @Test
    public void determinesValidFieldsCorrectly() {

        assertTrue(hasProperty(User.class, "firstname"));
        assertTrue(hasProperty(User.class, "Firstname"));
        assertFalse(hasProperty(User.class, "address"));
    }

    @SuppressWarnings("unused")
    private class User {

        private String firstname;


        public String getAddress() {

            return null;
        }
    }

    static interface UserRepository extends Repository<User, Integer> {

    }

    /**
     * Sample interface to serve two purposes:
     * <ol>
     * <li>Check that {@link ClassUtils#getDomainClass(Class)} skips non
     * {@link GenericDao} interfaces</li>
     * <li>Check that {@link ClassUtils#getDomainClass(Class)} traverses
     * interface hierarchy</li>
     * </ol>
     * 
     * @author Oliver Gierke
     */
    private interface SomeDao extends Serializable, UserRepository {

        Page<User> findByFirstname(Pageable pageable, String firstname);
    }
}
