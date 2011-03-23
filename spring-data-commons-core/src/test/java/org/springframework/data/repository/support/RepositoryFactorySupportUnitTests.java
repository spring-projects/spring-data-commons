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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;


/**
 * Unit tests for {@link RepositoryFactorySupport}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class RepositoryFactorySupportUnitTests {

    RepositoryFactorySupport factory = new DummyRepositoryFactory();

    @Mock
    MyQueryCreationListener listener;
    @Mock
    PlainQueryCreationListener otherListener;


    @Test
    public void invokesCustomQueryCreationListenerForSpecialRepositoryQueryOnly()
            throws Exception {

        factory.addQueryCreationListener(listener);
        factory.addQueryCreationListener(otherListener);

        factory.getRepository(ObjectRepository.class);

        verify(listener, times(1)).onCreation(any(MyRepositoryQuery.class));
        verify(otherListener, times(2)).onCreation(any(RepositoryQuery.class));

    }

    class DummyRepositoryFactory extends RepositoryFactorySupport {
        
        /* (non-Javadoc)
         * @see org.springframework.data.repository.support.RepositoryFactorySupport#getEntityInformation(java.lang.Class)
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(
                Class<T> domainClass) {
        
            return mock(EntityInformation.class);
        }

        @Override
        protected Object getTargetRepository(RepositoryMetadata metadata) {

            return new Object();
        }


        @Override
        protected Class<?> getRepositoryBaseClass(Class<?> repositoryInterface) {

            return Object.class;
        }


        @Override
        protected QueryLookupStrategy getQueryLookupStrategy(Key key) {

            MyRepositoryQuery queryOne = mock(MyRepositoryQuery.class);
            RepositoryQuery queryTwo = mock(RepositoryQuery.class);

            QueryLookupStrategy strategy = mock(QueryLookupStrategy.class);
            when(strategy.resolveQuery(any(Method.class), any(Class.class)))
                    .thenReturn(queryOne, queryTwo);

            return strategy;
        }
    }

    interface ObjectRepository extends Repository<Object, Serializable> {

        Object findByClass(Class<?> clazz);


        Object findByFoo();
    }

    interface PlainQueryCreationListener extends
            QueryCreationListener<RepositoryQuery> {

    }

    interface MyQueryCreationListener extends
            QueryCreationListener<MyRepositoryQuery> {

    }

    interface MyRepositoryQuery extends RepositoryQuery {

    }
}
