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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;


/**
 * {@link SimpleParameterAccessor} is used to bind method parameters.
 * 
 * @author Oliver Gierke
 */
public class SimpleParameterAccessor {

    private final Parameters parameters;
    private final Object[] values;


    /**
     * Creates a new {@link SimpleParameterAccessor}.
     * 
     * @param parameters
     * @param values
     */
    public SimpleParameterAccessor(Parameters parameters, Object[] values) {

        Assert.notNull(parameters);
        Assert.notNull(values);

        Assert.isTrue(parameters.getNumberOfParameters() == values.length,
                "Invalid number of parameters given!");

        this.parameters = parameters;
        this.values = values;
    }


    /**
     * Returns the {@link Pageable} of the parameters, if available. Returns
     * {@code null} otherwise.
     * 
     * @return
     */
    public Pageable getPageable() {

        if (!parameters.hasPageableParameter()) {
            return null;
        }

        return (Pageable) values[parameters.getPageableIndex()];
    }


    /**
     * Returns the sort instance to be used for query creation. Will use a
     * {@link Sort} parameter if available or the {@link Sort} contained in a
     * {@link Pageable} if available. Returns {@code null} if no {@link Sort}
     * can be found.
     * 
     * @return
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


    private Object getBindableValue(int index) {

        return values[parameters.getBindableParameter(index).getIndex()];
    }


    /**
     * Returns a {@link BindableParameterIterator} to traverse all bindable
     * parameters.
     * 
     * @return
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
    public class BindableParameterIterator {

        private int currentIndex = 0;


        /**
         * Returns the next bindable parameter.
         * 
         * @return
         */
        public Object next() {

            return getBindableValue(currentIndex++);
        }
    }
}
