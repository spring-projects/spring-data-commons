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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.MethodCallback;


/**
 * {@link IsNewAware} and {@link IdAware} implementation that reflectively
 * checks a {@link Field} or {@link Method} annotated with the given
 * annotations. Subclasses usually simply have to provide the persistence
 * technology specific annotations.
 * 
 * @author Oliver Gierke
 */
public class ReflectiveEntityInformationSupport implements IsNewAware, IdAware {

    private Field field;
    private Method method;


    /**
     * Creates a new {@link ReflectiveEntityInformationSupport} by inspecting
     * the given class for a {@link Field} or {@link Method} for and {@link Id}
     * annotation.
     * 
     * @param domainClass not {@literal null}, must be annotated with
     *            {@link Entity} and carry an anootation defining the id
     *            property.
     */
    public ReflectiveEntityInformationSupport(Class<?> domainClass,
            final Class<? extends Annotation>... annotationsToScanFor) {

        Assert.notNull(domainClass);

        ReflectionUtils.doWithFields(domainClass, new FieldCallback() {

            public void doWith(Field field) {

                if (ReflectiveEntityInformationSupport.this.field != null) {
                    return;
                }

                if (hasAnnotation(field, annotationsToScanFor)) {
                    ReflectiveEntityInformationSupport.this.field = field;
                }
            }
        });

        if (field != null) {
            return;
        }

        ReflectionUtils.doWithMethods(domainClass, new MethodCallback() {

            public void doWith(Method method) {

                if (ReflectiveEntityInformationSupport.this.method != null) {
                    return;
                }

                if (hasAnnotation(method, annotationsToScanFor)) {
                    ReflectiveEntityInformationSupport.this.method = method;
                }
            }
        });

        Assert.isTrue(this.field != null || this.method != null,
                "No id method or field found!");
    }


    /**
     * Checks whether the given {@link AnnotatedElement} carries one of the
     * given {@link Annotation}s.
     * 
     * @param annotatedElement
     * @param annotations
     * @return
     */
    private boolean hasAnnotation(AnnotatedElement annotatedElement,
            Class<? extends Annotation>... annotations) {

        for (Class<? extends Annotation> annotation : annotations) {

            if (annotatedElement.getAnnotation(annotation) != null) {
                return true;
            }
        }

        return false;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositorySupport.IsNewAware
     * #isNew(java.lang.Object)
     */
    public boolean isNew(Object entity) {

        return getId(entity) == null;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.data.repository.support.RepositorySupport.IdAware
     * #getId(java.lang.Object)
     */
    public Object getId(Object entity) {

        if (field != null) {
            ReflectionUtils.makeAccessible(field);
            return ReflectionUtils.getField(field, entity);
        }

        ReflectionUtils.makeAccessible(method);
        return ReflectionUtils.invokeMethod(method, entity);
    }
}