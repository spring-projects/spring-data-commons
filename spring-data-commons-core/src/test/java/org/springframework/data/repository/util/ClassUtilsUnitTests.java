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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.repository.util.ClassUtils.*;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.support.RepositorySupport;


/**
 * Unit test for {@link ClassUtils}.
 * 
 * @author Oliver Gierke
 */
public class ClassUtilsUnitTests {

    @Test
    public void looksUpDomainClassCorrectly() throws Exception {

        assertEquals(User.class, getDomainClass(UserRepository.class));
        assertEquals(User.class, getDomainClass(SomeDao.class));
        assertNull(getDomainClass(Serializable.class));
    }


    @Test
    public void looksUpIdClassCorrectly() throws Exception {

        assertEquals(Integer.class, getIdClass(UserRepository.class));
        assertNull(getIdClass(Serializable.class));
    }


    @Test(expected = IllegalStateException.class)
    public void rejectsInvalidReturnType() throws Exception {

        assertReturnType(SomeDao.class.getMethod("findByFirstname",
                Pageable.class, String.class), User.class);
    }


    @Test
    public void findsDomainClassOnExtensionOfDaoInterface() throws Exception {

        assertEquals(User.class,
                getDomainClass(ExtensionOfUserCustomExtendedDao.class));
    }


    @Test
    public void determinesValidFieldsCorrectly() {

        assertTrue(hasProperty(User.class, "firstname"));
        assertTrue(hasProperty(User.class, "Firstname"));
        assertFalse(hasProperty(User.class, "address"));
    }


    /**
     * References #256.
     */
    @Test
    public void detectsParameterizedEntitiesCorrectly() {

        assertEquals(GenericEntity.class,
                getDomainClass(GenericEntityDao.class));
    }


    /**
     * #301
     */
    @Test
    public void discoversDaoBaseClassMethod() throws Exception {

        Method method = FooDao.class.getMethod("findById", Integer.class);

        Method reference =
                getBaseClassMethodFor(method,
                        DummyGenericRepositorySupport.class, FooDao.class);
        assertEquals(DummyGenericRepositorySupport.class,
                reference.getDeclaringClass());
        assertThat(reference.getName(), is("findById"));
    }


    /**
     * #301
     */
    @Test
    public void discoveresNonDaoBaseClassMethod() throws Exception {

        Method method = FooDao.class.getMethod("readById", Long.class);

        assertThat(
                getBaseClassMethodFor(method, RepositorySupport.class,
                        FooDao.class), is(method));
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

    /**
     * Sample interface to test recursive lookup of domain class.
     * 
     * @author Oliver Gierke
     */
    static interface ExtensionOfUserCustomExtendedDao extends
            UserCustomExtendedRepository {

    }

    static interface UserCustomExtendedRepository extends
            Repository<User, Integer> {

    }

    /**
     * Helper class to reproduce #256.
     * 
     * @author Oliver Gierke
     */
    static class GenericEntity<T> {
    }

    static interface GenericEntityDao extends
            Repository<GenericEntity<String>, Long> {

    }

    /**
     * Sample DAO interface to test redeclaration of {@link GenericDao} methods.
     * 
     * @author Oliver Gierke
     */
    private static interface FooDao extends Repository<User, Integer> {

        // Redeclared method
        User findById(Integer primaryKey);


        // Not a redeclared method
        User readById(Long primaryKey);
    }

    static abstract class DummyGenericRepositorySupport<T, ID extends Serializable>
            extends RepositorySupport<T, ID> {

        public DummyGenericRepositorySupport(Class<T> domainClass) {

            super(domainClass);
        }


        public T findById(ID id) {

            return null;
        }
    }
}
