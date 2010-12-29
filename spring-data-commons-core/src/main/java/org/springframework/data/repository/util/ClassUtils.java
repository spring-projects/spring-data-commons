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

import static org.springframework.core.GenericTypeResolver.*;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.data.repository.Repository;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;


/**
 * Utility class to work with classes.
 * 
 * @author Oliver Gierke
 */
public abstract class ClassUtils {

    @SuppressWarnings("rawtypes")
    private static final TypeVariable<Class<Repository>>[] PARAMETERS =
            Repository.class.getTypeParameters();
    private static final String DOMAIN_TYPE_NAME = PARAMETERS[0].getName();
    private static final String ID_TYPE_NAME = PARAMETERS[1].getName();


    /**
     * Private constructor to prevent instantiation.
     */
    private ClassUtils() {

    }


    /**
     * Returns the domain class the given class is declared for. Will introspect
     * the given class for extensions of {@link Repository} and retrieve the
     * domain class type from its generics declaration.
     * 
     * @param clazz
     * @return the domain class the given class is repository for or
     *         {@code null} if none found.
     */
    public static Class<?> getDomainClass(Class<?> clazz) {

        Class<?>[] arguments = resolveTypeArguments(clazz, Repository.class);
        return arguments == null ? null : arguments[0];
    }


    /**
     * Returns the id class the given class is declared for. Will introspect the
     * given class for extensions of {@link Repository} or and retrieve the
     * {@link Serializable} type from its generics declaration.
     * 
     * @param clazz
     * @return the id class the given class is repository for or {@code null} if
     *         none found.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Serializable> getIdClass(Class<?> clazz) {

        Class<?>[] arguments = resolveTypeArguments(clazz, Repository.class);
        return (Class<? extends Serializable>) (arguments == null ? null
                : arguments[1]);
    }


    /**
     * Returns the domain class returned by the given {@link Method}. Will
     * extract the type from {@link Collection}s and
     * {@link org.springframework.data.domain.Page} as well.
     * 
     * @param method
     * @return
     */
    public static Class<?> getReturnedDomainClass(Method method) {

        Type type = method.getGenericReturnType();

        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type)
                    .getActualTypeArguments()[0];

        } else {
            return method.getReturnType();
        }
    }


    /**
     * Returns whether the given class contains a property with the given name.
     * 
     * @param fieldName
     * @return
     */
    public static boolean hasProperty(Class<?> type, String property) {

        if (null != ReflectionUtils.findMethod(type, "get" + property)) {
            return true;
        }

        return null != ReflectionUtils.findField(type,
                StringUtils.uncapitalize(property));
    }


    /**
     * Returns wthere the given type is the {@link Repository} interface.
     * 
     * @param interfaze
     * @return
     */
    public static boolean isGenericRepositoryInterface(Class<?> interfaze) {

        return Repository.class.equals(interfaze);
    }


    /**
     * Returns whether the given type name is a repository interface name.
     * 
     * @param interfaceName
     * @return
     */
    public static boolean isGenericRepositoryInterface(String interfaceName) {

        return Repository.class.getName().equals(interfaceName);
    }


    /**
     * Returns the number of occurences of the given type in the given
     * {@link Method}s parameters.
     * 
     * @param method
     * @param type
     * @return
     */
    public static int getNumberOfOccurences(Method method, Class<?> type) {

        int result = 0;
        for (Class<?> clazz : method.getParameterTypes()) {
            if (type.equals(clazz)) {
                result++;
            }
        }

        return result;
    }


    /**
     * Asserts the given {@link Method}'s return type to be one of the given
     * types.
     * 
     * @param method
     * @param types
     */
    public static void assertReturnType(Method method, Class<?>... types) {

        if (!Arrays.asList(types).contains(method.getReturnType())) {
            throw new IllegalStateException(
                    "Method has to have one of the following return types! "
                            + Arrays.toString(types));
        }
    }


    /**
     * Returns whether the given object is of one of the given types. Will
     * return {@literal false} for {@literal null}.
     * 
     * @param object
     * @param types
     * @return
     */
    public static boolean isOfType(Object object, Collection<Class<?>> types) {

        if (null == object) {
            return false;
        }

        for (Class<?> type : types) {
            if (type.isAssignableFrom(object.getClass())) {
                return true;
            }
        }

        return false;
    }


    /**
     * Returns whether the given {@link Method} has a parameter of the given
     * type.
     * 
     * @param method
     * @param type
     * @return
     */
    public static boolean hasParameterOfType(Method method, Class<?> type) {

        return Arrays.asList(method.getParameterTypes()).contains(type);
    }


    /**
     * Helper method to extract the original exception that can possibly occur
     * during a reflection call.
     * 
     * @param ex
     * @throws Throwable
     */
    public static void unwrapReflectionException(Exception ex) throws Throwable {

        if (ex instanceof InvocationTargetException) {
            throw ((InvocationTargetException) ex).getTargetException();
        }

        throw ex;
    }


    /**
     * Returns the given base class' method if the given method (declared in the
     * interface) was also declared at the base class. Returns the given method
     * if the given base class does not declare the method given. Takes generics
     * into account.
     * 
     * @param method
     * @param baseClass
     * @param repositoryInterface
     * @return
     */
    public static Method getBaseClassMethodFor(Method method,
            Class<?> baseClass, Class<?> repositoryInterface) {

        for (Method baseClassMethod : baseClass.getMethods()) {

            // Wrong name
            if (!method.getName().equals(baseClassMethod.getName())) {
                continue;
            }

            // Wrong number of arguments
            if (!(method.getParameterTypes().length == baseClassMethod
                    .getParameterTypes().length)) {
                continue;
            }

            // Check whether all parameters match
            if (!parametersMatch(method, baseClassMethod, repositoryInterface)) {
                continue;
            }

            return baseClassMethod;
        }

        return method;
    }


    /**
     * Checks the given method's parameters to match the ones of the given base
     * class method. Matches generic arguments agains the ones bound in the
     * given repository interface.
     * 
     * @param method
     * @param baseClassMethod
     * @param repositoryInterface
     * @return
     */
    private static boolean parametersMatch(Method method,
            Method baseClassMethod, Class<?> repositoryInterface) {

        Type[] genericTypes = baseClassMethod.getGenericParameterTypes();
        Class<?>[] types = baseClassMethod.getParameterTypes();
        Class<?>[] methodParameters = method.getParameterTypes();

        for (int i = 0; i < genericTypes.length; i++) {

            Type type = genericTypes[i];

            if (type instanceof TypeVariable<?>) {

                String name = ((TypeVariable<?>) type).getName();

                if (!matchesGenericType(name, methodParameters[i],
                        repositoryInterface)) {
                    return false;
                }

            } else {

                if (!types[i].equals(methodParameters[i])) {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Checks whether the given parameter type matches the generic type of the
     * given parameter. Thus when {@literal PK} is declared, the method ensures
     * that given method parameter is the primary key type declared in the given
     * repository interface e.g.
     * 
     * @param name
     * @param parameterType
     * @param repositoryInterface
     * @return
     */
    private static boolean matchesGenericType(String name,
            Class<?> parameterType, Class<?> repositoryInterface) {

        Class<?> entityType = getDomainClass(repositoryInterface);
        Class<?> idClass = getIdClass(repositoryInterface);

        if (ID_TYPE_NAME.equals(name) && parameterType.equals(idClass)) {
            return true;
        }

        if (DOMAIN_TYPE_NAME.equals(name) && parameterType.equals(entityType)) {
            return true;
        }

        return false;
    }
}
