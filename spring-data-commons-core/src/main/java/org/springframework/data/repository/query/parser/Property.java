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
package org.springframework.data.repository.query.parser;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;


/**
 * Abstraction of a {@link Property} of a domain class.
 * 
 * @author Oliver Gierke
 */
public class Property {

    private static String ERROR_TEMPLATE = "No property %s found for type %s";

    private final String name;
    private final Class<?> type;
    private final boolean isCollection;

    private Property next;


    /**
     * Creates a leaf property (no nested ones) with the given name inside the
     * given owning type.
     * 
     * @param name
     * @param owningType
     */
    Property(String name, Class<?> owningType) {

        Assert.hasText(name);
        Assert.notNull(owningType);

        Type genericType = getPropertyType(name, owningType);

        if (genericType == null) {
            throw new IllegalArgumentException(String.format(ERROR_TEMPLATE,
                    name, owningType));
        }

        this.type = getType(genericType);
        this.isCollection = isCollection(genericType);
        this.name = StringUtils.uncapitalize(name);
    }


    /**
     * Creates a {@link Property} with the given name inside the given owning
     * type and tries to resolve the other {@link String} to create nested
     * properties.
     * 
     * @param name
     * @param owningType
     * @param toTraverse
     */
    Property(String name, Class<?> owningType, String toTraverse) {

        this(name, owningType);

        if (StringUtils.hasText(toTraverse)) {
            this.next = from(toTraverse, type);
        }
    }


    /**
     * Returns the name of the {@link Property}.
     * 
     * @return the name
     */
    public String getName() {

        return name;
    }


    /**
     * Returns the next nested {@link Property}.
     * 
     * @see #hasNext()
     * @return the next nested {@link Property} or {@literal null} if no nested
     *         {@link Property} available.
     */
    public Property next() {

        return next;
    }


    /**
     * Returns whether there is a nested {@link Property}. If this returns
     * {@literal true} you can expect {@link #next()} to return a non-
     * {@literal null} value.
     * 
     * @return
     */
    public boolean hasNext() {

        return next != null;
    }


    /**
     * Returns the {@link Property} path in dot notation.
     * 
     * @return
     */
    public String toDotPath() {

        if (hasNext()) {
            return getName() + "." + next().toDotPath();
        }

        return getName();
    }


    /**
     * Returns whether the {@link Property} is actually a collection.
     * 
     * @return
     */
    public boolean isCollection() {

        return isCollection;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        Property that = (Property) obj;

        return this.name.equals(that.name) && this.type.equals(type);
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        return name.hashCode() + type.hashCode();
    }


    /**
     * Extracts the {@link Property} chain from the given source {@link String}
     * and type.
     * 
     * @param source
     * @param type
     * @return
     */
    public static Property from(String source, Class<?> type) {

        Iterator<String> parts = Arrays.asList(source.split("_")).iterator();

        Property result = null;
        Property current = null;

        while (parts.hasNext()) {
            if (result == null) {
                result = create(parts.next(), type);
                current = result;
            } else {
                current = create(parts.next(), current);
            }
        }

        return result;
    }


    /**
     * Creates a new {@link Property} as subordinary of the given
     * {@link Property}.
     * 
     * @param source
     * @param base
     * @return
     */
    private static Property create(String source, Property base) {

        Property property = create(source, base.type);
        base.next = property;
        return property;
    }


    /**
     * Factory method to create a new {@link Property} for the given
     * {@link String} and owning type. It will inspect the given source for
     * camel-case parts and traverse the {@link String} along its parts starting
     * with the entire one and chewing off parts from the right side then.
     * Whenever a valid property for the given class is found, the tail will be
     * traversed for subordinary properties of the just found one and so on.
     * 
     * @param source
     * @param type
     * @return
     */
    private static Property create(String source, Class<?> type) {

        return create(source, type, "");
    }


    /**
     * Tries to look up a chain of {@link Property}s by trying the givne source
     * first. If that fails it will split the source apart at camel case borders
     * (starting from the right side) and try to look up a {@link Property} from
     * the calculated head and recombined new tail and additional tail.
     * 
     * @param source
     * @param type
     * @param addTail
     * @return
     */
    private static Property create(String source, Class<?> type, String addTail) {

        IllegalArgumentException exception = null;

        try {
            return new Property(source, type);
        } catch (IllegalArgumentException e) {
            exception = e;
        }

        Pattern pattern = Pattern.compile("[A-Z]?[a-z]*$");
        Matcher matcher = pattern.matcher(source);

        if (matcher.find()) {

            int position = matcher.start();
            String head = source.substring(0, position);
            String tail = source.substring(position);

            try {
                return new Property(head, type, tail + addTail);
            } catch (IllegalArgumentException e) {
                if (position > 0) {
                    return create(head, type, tail);
                } else {
                    throw exception;
                }
            }
        }

        throw new IllegalArgumentException("Foo!");
    }


    /**
     * Looks up the {@link Property}s type for the given {@link Property} name
     * and owning type.
     * 
     * @param name
     * @param owningType
     * @return
     */
    private static Type getPropertyType(String name, Class<?> owningType) {

        Assert.notNull(name);
        Assert.notNull(owningType);

        Method method =
                ReflectionUtils.findMethod(owningType,
                        "get" + StringUtils.capitalize(name));

        if (method != null) {
            return method.getGenericReturnType();
        }

        Field field =
                ReflectionUtils.findField(owningType,
                        StringUtils.uncapitalize(name));

        return field == null ? null : field.getGenericType();
    }


    /**
     * Returns whether the given {@link Type} is a {@link Collection} type.
     * 
     * @param type
     * @return
     */
    private boolean isCollection(Type type) {

        if (type instanceof ParameterizedType) {
            return isCollection(((ParameterizedType) type).getRawType());
        } else if (type instanceof Class) {
            Class<?> result = (Class<?>) type;
            return result.isArray() ? true : Collection.class
                    .isAssignableFrom(result);
        }

        return false;
    }


    /**
     * Resolves the actual type of the field. Unpacks {@link Collection}s and
     * {@link Map}s. Will return the value type in case the given type is a
     * {@link Map}.
     * 
     * @param type
     * @return
     */
    private static Class<?> getType(Type type) {

        if (type instanceof ParameterizedType) {

            ParameterizedType actualType = (ParameterizedType) type;
            Class<?> rawType = getType(actualType.getRawType());

            if (Map.class.isAssignableFrom(rawType)) {
                return getType(actualType.getActualTypeArguments()[1]);
            } else if (Collection.class.isAssignableFrom(rawType)) {
                return getType(actualType.getActualTypeArguments()[0]);
            }
        } else if (type instanceof Class) {
            Class<?> result = (Class<?>) type;
            return result.isArray() ? result.getComponentType() : result;
        } else if (type instanceof GenericArrayType) {
            return getType(((GenericArrayType) type).getGenericComponentType());
        }

        throw new IllegalArgumentException();
    }
}
