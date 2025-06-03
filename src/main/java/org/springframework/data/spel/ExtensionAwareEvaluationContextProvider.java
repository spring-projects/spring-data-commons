/*
 * Copyright 2014-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.spel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.ExtensionIdAware;
import org.springframework.data.spel.spi.Function;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.Optionals;
import org.springframework.data.util.Predicates;
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
import org.springframework.util.ClassUtils;

/**
 * An {@link EvaluationContextProvider} that assembles an {@link EvaluationContext} from a list of
 * {@link EvaluationContextExtension} instances.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Mark Paluch
 * @since 2.1
 */
public class ExtensionAwareEvaluationContextProvider implements EvaluationContextProvider {

	private final Map<String, EvaluationContextExtensionInformation> extensionInformationCache = new ConcurrentHashMap<>();
	private final Lazy<? extends Collection<? extends ExtensionIdAware>> extensions;

	private @Nullable ListableBeanFactory beanFactory;

	ExtensionAwareEvaluationContextProvider() {
		this(Collections.emptyList());
	}

	/**
	 * Creates a new {@link ExtensionAwareEvaluationContextProvider} with extensions looked up lazily from the given
	 * {@link BeanFactory}.
	 *
	 * @param beanFactory the {@link ListableBeanFactory} to lookup extensions from.
	 */
	public ExtensionAwareEvaluationContextProvider(ListableBeanFactory beanFactory) {

		this(Lazy.of(() -> beanFactory.getBeansOfType(ExtensionIdAware.class, true, false).values()));

		this.beanFactory = beanFactory;
	}

	/**
	 * Creates a new {@link ExtensionAwareEvaluationContextProvider} for the given {@link EvaluationContextExtension}s.
	 *
	 * @param extensions must not be {@literal null}.
	 */
	public ExtensionAwareEvaluationContextProvider(Collection<? extends ExtensionIdAware> extensions) {
		this(Lazy.of(extensions));
	}

	public ExtensionAwareEvaluationContextProvider(Lazy<? extends Collection<? extends ExtensionIdAware>> extensions) {
		this.extensions = extensions;
	}

	@Override
	public StandardEvaluationContext getEvaluationContext(@Nullable Object rootObject) {
		return doGetEvaluationContext(rootObject, getExtensions(Predicates.isTrue()));
	}

	@Override
	public StandardEvaluationContext getEvaluationContext(@Nullable Object rootObject,
			ExpressionDependencies dependencies) {
		return doGetEvaluationContext(rootObject, getExtensions(it -> dependencies.stream().anyMatch(it::provides)));
	}

	StandardEvaluationContext doGetEvaluationContext(@Nullable Object rootObject,
			Collection<? extends EvaluationContextExtension> extensions) {
		StandardEvaluationContext context = new StandardEvaluationContext();

		if (beanFactory != null) {
			context.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}

		ExtensionAwarePropertyAccessor accessor = new ExtensionAwarePropertyAccessor(extensions);

		context.addPropertyAccessor(accessor);
		context.addPropertyAccessor(new ReflectivePropertyAccessor());
		context.addMethodResolver(accessor);

		if (rootObject != null) {
			context.setRootObject(rootObject);
		}

		return context;
	}

	Collection<? extends ExtensionIdAware> getExtensions() {
		return this.extensions.get();
	}

	private Collection<? extends EvaluationContextExtension> getExtensions(
			Predicate<EvaluationContextExtensionInformation> extensionFilter) {

		Collection<EvaluationContextExtension> extensionsToUse = new ArrayList<>();

		for (ExtensionIdAware candidate : getExtensions()) {

			if (candidate instanceof EvaluationContextExtension extension) {

				if (extensionFilter.test(getOrCreateInformation(extension))) {
					extensionsToUse.add(extension);
				}
			}
		}

		return extensionsToUse;
	}

	/**
	 * Looks up the {@link EvaluationContextExtensionInformation} for the given {@link EvaluationContextExtension} from
	 * the cache or creates a new one and caches that for later lookup.
	 *
	 * @param extension must not be {@literal null}.
	 * @return
	 */
	EvaluationContextExtensionInformation getOrCreateInformation(EvaluationContextExtension extension) {
		return getOrCreateInformation(extension.getClass());
	}

	/**
	 * Looks up the {@link EvaluationContextExtensionInformation} for the given {@link EvaluationContextExtension} from
	 * the cache or creates a new one and caches that for later lookup.
	 *
	 * @param extension must not be {@literal null}.
	 * @return
	 */
	EvaluationContextExtensionInformation getOrCreateInformation(Class<? extends EvaluationContextExtension> extension) {
		return extensionInformationCache.computeIfAbsent(ClassUtils.getUserClass(extension).getName(),
				type -> new EvaluationContextExtensionInformation(extension));
	}

