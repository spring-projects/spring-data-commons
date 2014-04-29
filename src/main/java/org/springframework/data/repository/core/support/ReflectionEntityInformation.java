/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.data.repository.core.support;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.util.ClassUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * {@link EntityInformation} implementation that inspects fields for an annotation and looks up this field's value to
 * retrieve the id.
 * 
 * @author Oliver Gierke
 * @author John Blum
 */
public class ReflectionEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> {

	private static final Class<Id> DEFAULT_ID_ANNOTATION = Id.class;

	private Field field;

  private Method method;

	/**
	 * Creates a new {@link ReflectionEntityInformation} inspecting the given domain class for a field carrying the
	 * {@link Id} annotation.
	 * 
	 * @param domainClass must not be {@literal null}.
	 */
	public ReflectionEntityInformation(Class<T> domainClass) {
		this(domainClass, DEFAULT_ID_ANNOTATION);
	}

	/**
	 * Creates a new {@link ReflectionEntityInformation} inspecting the given domain class for a field carrying the given
	 * annotation.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 */
	public ReflectionEntityInformation(Class<T> domainClass, final Class<? extends Annotation> annotation) {

		super(domainClass);
		Assert.notNull(annotation);

		ReflectionUtils.doWithFields(domainClass, new FieldCallback() {
			public void doWith(Field field) {
				if (field.getAnnotation(annotation) != null) {
					ReflectionEntityInformation.this.field = field;
				}
			}
		});

    ReflectionUtils.doWithMethods(domainClass, new MethodCallback() {
      public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
        if (method.getAnnotation(annotation) != null) {
          ReflectionEntityInformation.this.method = method;
        }
      }
    });

    // Assert that at least 1 and only 1 field or method is annotated with @Id...
    Assert.isTrue(this.field == null || this.method == null, String.format(
      "Either a field or method should be annotated with %1$s, but not both."
        + "The offending field (%2$s) and method (%3$s) in class (%4$s).",
          annotation, ClassUtils.nullSafeGetName(field), ClassUtils.nullSafeGetName(method), domainClass.getName()));

    Assert.isTrue(this.field != null || this.method != null, String.format(
      "No field or method annotated with %s was found!", annotation.toString()));

    if (field != null) {
      ReflectionUtils.makeAccessible(field);
    }
    else {
      ReflectionUtils.makeAccessible(method);
    }
  }

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getId(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public ID getId(Object entity) {
		return (entity == null ? null : (ID) (field != null ? ReflectionUtils.getField(field, entity)
      : ReflectionUtils.invokeMethod(method, entity)));
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.EntityInformation#getIdType()
	 */
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		return (Class<ID>) (field != null ? field.getType() : method.getReturnType());
	}
}
