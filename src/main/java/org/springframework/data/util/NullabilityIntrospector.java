/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.NonNull;
import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.MultiValueMap;

/**
 * Default {@link Nullability.Introspector} implementation backed by {@link NullabilityProvider nullability providers}.
 *
 * @author Mark Paluch
 */
class NullabilityIntrospector implements Nullability.Introspector {

	private static final List<NullabilityProvider> providers;

	static {
		providers = new ArrayList<>(4);

		if (Jsr305Provider.isAvailable()) {
			providers.add(new Jsr305Provider());
		}

		if (JakartaAnnotationProvider.isAvailable()) {
			providers.add(new JakartaAnnotationProvider());
		}

		if (JSpecifyAnnotationProvider.isAvailable()) {
			providers.add(new JSpecifyAnnotationProvider());
		}

		providers.add(new SpringProvider());
	}

	private final DeclarationAnchor anchor;

	NullabilityIntrospector(AnnotatedElement segment, boolean cache) {
		DeclarationAnchor tree = createTree(segment);
		this.anchor = cache ? new CachingDeclarationAnchor(tree) : tree;
	}

	/**
	 * Create a tree of declaration anchors.
	 *
	 * @param element the element to create the tree for.
	 * @return DeclarationAnchor encapsulating the source {@code element}.
	 */
	static DeclarationAnchor createTree(AnnotatedElement element) {

		if (element instanceof Package || element instanceof Module) {
			return new AnnotatedElementAnchor(element);
		}

		if (element instanceof Class<?> cls) {

			Class<?> enclosingClass = cls.getEnclosingClass();

			if (enclosingClass == null) {

				if (cls.getPackage() == null) {
					return new HierarchicalAnnotatedElementAnchor(createTree(cls.getModule()), element);
				}

				return new HierarchicalAnnotatedElementAnchor(
						new HierarchicalAnnotatedElementAnchor(createTree(cls.getModule()), cls.getPackage()), element);
			}

			return new HierarchicalAnnotatedElementAnchor(createTree(enclosingClass), element);
		}

		if (element instanceof Method m) {
			return new HierarchicalAnnotatedElementAnchor(createTree(m.getDeclaringClass()), element);
		}

		throw new IllegalArgumentException(String.format("Cannot create DeclarationAnchor for %s", element));
	}

	@Override
	public boolean isDeclared(ElementType elementType) {
		return anchor.evaluate(elementType) != Spec.UNSPECIFIED;
	}

	@Override
	public Nullability.MethodNullability forMethod(Method method) {

		HierarchicalAnnotatedElementAnchor element = new HierarchicalAnnotatedElementAnchor(anchor, method);
		Map<Parameter, Nullability> parameters = new HashMap<>();

		for (Parameter parameter : method.getParameters()) {
			parameters.put(parameter, Nullability.forParameter(parameter));
		}

		return new DefaultMethodNullability(element.evaluate(ElementType.METHOD), parameters, method);
	}

	@Override
	public Nullability forReturnType(Method method) {

		HierarchicalAnnotatedElementAnchor element = new HierarchicalAnnotatedElementAnchor(anchor, method);
		return new TheNullability(element.evaluate(ElementType.METHOD));
	}

	@Override
	public Nullability forParameter(Parameter parameter) {

		HierarchicalAnnotatedElementAnchor element = new HierarchicalAnnotatedElementAnchor(anchor, parameter);
		return new TheNullability(element.evaluate(ElementType.PARAMETER));
	}

	static Spec doWith(Function<NullabilityProvider, Spec> function) {

		for (NullabilityProvider provider : providers) {
			Spec result = function.apply(provider);

			if (result != Spec.UNSPECIFIED) {
				return result;
			}
		}

		return Spec.UNSPECIFIED;
	}