	/**
	 * Creates {@link EvaluationContextExtensionAdapter}s for the given {@link EvaluationContextExtension}s.
	 *
	 * @param extensions
	 * @return
	 */
	private List<EvaluationContextExtensionAdapter> toAdapters(
			Collection<? extends EvaluationContextExtension> extensions) {

		return extensions.stream()//
				.sorted(AnnotationAwareOrderComparator.INSTANCE)//
				.map(it -> new EvaluationContextExtensionAdapter(it, getOrCreateInformation(it))) //
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @see 1.9
	 */
	class ExtensionAwarePropertyAccessor implements PropertyAccessor, MethodResolver {

		private final List<EvaluationContextExtensionAdapter> adapters;
		private final Map<String, EvaluationContextExtensionAdapter> adapterMap;

		/**
		 * Creates a new {@link ExtensionAwarePropertyAccessor} for the given {@link EvaluationContextExtension}s.
		 *
		 * @param extensions must not be {@literal null}.
		 */
		public ExtensionAwarePropertyAccessor(Collection<? extends EvaluationContextExtension> extensions) {

			Assert.notNull(extensions, "Extensions must not be null");

			this.adapters = toAdapters(extensions);
			this.adapterMap = adapters.stream()//
					.collect(Collectors.toMap(EvaluationContextExtensionAdapter::getExtensionId, it -> it));

			Collections.reverse(this.adapters);
		}

		@Override
		public boolean canRead(EvaluationContext context, @Nullable Object target, String name) {

			if (target instanceof EvaluationContextExtension) {
				return true;
			}

			if (adapterMap.containsKey(name)) {
				return true;
			}

			return adapters.stream().anyMatch(it -> it.getProperties().containsKey(name));
		}

		@Override
		public TypedValue read(EvaluationContext context, @Nullable Object target, String name) {

			if (target instanceof EvaluationContextExtensionAdapter) {
				return lookupPropertyFrom((EvaluationContextExtensionAdapter) target, name);
			}

			if (adapterMap.containsKey(name)) {
				return new TypedValue(adapterMap.get(name));
			}

			return adapters.stream()//
					.filter(it -> it.getProperties().containsKey(name))//
					.map(it -> lookupPropertyFrom(it, name))//
					.findFirst().orElse(TypedValue.NULL);
		}

		@Nullable
		@Override
		public MethodExecutor resolve(EvaluationContext context, @Nullable Object target, final String name,
				List<TypeDescriptor> argumentTypes) {

			if (target instanceof EvaluationContextExtensionAdapter) {
				return getMethodExecutor((EvaluationContextExtensionAdapter) target, name, argumentTypes).orElse(null);
			}

			return adapters.stream()//
					.flatMap(it -> Optionals.toStream(getMethodExecutor(it, name, argumentTypes)))//
					.findFirst().orElse(null);
		}

		@Override
		public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) {
			// noop
		}

		@Override
		public Class<?> @Nullable [] getSpecificTargetClasses() {
			return null;
		}

		/**
		 * Returns a {@link MethodExecutor} wrapping a function from the adapter passed in as an argument.
		 *
		 * @param adapter the source of functions to consider.
		 * @param name the name of the function
		 * @param argumentTypes the types of the arguments that the function must accept.
		 * @return a matching {@link MethodExecutor}
		 */
		private Optional<MethodExecutor> getMethodExecutor(EvaluationContextExtensionAdapter adapter, String name,
				List<TypeDescriptor> argumentTypes) {
			return adapter.getFunctions().get(name, argumentTypes).map(FunctionMethodExecutor::new);
		}

		/**
		 * Looks up the property value for the property of the given name from the given extension. Takes care of resolving
		 * {@link Function} values transitively.
		 *
		 * @param extension must not be {@literal null}.
		 * @param name must not be {@literal null} or empty.
		 * @return a {@link TypedValue} matching the given parameters.
		 */
		private TypedValue lookupPropertyFrom(EvaluationContextExtensionAdapter extension, String name) {

			Object value = extension.getProperties().get(name);

			if (!(value instanceof Function function)) {
				return new TypedValue(value);
			}

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
	private record FunctionMethodExecutor(Function function) implements MethodExecutor {

		@Override
		public TypedValue execute(EvaluationContext context, Object target, @Nullable Object... arguments)
				throws AccessException {

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
		private final Functions functions = new Functions();
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

			Assert.notNull(extension, "Extension must not be null");
			Assert.notNull(information, "Extension information must not be null");

			Optional<Object> target = Optional.ofNullable(extension.getRootObject());
			EvaluationContextExtensionInformation.ExtensionTypeInformation extensionTypeInformation = information
					.getExtensionTypeInformation();
			EvaluationContextExtensionInformation.RootObjectInformation rootObjectInformation = information
					.getRootObjectInformation(target);

			functions.addAll(extension.getFunctions());
			functions.addAll(rootObjectInformation.getFunctions(target));
			functions.addAll(extensionTypeInformation.getFunctions());

			this.properties = new HashMap<>();
			this.properties.putAll(extensionTypeInformation.getProperties());
			this.properties.putAll(rootObjectInformation.getProperties(target));
			this.properties.putAll(extension.getProperties());

			this.extension = extension;
		}

		/**
		 * Returns the extension identifier.
		 *
		 * @return the id of the extension
		 */
		String getExtensionId() {
			return extension.getExtensionId();
		}

		/**
		 * Returns all functions exposed.
		 *
		 * @return all exposed functions.
		 */
		Functions getFunctions() {
			return this.functions;
		}

		/**
		 * Returns all properties exposed. Note, the value of a property can be a {@link Function} in turn
		 *
		 * @return a map from property name to property value.
		 */
		public Map<String, Object> getProperties() {
			return this.properties;
		}

		@Override
		public String toString() {
			return String.format("EvaluationContextExtensionAdapter for '%s'", getExtensionId());
		}

	}

}
