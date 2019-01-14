/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.data.mapping.callback;

import java.util.function.BiFunction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * Simple implementation to invoke {@link EntityCallback}s.
 *
 * @author Mark Paluch
 */
public class SimpleEntityCallbacks extends AbstractEntityCallbacks {

	@Nullable private ErrorHandler errorHandler;

	public SimpleEntityCallbacks() {
		super();
	}

	public SimpleEntityCallbacks(ApplicationContext beanFactory) {
		super(beanFactory);
	}

	/**
	 * Set the {@link ErrorHandler} to invoke in case an exception is thrown from a {@link EntityCallback}.
	 * <p>
	 * Default is none, with a callback {@link Exception} stopping the current dispatch and getting propagated to the
	 * publisher of the current event.
	 * <p>
	 * Consider setting an {@link ErrorHandler} implementation that catches and logs exceptions (a la
	 * {@link org.springframework.scheduling.support.TaskUtils#LOG_AND_SUPPRESS_ERROR_HANDLER}) or an implementation that
	 * logs exceptions while nevertheless propagating them (e.g.
	 * {@link org.springframework.scheduling.support.TaskUtils#LOG_AND_PROPAGATE_ERROR_HANDLER}).
	 */
	public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Return the current {@link ErrorHandler} for this callback dispatcher.
	 *
	 * @return the current {@link ErrorHandler}.
	 */
	@Nullable
	protected ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * Perform an entity callback.
	 *
	 * @param entity the entity, must not be {@literal null}.
	 * @param callbackType desired callback type.
	 * @param callbackInvoker invocation function for the callback to optionally pass additional parameters.
	 * @return the resulting entity after invoking all callbacks.
	 */
	@SuppressWarnings("unchecked")
	public <T, E extends EntityCallback<T>> T callback(T entity, Class<? extends E> callbackType,
			BiFunction<? extends E, T, Object> callbackInvoker) {

		Assert.notNull(entity, "Entity must not be null!");

		ResolvableType resolvedCallbackType = ResolvableType.forClass(callbackType);
		T entityToUse = entity;

		for (EntityCallback<?> callback : getEntityCallbacks(entity, resolvedCallbackType)) {
			entityToUse = (T) invokeCallback(callback, entityToUse, (BiFunction) callbackInvoker);
		}

		return entityToUse;
	}

	/**
	 * Invoke the given callback with the given entity.
	 */
	protected Object invokeCallback(EntityCallback<?> callback, Object entity,
			BiFunction<EntityCallback<?>, Object, Object> callbackInvokerFunction) {

		ErrorHandler errorHandler = getErrorHandler();

		if (errorHandler != null) {
			try {
				return doInvokeCallback(callback, entity, callbackInvokerFunction);
			} catch (Throwable err) {
				errorHandler.handleError(err);
				return entity;
			}
		}

		return doInvokeCallback(callback, entity, callbackInvokerFunction);

	}

	@SuppressWarnings({ "rawtypes" })
	private Object doInvokeCallback(EntityCallback<?> callback, Object entity,
			BiFunction<EntityCallback<?>, Object, Object> callbackInvokerFunction) {

		try {
			return callbackInvokerFunction.apply(callback, entity);
		} catch (ClassCastException ex) {
			String msg = ex.getMessage();
			if (msg == null || matchesClassCastMessage(msg, entity.getClass())) {
				// Possibly a lambda-defined listener which we could not resolve the generic event type for
				// -> let's suppress the exception and just log a debug message.
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Non-matching callback type for entity callback: " + callback, ex);
				}

				return entity;
			} else {
				throw ex;
			}
		}
	}

	private boolean matchesClassCastMessage(String classCastMessage, Class<?> eventClass) {

		// On Java 8, the message starts with the class name: "java.lang.String cannot be cast..."
		if (classCastMessage.startsWith(eventClass.getName())) {
			return true;
		}

		// On Java 11, the message starts with "class ..." a.k.a. Class.toString()
		if (classCastMessage.startsWith(eventClass.toString())) {
			return true;
		}

		// On Java 9, the message used to contain the module name: "java.base/java.lang.String cannot be cast..."
		int moduleSeparatorIndex = classCastMessage.indexOf('/');
		if (moduleSeparatorIndex != -1 && classCastMessage.startsWith(eventClass.getName(), moduleSeparatorIndex + 1)) {
			return true;
		}

		// Assuming an unrelated class cast failure...
		return false;
	}
}