	@SuppressWarnings("unchecked")
	static <T> boolean test(Annotation annotation, String metaAnnotationName, String attribute, Predicate<T> filter) {

		if (annotation.annotationType().getName().equals(metaAnnotationName)) {

			Map<String, Object> attributes = AnnotationUtils.getAnnotationAttributes(annotation);

			return !attributes.isEmpty() && filter.test((T) attributes.get(attribute));
		}

		MultiValueMap<String, Object> attributes = AnnotatedElementUtils
				.getAllAnnotationAttributes(annotation.annotationType(), metaAnnotationName);

		if (attributes == null || attributes.isEmpty()) {
			return false;
		}

		List<Object> elementTypes = attributes.get(attribute);

		for (Object value : elementTypes) {

			if (filter.test((T) value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Provider for nullability rules.
	 */
	static abstract class NullabilityProvider {

		/**
		 * Evaluate nullability rules for a given {@link ElementType} in the scope of an {@link AnnotatedElement element}
		 * (i.e. enclosing class or package).
		 *
		 * @param element the contextual element.
		 * @param elementType element type to inspect.
		 * @return Specification result. Can be {@link Spec#UNSPECIFIED}.
		 */
		abstract Spec evaluate(AnnotatedElement element, ElementType elementType);
	}

	/**
	 * Spring provider leveraging {@link NonNullApi @NonNullApi}, {@link NonNullFields @NonNullFields},
	 * {@link NonNull @NonNull}, and {@link Nullable @Nullable} annotations.
	 */
	static class SpringProvider extends NullabilityProvider {

		@Override
		Spec evaluate(AnnotatedElement element, ElementType elementType) {

			if (element instanceof Package) {

				if (elementType == ElementType.METHOD || elementType == ElementType.PARAMETER) {
					return element.isAnnotationPresent(NonNullApi.class) ? Spec.NON_NULL : Spec.UNSPECIFIED;
				}

				if (elementType == ElementType.FIELD) {
					return element.isAnnotationPresent(NonNullFields.class) ? Spec.NON_NULL : Spec.UNSPECIFIED;
				}
			}

			if (elementType == ElementType.METHOD || elementType == ElementType.PARAMETER
					|| elementType == ElementType.FIELD) {

				if (element.isAnnotationPresent(NonNull.class)) {
					return Spec.NON_NULL;
				}

				if (element.isAnnotationPresent(Nullable.class)) {
					return Spec.NULLABLE;
				}
			}

			return Spec.UNSPECIFIED;
		}
	}

	/**
	 * Provider based on the JSR-305 (dormant) spec. Elements can be either annotated with
	 * {@code @Nonnull}/{@code @Nullable} directly or through meta-annotations that are composed of
	 * {@code @Nonnull}/{@code @Nullable} and {@code @TypeQualifierDefault}.
	 */
	@SuppressWarnings("DataFlowIssue")
	static class Jsr305Provider extends NullabilityProvider {

		private static final Class<Annotation> NON_NULL = findClass("javax.annotation.Nonnull");
		private static final Class<Annotation> NULLABLE = findClass("javax.annotation.Nullable");
		private static final String TYPE_QUALIFIER_CLASS_NAME = "javax.annotation.meta.TypeQualifierDefault";
		private static final Set<String> WHEN_NULLABLE = new HashSet<>(Arrays.asList("UNKNOWN", "MAYBE", "NEVER"));
		private static final Set<String> WHEN_NON_NULLABLE = new HashSet<>(Collections.singletonList("ALWAYS"));

		public static boolean isAvailable() {
			return NON_NULL != null && NULLABLE != null;
		}

		@Override
		Spec evaluate(AnnotatedElement element, ElementType elementType) {

			if (element.isAnnotationPresent(NULLABLE) || MergedAnnotations.from(element).isPresent(NULLABLE)) {
				return Spec.NULLABLE;
			}

			Annotation[] annotations = element.getAnnotations();

			for (Annotation annotation : annotations) {

				if (isNonNull(NON_NULL, annotation, elementType)) {
					return Spec.NON_NULL;
				}

				if (isNullable(NON_NULL, annotation, elementType)) {
					return Spec.NULLABLE;
				}
			}

			return Spec.UNSPECIFIED;
		}

		static boolean isNonNull(Class<Annotation> annotationClass, Annotation annotation, ElementType elementType) {
			return test(annotationClass, annotation, elementType, Jsr305Provider::isNonNull);
		}

		static boolean isNullable(Class<Annotation> annotationClass, Annotation annotation, ElementType elementType) {
			return test(annotationClass, annotation, elementType, Jsr305Provider::isNullable);
		}

		private static boolean test(Class<Annotation> annotationClass, Annotation annotation, ElementType elementType,
				Predicate<Annotation> predicate) {

			if (annotation.annotationType().equals(annotationClass)) {
				return predicate.test(annotation);
			}

			MergedAnnotations annotations = MergedAnnotations.from(annotation.annotationType());
			if (annotations.isPresent(annotationClass) && isInScope(annotation, elementType)) {
				Annotation meta = annotations.get(annotationClass).synthesize();

				return predicate.test(meta);
			}

			return false;
		}

		private static boolean isInScope(Annotation annotation, ElementType elementType) {
			return NullabilityIntrospector.test(annotation, TYPE_QUALIFIER_CLASS_NAME, "value",
					(ElementType[] o) -> Arrays.binarySearch(o, elementType) >= 0);
		}

		/**
		 * Introspect {@link Annotation} for being either a meta-annotation composed of {@code Nonnull} or {code Nonnull}
		 * itself expressing non-nullability.
		 *
		 * @param annotation
		 * @return {@literal true} if the annotation expresses non-nullability.
		 */
		static boolean isNonNull(Annotation annotation) {
			return NullabilityIntrospector.test(annotation, NON_NULL.getName(), "when",
					o -> WHEN_NON_NULLABLE.contains(o.toString()));
		}

		/**
		 * Introspect {@link Annotation} for being either a meta-annotation composed of {@code Nonnull} or {@code Nonnull}
		 * itself expressing nullability.
		 *
		 * @param annotation the annotation to introspect.
		 * @return {@literal true} if the annotation expresses nullability.
		 */
		static boolean isNullable(Annotation annotation) {
			return NullabilityIntrospector.test(annotation, NON_NULL.getName(), "when",
					o -> WHEN_NULLABLE.contains(o.toString()));
		}
	}

	/**
	 * Simplified variant of {@link Jsr305Provider} without {@code when} and {@code @TypeQualifierDefault} support.
	 */
	@SuppressWarnings("DataFlowIssue")
	static class JakartaAnnotationProvider extends SimpleAnnotationNullabilityProvider {

		private static final Class<Annotation> NON_NULL = findClass("jakarta.annotation.Nonnull");
		private static final Class<Annotation> NULLABLE = findClass("jakarta.annotation.Nullable");

		JakartaAnnotationProvider() {
			super(NON_NULL, NULLABLE);
		}

		public static boolean isAvailable() {
			return NON_NULL != null && NULLABLE != null;
		}
	}

	/**
	 * Provider for JSpecify annotations.
	 */
	@SuppressWarnings("DataFlowIssue")
	static class JSpecifyAnnotationProvider extends NullabilityProvider {

		private static final Class<Annotation> NON_NULL = findClass("org.jspecify.annotations.NonNull");
		private static final Class<Annotation> NULLABLE = findClass("org.jspecify.annotations.Nullable");
		private static final Class<Annotation> NULL_MARKED = findClass("org.jspecify.annotations.NullMarked");
		private static final Class<Annotation> NULL_UNMARKED = findClass("org.jspecify.annotations.NullUnmarked");

		public static boolean isAvailable() {
			return NON_NULL != null && NULLABLE != null && NULL_MARKED != null && NULL_UNMARKED != null;
		}

		@Override
		Spec evaluate(AnnotatedElement element, ElementType elementType) {

			Annotation[] annotations = element.getAnnotations();

			if (element instanceof Parameter p) {

				Spec result = evaluate(p.getAnnotatedType(), elementType);

				if (result != Spec.UNSPECIFIED) {
					return result;
				}
			}

			if (element instanceof Executable e) {

				Spec result = evaluate(e.getAnnotatedReturnType(), elementType);

				if (result != Spec.UNSPECIFIED) {
					return result;
				}
			}

			return evaluate(annotations);
		}

		private static Spec evaluate(Annotation[] annotations) {
			for (Annotation annotation : annotations) {

				if (SimpleAnnotationNullabilityProvider.test(NULL_UNMARKED, annotation)) {
					return Spec.UNSPECIFIED;
				}

				if (SimpleAnnotationNullabilityProvider.test(NULL_MARKED, annotation)
						|| SimpleAnnotationNullabilityProvider.test(NON_NULL, annotation)) {
					return Spec.NON_NULL;
				}

				if (SimpleAnnotationNullabilityProvider.test(NULLABLE, annotation)) {
					return Spec.NULLABLE;
				}
			}

			return Spec.UNSPECIFIED;
		}
	}

	/**
	 * Annotation-based {@link NullabilityProvider} leveraging simple or meta-annotations.
	 */
	static class SimpleAnnotationNullabilityProvider extends NullabilityProvider {

		private final Class<Annotation> nonNull;
		private final Class<Annotation> nullable;

		SimpleAnnotationNullabilityProvider(Class<Annotation> nonNull, Class<Annotation> nullable) {
			this.nonNull = nonNull;
			this.nullable = nullable;
		}

		@Override
		Spec evaluate(AnnotatedElement element, ElementType elementType) {

			Annotation[] annotations = element.getAnnotations();

			for (Annotation annotation : annotations) {

				if (test(nonNull, annotation)) {
					return Spec.NON_NULL;
				}

				if (test(nullable, annotation)) {
					return Spec.NULLABLE;
				}
			}

			return Spec.UNSPECIFIED;
		}

		static boolean test(Class<Annotation> annotationClass, Annotation annotation) {

			if (annotation.annotationType().equals(annotationClass)) {
				return true;
			}

			MergedAnnotations annotations = MergedAnnotations.from(annotation.annotationType());
			return annotations.isPresent(annotationClass);
		}

	}

	@Nullable
	@SuppressWarnings("unchecked")
	static <T> Class<T> findClass(String className) {

		try {
			return (Class<T>) ClassUtils.forName(className, NullabilityIntrospector.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	static class TheNullability implements Nullability {

		private final Spec spec;

		TheNullability(Spec spec) {
			this.spec = spec;
		}

		@Override
		public boolean isDeclared() {
			return spec != Spec.UNSPECIFIED;
		}

		@Override
		public boolean isNullable() {
			return spec == Spec.NULLABLE || spec == Spec.UNSPECIFIED;
		}

		@Override
		public boolean isNonNull() {
			return spec == Spec.NON_NULL;
		}
	}

	static class DefaultMethodNullability extends TheNullability implements Nullability.MethodNullability {

		private final Map<Parameter, Nullability> parameters;
		private final Method method;

		public DefaultMethodNullability(Spec spec, Map<Parameter, Nullability> parameters, Method method) {
			super(spec);
			this.parameters = parameters;
			this.method = method;
		}

		@Override
		public Nullability forParameter(Parameter parameter) {

			Nullability nullability = parameters.get(parameter);

			if (nullability == null) {
				throw new IllegalArgumentException(String.format("Parameter %s is not defined by %s", parameter, method));
			}

			return nullability;
		}
	}

	/**
	 * Declaration result.
	 */
	enum Spec {
		/**
		 * No nullabilty rule declared.
		 */
		UNSPECIFIED,

		/**
		 * Declaration yields nullable.
		 */
		NULLABLE,

		/**
		 * Declaration yields non-nullable.
		 */
		NON_NULL
	}

	/**
	 * Declaration anchors represent elements that hold nullability declarations such as classes, packages, modules.
	 */
	interface DeclarationAnchor {

		/**
		 * Evaluate nullability declarations for the given {@link ElementType}.
		 *
		 * @param target target element type to evaluate declarations for.
		 * @return specification result.
		 */
		Spec evaluate(ElementType target);

	}

	/**
	 * Caching variant of {@link DeclarationAnchor}.
	 */
	static class CachingDeclarationAnchor implements DeclarationAnchor {

		private final ConcurrentLruCache<ElementType, Spec> cache;

		public CachingDeclarationAnchor(DeclarationAnchor delegate) {
			this.cache = new ConcurrentLruCache<>(ElementType.values().length, delegate::evaluate);
		}

		@Override
		public Spec evaluate(ElementType target) {
			return this.cache.get(target);
		}

	}

	static class AnnotatedElementAnchor implements DeclarationAnchor {

		private final AnnotatedElement element;

		AnnotatedElementAnchor(AnnotatedElement element) {
			this.element = element;
		}

		@Override
		public Spec evaluate(ElementType target) {
			return doWith(np -> np.evaluate(element, target));
		}

		@Override
		public String toString() {
			return "DeclarationAnchor[" + element + "]";
		}
	}

	static class HierarchicalAnnotatedElementAnchor extends AnnotatedElementAnchor {

		private final DeclarationAnchor parent;

		public HierarchicalAnnotatedElementAnchor(DeclarationAnchor parent, AnnotatedElement element) {
			super(element);
			this.parent = parent;
		}

		@Override
		public Spec evaluate(ElementType target) {

			Spec result = super.evaluate(target);
			if (result != Spec.UNSPECIFIED) {
				return result;
			}

			return parent.evaluate(target);
		}
	}

}
