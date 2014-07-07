/*
 * Copyright 2014 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.repository.query.spi.EvaluationContextExtension;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.TypeUtils;

/**
 * An {@link EvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link EvaluationContextExtension} instances.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.9
 */
public class ExtensionAwareEvaluationContextProvider implements EvaluationContextProvider, ApplicationContextAware {

	private List<EvaluationContextExtension> extensions;
	private ListableBeanFactory beanFactory;

	/**
	 * Creates a new {@link ExtensionAwareEvaluationContextProvider}. Extensions are being looked up lazily from the
	 * {@link BeanFactory} configured.
	 */
	public ExtensionAwareEvaluationContextProvider() {}

	/**
	 * Creates a new {@link ExtensionAwareEvaluationContextProvider} for the given {@link EvaluationContextExtension}s.
	 * 
	 * @param extensions must not be {@literal null}.
	 */
	public ExtensionAwareEvaluationContextProvider(List<? extends EvaluationContextExtension> extensions) {

		Assert.notNull(extensions, "List of EvaluationContextExtensions must not be null!");

		List<EvaluationContextExtension> extensionsToSet = new ArrayList<EvaluationContextExtension>(extensions);
		Collections.sort(extensionsToSet, AnnotationAwareOrderComparator.INSTANCE);

		this.extensions = Collections.unmodifiableList(extensionsToSet);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.EvaluationContextProvider#getEvaluationContext()
	 */
	@Override
	public <T extends Parameters<T, ? extends Parameter>> StandardEvaluationContext getEvaluationContext(T parameters,
			Object[] parameterValues) {

		StandardEvaluationContext ec = new StandardEvaluationContext();

		if (beanFactory != null) {
			ec.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}

		ExtensionAwarePropertyAccessor accessor = new ExtensionAwarePropertyAccessor(getExtensions());

		ec.addPropertyAccessor(accessor);
		ec.addPropertyAccessor(new ReflectivePropertyAccessor());
		ec.addMethodResolver(accessor);

		// Add parameters for indexed access
		ec.setRootObject(parameterValues);

		Map<String, Object> variables = collectVariables(parameters, parameterValues);

		ec.setVariables(variables);

		return ec;
	}

	protected <T extends Parameters<T, ? extends Parameter>> Map<String, Object> collectVariables(T parameters,
			Object[] parameterValues) {

		Map<String, Object> variables = new HashMap<String, Object>();

		registerSpecialParameterVariablesIfPresent(parameters, parameterValues, variables);
		registerNamedMethodParameterVariables(parameters, parameterValues, variables);

		return variables;
	}

	protected <T extends Parameters<T, ? extends Parameter>> void registerNamedMethodParameterVariables(T parameters,
			Object[] parameterValues, Map<String, Object> variables) {

		for (Parameter param : parameters) {
			if (param.isNamedParameter()) {
				variables.put(param.getName(), parameterValues[param.getIndex()]);
			}
		}
	}

	protected <T extends Parameters<T, ? extends Parameter>> void registerSpecialParameterVariablesIfPresent(
			T parameters, Object[] parameterValues, Map<String, Object> variables) {

		if (parameters.hasSpecialParameter()) {

			for (Parameter param : parameters) {
				if (param.isSpecialParameter()) {

					Object paramValue = parameterValues[param.getIndex()];
					if (paramValue == null) {
						continue;
					}

					variables.put(StringUtils.uncapitalize(param.getType().getSimpleName()), paramValue);
				}
			}
		}
	}

	/**
	 * Returns the {@link EvaluationContextExtension} to be used. Either from the current configuration or the configured
	 * {@link BeanFactory}.
	 * 
	 * @return
	 */
	private List<EvaluationContextExtension> getExtensions() {

		if (this.extensions != null) {
			return this.extensions;
		}

		if (beanFactory == null) {
			this.extensions = Collections.emptyList();
			return this.extensions;
		}

		List<EvaluationContextExtension> extensions = new ArrayList<EvaluationContextExtension>(beanFactory.getBeansOfType(
				EvaluationContextExtension.class, true, false).values());
		Collections.sort(extensions, AnnotationAwareOrderComparator.INSTANCE);
		this.extensions = extensions;

		return extensions;
	}

	/**
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @see 1.9
	 */
	private static class ExtensionAwarePropertyAccessor implements PropertyAccessor, MethodResolver {

		private final Map<String, EvaluationContextExtension> extensionMap;
		private final List<EvaluationContextExtension> extensions;
		private final Map<String, Object> functions;

		/**
		 * Creates a new {@link ExtensionAwarePropertyAccessor} for the given {@link EvaluationContextExtension}s.
		 * 
		 * @param extensions must not be {@literal null}.
		 */
		public ExtensionAwarePropertyAccessor(List<? extends EvaluationContextExtension> extensions) {

			Assert.notNull(extensions, "Extensions must not be null!");

			Map<String, Object> functions = new HashMap<String, Object>();

			for (EvaluationContextExtension ext : extensions) {

				Map<String, Method> extFunctions = ext.getFunctions();

				if (ext.getExtensionId() != null) {
					functions.put(ext.getExtensionId(), extFunctions);
				}

				functions.putAll(extFunctions);
			}

			this.functions = functions;

			this.extensions = new ArrayList<EvaluationContextExtension>(extensions);
			Collections.reverse(this.extensions);

			this.extensionMap = new HashMap<String, EvaluationContextExtension>(extensions.size());

			for (EvaluationContextExtension extension : extensions) {
				this.extensionMap.put(extension.getExtensionId(), extension);
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.query.ExtensionAwareEvaluationContextProvider.ReadOnlyPropertyAccessor#canRead(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.String)
		 */
		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {

			if (target instanceof EvaluationContextExtension) {
				return true;
			}

			if (extensionMap.containsKey(name)) {
				return true;
			}

			for (EvaluationContextExtension extension : extensions) {
				if (extension.getProperties().containsKey(name)) {
					return true;
				}
			}

			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.expression.PropertyAccessor#read(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.String)
		 */
		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {

			if (target instanceof EvaluationContextExtension) {
				return new TypedValue(((EvaluationContextExtension) target).getProperties().get(name));
			}

			if (extensionMap.containsKey(name)) {
				return new TypedValue(extensionMap.get(name));
			}

			for (EvaluationContextExtension extension : extensions) {

				Map<String, Object> properties = extension.getProperties();

				if (properties.containsKey(name)) {
					return new TypedValue(properties.get(name));
				}
			}

			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.expression.MethodResolver#resolve(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.String, java.util.List)
		 */
		@Override
		public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
				List<TypeDescriptor> argumentTypes) throws AccessException {

			final Method function = targetObject instanceof Map && ((Map<?, ?>) targetObject).containsKey(name) ? (Method) ((Map<?, ?>) targetObject)
					.get(name) : (Method) functions.get(name);

			if (function == null) {
				return null;
			}

			Class<?>[] parameterTypes = function.getParameterTypes();
			if (parameterTypes.length != argumentTypes.size()) {
				return null;
			}

			for (int i = 0; i < parameterTypes.length; i++) {
				if (!TypeUtils.isAssignable(parameterTypes[i], argumentTypes.get(i).getType())) {
					return null;
				}
			}

			return new MethodExecutor() {

				/*
				 * (non-Javadoc)
				 * @see org.springframework.expression.MethodExecutor#execute(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.Object[])
				 */
				@Override
				public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {

					try {
						return new TypedValue(function.invoke(null, arguments));
					} catch (Exception e) {
						throw new SpelEvaluationException(e, SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, function.getName(),
								function.getDeclaringClass());
					}
				}
			};
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.expression.PropertyAccessor#canWrite(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.String)
		 */
		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.expression.PropertyAccessor#write(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.String, java.lang.Object)
		 */
		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
			// noop
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.expression.PropertyAccessor#getSpecificTargetClasses()
		 */
		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}
	}
}
