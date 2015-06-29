/*
 * Copyright 2013-2015 the original author or authors.
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
package org.springframework.data.repository.augment;

import org.springframework.beans.BeanWrapper;
import org.springframework.data.repository.SoftDelete;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;

/**
 * Base class to implement a {@link QueryAugmentor} to soft-delete entities.
 * 
 * @since 1.11
 * @author Oliver Gierke
 */
public abstract class AbstractSoftDeleteQueryAugmentor<Q extends QueryContext<?>, N extends QueryContext<?>, U extends UpdateContext<?>>
		extends AnnotationBasedQueryAugmentor<SoftDelete, Q, N, U> {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AnnotationBasedQueryAugmentor#prepareUpdate(org.springframework.data.repository.augment.UpdateContext, java.lang.annotation.Annotation)
	 */
	@Override
	protected U prepareUpdate(U context, SoftDelete annotation) {

		if (!context.getMode().in(UpdateContext.UpdateMode.DELETE)) {
			return context;
		}

		String property = annotation.value();
		Object entity = context.getEntity();

		if (entity == null) {
			return context;
		}

		BeanWrapper wrapper = createBeanWrapper(context);

		Object currentValue = wrapper.getPropertyValue(property);
		Object nextValue = annotation.flagMode().toDeletedValue(currentValue);

		if (nextValue == null) {
			return context;
		}

		wrapper.setPropertyValue(property, nextValue);
		updateDeletedState(entity, context);

		return null;
	}

	/**
	 * Creates a new {@link BeanWrapper} for the given {@link UpdateContext}. Defaults to a
	 * {@link DirectFieldAccessFallbackBeanWrapper}.
	 * 
	 * @param context will never be {@literal null}.
	 * @return
	 */
	protected BeanWrapper createBeanWrapper(U context) {
		return new DirectFieldAccessFallbackBeanWrapper(context.getEntity());
	}

	/**
	 * Update the entity using the API exposed in the given {@link UpdateContext}.
	 * 
	 * @param entity will never be {@literal null}.
	 * @param context will never be {@literal null}.
	 */
	public abstract void updateDeletedState(Object entity, U context);
}
