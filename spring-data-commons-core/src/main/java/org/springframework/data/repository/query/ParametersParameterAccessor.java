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
package org.springframework.data.repository.query;

import java.util.Iterator;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;


/**
 * {@link ParameterAccessor} implementation using a {@link Parameters} instance
 * to find special parameters.
 * 
 * @author Oliver Gierke
 */
public class ParametersParameterAccessor implements ParameterAccessor {

    private final Parameters parameters;
    private final Object[] values;


    /**
     * Creates a new {@link ParametersParameterAccessor}.
     * 
     * @param parameters
     * @param values
     */
    public ParametersParameterAccessor(Parameters parameters, Object[] values) {

        Assert.notNull(parameters);
        Assert.notNull(values);

        Assert.isTrue(parameters.getNumberOfParameters() == values.length,
                "Invalid number of parameters given!");

        this.parameters = parameters;
        this.values = values.clone();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.query.ParameterAccessor#getPageable()
     */
    public Pageable getPageable() {

        if (!parameters.hasPageableParameter()) {
            return null;
        }

        return (Pageable) values[parameters.getPageableIndex()];
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.query.ParameterAccessor#getSort()
     */
    public Sort getSort() {

        if (parameters.hasSortParameter()) {
            return (Sort) values[parameters.getSortIndex()];
        }

        if (parameters.hasPageableParameter() && getPageable() != null) {
            return getPageable().getSort();
        }

        return null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.query.ParameterAccessor#getBindableValue
     * (int)
     */
    public Object getBindableValue(int index) {

        return values[parameters.getBindableParameter(index).getIndex()];
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.query.ParameterAccessor#iterator()
     */
    public BindableParameterIterator iterator() {

        return new BindableParameterIterator();
    }

    /**
     * Iterator class to allow traversing all bindable parameters inside the
     * accessor.
     * 
     * @author Oliver Gierke
     */
    private class BindableParameterIterator implements Iterator<Object> {

        private int currentIndex = 0;


        /**
         * Returns the next bindable parameter.
         * 
         * @return
         */
        public Object next() {

            return getBindableValue(currentIndex++);
        }


        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {

            return values.length <= currentIndex;
        }


        /*
         * (non-Javadoc)
         * 
         * @see java.util.Iterator#remove()
         */
        public void remove() {

            throw new UnsupportedOperationException();
        }
    }
}
