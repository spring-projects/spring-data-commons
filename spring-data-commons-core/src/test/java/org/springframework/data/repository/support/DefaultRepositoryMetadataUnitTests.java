/*
 * Copyright 2011 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.util.ClassUtils;


/**
 * Unit tests for {@link DefaultRepositoryMetadata}.
 * 
 * @author Oliver Gierke
 */
public class DefaultRepositoryMetadataUnitTests {

    @SuppressWarnings("rawtypes")
    static final Class<DummyGenericRepositorySupport> REPOSITORY =
            DummyGenericRepositorySupport.class;


    @Test
    public void looksUpDomainClassCorrectly() throws Exception {

        RepositoryMetadata metadata =
                new DefaultRepositoryMetadata(UserRepository.class, REPOSITORY);
        assertEquals(User.class, metadata.getDomainClass());

        metadata = new DefaultRepositoryMetadata(SomeDao.class, REPOSITORY);
        assertEquals(User.class, metadata.getDomainClass());
    }


    @Test
    public void findsDomainClassOnExtensionOfDaoInterface() throws Exception {

        RepositoryMetadata metadata =
                new DefaultRepositoryMetadata(
                        ExtensionOfUserCustomExtendedDao.class, REPOSITORY);
        assertEquals(User.class, metadata.getDomainClass());
    }


    @Test
    public void detectsParameterizedEntitiesCorrectly() {

        RepositoryMetadata metadata =
                new DefaultRepositoryMetadata(GenericEntityRepository.class,
                        REPOSITORY);
        assertEquals(GenericEntity.class, metadata.getDomainClass());
    }


    @Test
    public void looksUpIdClassCorrectly() throws Exception {

        RepositoryMetadata metadata =
                new DefaultRepositoryMetadata(UserRepository.class, REPOSITORY);

        assertEquals(Integer.class, metadata.getIdClass());
    }


    @Test
    public void discoversRepositoryBaseClassMethod() throws Exception {

        Method method = FooDao.class.getMethod("findOne", Integer.class);
        DefaultRepositoryMetadata metadata =
                new DefaultRepositoryMetadata(FooDao.class, REPOSITORY);

        Method reference = metadata.getBaseClassMethodFor(method);
        assertEquals(REPOSITORY, reference.getDeclaringClass());
        assertThat(reference.getName(), is("findOne"));
    }


    @Test
    public void discoveresNonRepositoryBaseClassMethod() throws Exception {

        Method method = FooDao.class.getMethod("findOne", Long.class);

        DefaultRepositoryMetadata metadata =
                new DefaultRepositoryMetadata(FooDao.class, Repository.class);

        assertThat(metadata.getBaseClassMethodFor(method), is(method));
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

    private static interface FooDao extends Repository<User, Integer> {

        // Redeclared method
        User findOne(Integer primaryKey);


        // Not a redeclared method
        User findOne(Long primaryKey);
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

    static abstract class DummyGenericRepositorySupport<T, ID extends Serializable>
            implements Repository<T, ID> {

        public T findOne(ID id) {

            return null;
        }
    }

    /**
     * Helper class to reproduce #256.
     * 
     * @author Oliver Gierke
     */
    static class GenericEntity<T> {
    }

    static interface GenericEntityRepository extends
            Repository<GenericEntity<String>, Long> {

    }
}
