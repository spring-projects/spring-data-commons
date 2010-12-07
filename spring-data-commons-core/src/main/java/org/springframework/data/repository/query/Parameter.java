/*
 * Copyright 2008-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.repository.query;

import static java.lang.String.*;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;


/**
 * Class to abstract a single parameter of a query method. It is held in the
 * context of a {@link Parameters} instance.
 * 
 * @author Oliver Gierke
 */
public final class Parameter {

    @SuppressWarnings("unchecked")
    static final List<Class<?>> TYPES = Arrays.asList(Pageable.class,
            Sort.class);

    private static final String PARAM_ON_SPECIAL = format(
            "You must not user @%s on a parameter typed %s or %s",
            Param.class.getSimpleName(), Pageable.class.getSimpleName(),
            Sort.class.getSimpleName());

    private static final String NAMED_PARAMETER_TEMPLATE = ":%s";
    private static final String POSITION_PARAMETER_TEMPLATE = "?%s";

    private final Class<?> type;
    private final Parameters parameters;
    private final int index;
    private final String name;


    /**
     * Creates a new {@link Parameter} for the given type, {@link Annotation}s,
     * positioned at the given index inside the given {@link Parameters}.
     * 
     * @param type
     * @param parameters
     * @param index
     * @param name
     */
    Parameter(Class<?> type, Parameters parameters, int index, String name) {

        Assert.notNull(type);
        Assert.notNull(parameters);

        this.parameters = parameters;
        this.index = index;

        this.type = type;
        this.name = name;

        if (isSpecialParameter() && isNamedParameter()) {
            throw new IllegalArgumentException(PARAM_ON_SPECIAL);
        }
    }


    /**
     * Returns whether the {@link Parameter} is the first one.
     * 
     * @return
     */
    boolean isFirst() {

        return index == 0;
    }


    /**
     * Returns the next {@link Parameter} from the surrounding
     * {@link Parameters}.
     * 
     * @throws ParameterOutOfBoundsException
     * @return
     */
    public Parameter getNext() {

        return parameters.getParameter(index + 1);
    }


    /**
     * Returns the previous {@link Parameter}.
     * 
     * @return
     */
    Parameter getPrevious() {

        return parameters.getParameter(index - 1);
    }


    /**
     * Returns whether the parameter is a special parameter.
     * 
     * @see #TYPES
     * @param index
     * @return
     */
    public boolean isSpecialParameter() {

        return TYPES.contains(type);
    }


    /**
     * Returns whether the {@link Parameter} is to be bound to a query.
     * 
     * @return
     */
    public boolean isBindable() {

        return !isSpecialParameter();
    }


    /**
     * Returns the placeholder to be used for the parameter. Can either be a
     * named one or positional.
     * 
     * @param index
     * @return
     */
    public String getPlaceholder() {

        if (isNamedParameter()) {
            return format(NAMED_PARAMETER_TEMPLATE, getName());
        } else {
            return format(POSITION_PARAMETER_TEMPLATE, getIndex());
        }
    }


    /**
     * Returns the position index the parameter is bound to in the context of
     * its surrounding {@link Parameters}.
     * 
     * @return
     */
    public int getIndex() {

        return index;
    }


    /**
     * Returns whether the parameter is annotated with {@link Param}.
     * 
     * @param index
     * @return
     */
    public boolean isNamedParameter() {

        return !isSpecialParameter() && getName() != null;
    }


    /**
     * Returns the name of the parameter (through {@link Param} annotation) or
     * null if none can be found.
     * 
     * @return
     */
    public String getName() {

        return name;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return format("%s:%s", isNamedParameter() ? getName() : "#" + index,
                type.getName());
    }


    /**
     * Returns whether the {@link Parameter} is a {@link Pageable} parameter.
     * 
     * @return
     */
    boolean isPageable() {

        return Pageable.class.isAssignableFrom(type);
    }


    /**
     * Returns whether the {@link Parameter} is a {@link Sort} parameter.
     * 
     * @return
     */
    boolean isSort() {

        return Sort.class.isAssignableFrom(type);
    }
}
