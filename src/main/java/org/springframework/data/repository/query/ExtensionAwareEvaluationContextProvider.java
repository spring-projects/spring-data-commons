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

import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.data.repository.query.EvaluationContextExtensionInformation.ExtensionTypeInformation;
import org.springframework.data.repository.query.EvaluationContextExtensionInformation.RootObjectInformation;
import org.springframework.data.repository.query.spi.EvaluationContextExtension;
import org.springframework.data.repository.query.spi.Function;
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

/**
 * An {@link EvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link EvaluationContextExtension} instances.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.9
 */
public class ExtensionAwareEvaluationContextProvider implements EvaluationContextProvider, ApplicationContextAware {

	private final Map<Class<?>, EvaluationContextExtensionInformation> extensionInformationCache = new HashMap<Class<?>, EvaluationContextExtensionInformation>();

	private List<? extends EvaluationContextExtension> extensions;
	private ListableBeanFactory beanFactory;

	/**
	 * Creates a new {@link ExtensionAwareEvaluationContextProvider}. Extensions are being looked up lazily from the
	 * {@link BeanFactory} configured.
	 */
	public ExtensionAwareEvaluationContextProvider() {
		this.extensions = null;
	}

	/**
	 * Creates a new {@link ExtensionAwareEvaluationContextProvider} for the given {@link EvaluationContextExtension}s.
	 * 
	 * @param adapters must not be {@literal null}.
	 */
	public ExtensionAwareEvaluationContextProvider(List<? extends EvaluationContextExtension> extensions) {

		Assert.notNull(extensions, "List of EvaluationContextExtensions must not be null!");
		this.extensions = extensions;
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
	public <T extends Parameters<?, ?>> StandardEvaluationContext getEvaluationContext(T parameters,
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
		ec.setVariables(collectVariables(parameters, parameterValues));

		return ec;
	}

	/**
	 * Exposes variables for all named parameters for the given arguments. Also exposes non-bindable parameters under the
	 * names of their types.
	 * 
	 * @param parameters must not be {@literal null}.
	 * @param arguments must not be {@literal null}.
	 * @return
	 */
	private <T extends Parameters<?, ?>> Map<String, Object> collectVariables(T parameters, Object[] arguments) {

		Map<String, Object> variables = new HashMap<String, Object>();

		for (Parameter param : parameters) {
			if (param.isSpecialParameter()) {

				String key = StringUtils.uncapitalize(param.getType().getSimpleName());
				Object value = arguments[param.getIndex()];

				variables.put(key, value);
			}
		}

		for (Parameter param : parameters) {
			if (param.isNamedParameter()) {
				variables.put(param.getName(), arguments[param.getIndex()]);
			}
		}

		return variables;
	}

	/**
	 * Returns the {@link EvaluationContextExtension} to be used. Either from the current configuration or the configured
	 * {@link BeanFactory}.
	 * 
	 * @return
	 */
	private List<? extends EvaluationContextExtension> getExtensions() {

		if (this.extensions != null) {
			return this.extensions;
		}

		if (beanFactory == null) {
			this.extensions = Collections.emptyList();
			return this.extensions;
		}

		this.extensions = new ArrayList<EvaluationContextExtension>(beanFactory.getBeansOfType(
				EvaluationContextExtension.class, true, false).values());

		return extensions;
	}

	/**
	 * Looks up the {@link EvaluationContextExtensionInformation} for the given {@link EvaluationContextExtension} from
	 * the cache or creates a new one and caches that for later lookup.
	 * 
	 * @param extension must not be {@literal null}.
	 * @return
	 */
	private EvaluationContextExtensionInformation getOrCreateInformation(EvaluationContextExtension extension) {

		Class<? extends EvaluationContextExtension> extensionType = extension.getClass();
		EvaluationContextExtensionInformation information = extensionInformationCache.get(extensionType);

		if (information != null) {
			return information;
		}

		information = new EvaluationContextExtensionInformation(extensionType);
		extensionInformationCache.put(extensionType, information);
		return information;
	}

	/**
	 * Creates {@link EvaluationContextExtensionAdapter}s for the given {@link EvaluationContextExtension}s.
	 * 
	 * @param extensions
	 * @return
	 */
	private List<EvaluationContextExtensionAdapter> toAdapters(Collection<? extends EvaluationContextExtension> extensions) {

		List<EvaluationContextExtension> extensionsToSet = new ArrayList<EvaluationContextExtension>(extensions);
		Collections.sort(extensionsToSet, AnnotationAwareOrderComparator.INSTANCE);

		List<EvaluationContextExtensionAdapter> adapters = new ArrayList<EvaluationContextExtensionAdapter>(
				extensions.size());

		for (EvaluationContextExtension extension : extensionsToSet) {
			adapters.add(new EvaluationContextExtensionAdapter(extension, getOrCreateInformation(extension)));
		}

		return adapters;
	}

	/**
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @see 1.9
	 */
	private class ExtensionAwarePropertyAccessor implements PropertyAccessor, MethodResolver {

		private final List<EvaluationContextExtensionAdapter> adapters;
		private final Map<String, EvaluationContextExtensionAdapter> adapterMap;

		/**
		 * Creates a new {@link ExtensionAwarePropertyAccessor} for the given {@link EvaluationContextExtension}s.
		 * 
		 * @param adapters must not be {@literal null}.
		 */
		public ExtensionAwarePropertyAccessor(List<? extends EvaluationContextExtension> extensions) {

			Assert.notNull(extensions, "Extensions must not be null!");

			this.adapters = toAdapters(extensions);
			this.adapterMap = new HashMap<String, EvaluationContextExtensionAdapter>(extensions.size());

			for (EvaluationContextExtensionAdapter adapter : adapters) {
				this.adapterMap.put(adapter.getExtensionId(), adapter);
			}

			Collections.reverse(this.adapters);
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

			if (adapterMap.containsKey(name)) {
				return true;
			}

			for (EvaluationContextExtensionAdapter extension : adapters) {
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

			if (target instanceof EvaluationContextExtensionAdapter) {
				return lookupPropertyFrom(((EvaluationContextExtensionAdapter) target), name);
			}

			if (adapterMap.containsKey(name)) {
				return new TypedValue(adapterMap.get(name));
			}

			for (EvaluationContextExtensionAdapter extension : adapters) {

				Map<String, Object> properties = extension.getProperties();

				if (properties.containsKey(name)) {
					return lookupPropertyFrom(extension, name);
				}
			}

			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.expression.MethodResolver#resolve(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.String, java.util.List)
		 */
		@Override
		public MethodExecutor resolve(EvaluationContext context, Object target, final String name,
				List<TypeDescriptor> argumentTypes) throws AccessException {

			if (target instanceof EvaluationContextExtensionAdapter) {
				return getMethodExecutor((EvaluationContextExtensionAdapter) target, name, argumentTypes);
			}

			for (EvaluationContextExtensionAdapter adapter : adapters) {

				MethodExecutor executor = getMethodExecutor(adapter, name, argumentTypes);

				if (executor != null) {
					return executor;
				}
			}

			return null;
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

		/**
		 * Returns a {@link MethodExecutor}
		 * 
		 * @param adapter
		 * @param name
		 * @param argumentTypes
		 * @return
		 */
		private MethodExecutor getMethodExecutor(EvaluationContextExtensionAdapter adapter, String name,
				List<TypeDescriptor> argumentTypes) {

			Map<String, Function> functions = adapter.getFunctions();

			if (!functions.containsKey(name)) {
				return null;
			}

			Function function = functions.get(name);

			if (!function.supports(argumentTypes)) {
				return null;
			}

			return new FunctionMethodExecutor(function);
		}

		/**
		 * Looks up the property value for the property of the given name from the given extension. Takes care of resolving
		 * {@link Function} values transitively.
		 * 
		 * @param extension must not be {@literal null}.
		 * @param name must not be {@literal null} or empty.
		 * @return
		 */
		private TypedValue lookupPropertyFrom(EvaluationContextExtensionAdapter extension, String name) {

			Object value = extension.getProperties().get(name);

			if (!(value instanceof Function)) {
				return new TypedValue(value);
			}

			Function function = (Function) value;

			try {
				return new TypedValue(function.invoke(new Object[0]));
			} catch (Exception e) {
				throw new SpelEvaluationException(e, SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, name,
						function.getDeclaringClass());
			}
		}
	}

	/**
	 * {@link MethodExecutor} to invoke {@link Function} instances.
	 *
	 * @author Oliver Gierke
	 * @since 1.9
	 */
	private static class FunctionMethodExecutor implements MethodExecutor {

		private final Function function;

		/**
		 * Creates a new {@link FunctionMethodExecutor} for the given {@link Function}.
		 * 
		 * @param function must not be {@literal null}.
		 */
		public FunctionMethodExecutor(Function function) {
			this.function = function;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.expression.MethodExecutor#execute(org.springframework.expression.EvaluationContext, java.lang.Object, java.lang.Object[])
		 */
		@Override
		public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {

			try {
				return new TypedValue(function.invoke(arguments));
			} catch (Exception e) {
				throw new SpelEvaluationException(e, SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, function.getName(),
						function.getDeclaringClass());
			}
		}
	}

	/**
	 * Adapter to expose a unified view on {@link EvaluationContextExtension} based on some reflective inspection of the
	 * extension (see {@link EvaluationContextExtensionInformation}) as well as the values exposed by the extension
	 * itself.
	 * 
	 * @author Oliver Gierke
	 * @since 1.9
	 */
	private static class EvaluationContextExtensionAdapter {

		private final EvaluationContextExtension extension;

		private final Map<String, Function> functions;
		private final Map<String, Object> properties;

		/**
		 * Creates a new {@link EvaluationContextExtensionAdapter} for the given {@link EvaluationContextExtension} and
		 * {@link EvaluationContextExtensionInformation}.
		 * 
		 * @param extension must not be {@literal null}.
		 * @param information must not be {@literal null}.
		 */
		public EvaluationContextExtensionAdapter(EvaluationContextExtension extension,
				EvaluationContextExtensionInformation information) {

			Assert.notNull(extension, "Extenstion must not be null!");
			Assert.notNull(information, "Extension information must not be null!");

			Object target = extension.getRootObject();
			ExtensionTypeInformation extensionTypeInformation = information.getExtensionTypeInformation();
			RootObjectInformation rootObjectInformation = information.getRootObjectInformation(target);

			this.functions = new HashMap<String, Function>();
			this.functions.putAll(extensionTypeInformation.getFunctions());
			this.functions.putAll(rootObjectInformation.getFunctions(target));
			this.functions.putAll(extension.getFunctions());

			this.properties = new HashMap<String, Object>();
			this.properties.putAll(extensionTypeInformation.getProperties());
			this.properties.putAll(rootObjectInformation.getProperties(target));
			this.properties.putAll(extension.getProperties());

			this.extension = extension;
		}

		/**
		 * Returns the extension identifier.
		 * 
		 * @return
		 */
		public String getExtensionId() {
			return extension.getExtensionId();
		}

		/**
		 * Returns all functions exposed.
		 * 
		 * @return
		 */
		public Map<String, Function> getFunctions() {
			return this.functions;
		}

		/**
		 * Returns all properties exposed. Note, the value of a property can be a {@link Function} in turn
		 * 
		 * @return
		 */
		public Map<String, Object> getProperties() {
			return this.properties;
		}
	}
}
