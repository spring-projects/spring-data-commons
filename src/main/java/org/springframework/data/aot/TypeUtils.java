/*
 * Copyright 2019-2021 the original author or authors.
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
package org.springframework.data.aot;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationFilter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.annotation.RepeatableContainers;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 */
public class TypeUtils {

	/**
	 * Resolve ALL annotations present for a given type. Will inspect type, constructors, parameters, methods, fields,...
	 *
	 * @param type
	 * @return never {@literal null}.
	 */
	public static Set<MergedAnnotation<Annotation>> resolveUsedAnnotations(Class<?> type) {

		Set<MergedAnnotation<Annotation>> annotations = new LinkedHashSet<>();
		annotations.addAll(TypeUtils.resolveAnnotationsFor(type).collect(Collectors.toSet()));
		for (Constructor<?> ctor : type.getDeclaredConstructors()) {
			annotations.addAll(TypeUtils.resolveAnnotationsFor(ctor).collect(Collectors.toSet()));
			for (Parameter parameter : ctor.getParameters()) {
				annotations.addAll(TypeUtils.resolveAnnotationsFor(parameter).collect(Collectors.toSet()));
			}
		}
		for (Field field : type.getDeclaredFields()) {
			annotations.addAll(TypeUtils.resolveAnnotationsFor(field).collect(Collectors.toSet()));
		}
		try {
			for (Method method : type.getDeclaredMethods()) {
				annotations.addAll(TypeUtils.resolveAnnotationsFor(method).collect(Collectors.toSet()));
				for (Parameter parameter : method.getParameters()) {
					annotations.addAll(TypeUtils.resolveAnnotationsFor(parameter).collect(Collectors.toSet()));
				}
			}
		} catch (NoClassDefFoundError e) {
			// ignore an move on
		}
		return annotations;
	}

	public static Stream<MergedAnnotation<Annotation>> resolveAnnotationsFor(AnnotatedElement element) {
		return resolveAnnotationsFor(element, AnnotationFilter.PLAIN);
	}

	public static Stream<MergedAnnotation<Annotation>> resolveAnnotationsFor(AnnotatedElement element,
			AnnotationFilter filter) {
		return MergedAnnotations
				.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.standardRepeatables(), filter).stream();
	}

	public static Set<Class<?>> resolveAnnotationTypesFor(AnnotatedElement element, AnnotationFilter filter) {
		return MergedAnnotations
				.from(element, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.standardRepeatables(), filter).stream()
				.map(MergedAnnotation::getType).collect(Collectors.toSet());
	}

	public static Set<Class<?>> resolveAnnotationTypesFor(AnnotatedElement element) {
		return resolveAnnotationTypesFor(element, AnnotationFilter.PLAIN);
	}

	public static boolean isAnnotationFromOrMetaAnnotated(Class<? extends Annotation> annotation, String prefix) {
		if (annotation.getPackage().getName().startsWith(prefix)) {
			return true;
		}
		return TypeUtils.resolveAnnotationsFor(annotation)
				.anyMatch(it -> it.getType().getPackage().getName().startsWith(prefix));
	}

	public static boolean hasAnnotatedField(Class<?> type, String annotationName) {

		for (Field field : type.getDeclaredFields()) {
			MergedAnnotations fieldAnnotations = MergedAnnotations.from(field);
			boolean hasAnnotation = fieldAnnotations.get(annotationName).isPresent();
			if (hasAnnotation) {
				return true;
			}
		}
		return false;
	}

	public static Set<Field> getAnnotatedField(Class<?> type, String annotationName) {

		Set<Field> fields = new LinkedHashSet<>();
		for (Field field : type.getDeclaredFields()) {
			if (MergedAnnotations.from(field).get(annotationName).isPresent()) {
				fields.add(field);
			}
		}
		return fields;
	}

	public static Set<Class<?>> resolveTypesInSignature(Class<?> owner, Method method) {
		Set<Class<?>> signature = new LinkedHashSet<>();
		signature.addAll(resolveTypesInSignature(ResolvableType.forMethodReturnType(method, owner)));
		for (Parameter parameter : method.getParameters()) {
			signature
					.addAll(resolveTypesInSignature(ResolvableType.forMethodParameter(MethodParameter.forParameter(parameter))));
		}
		return signature;
	}

	public static Set<Class<?>> resolveTypesInSignature(Class<?> owner, Constructor<?> constructor) {
		Set<Class<?>> signature = new LinkedHashSet<>();
		for (int i = 0; i < constructor.getParameterCount(); i++) {
			signature.addAll(resolveTypesInSignature(ResolvableType.forConstructorParameter(constructor, i, owner)));
		}
		return signature;
	}

	public static Set<Class<?>> resolveTypesInSignature(Class<?> root) {
		Set<Class<?>> signature = new LinkedHashSet<>();
		resolveTypesInSignature(ResolvableType.forClass(root), signature);
		return signature;
	}

	public static Set<Class<?>> resolveTypesInSignature(ResolvableType root) {
		Set<Class<?>> signature = new LinkedHashSet<>();
		resolveTypesInSignature(root, signature);
		return signature;
	}

	private static void resolveTypesInSignature(ResolvableType current, Set<Class<?>> signatures) {

		if (ResolvableType.NONE.equals(current) || ObjectUtils.nullSafeEquals(Void.TYPE, current.getType())
				|| ObjectUtils.nullSafeEquals(Object.class, current.getType())) {
			return;
		}
		if (signatures.contains(current.toClass())) {
			return;
		}
		signatures.add(current.toClass());
		resolveTypesInSignature(current.getSuperType(), signatures);
		for (ResolvableType type : current.getGenerics()) {
			resolveTypesInSignature(type, signatures);
		}
		for (ResolvableType type : current.getInterfaces()) {
			resolveTypesInSignature(type, signatures);
		}
	}

	public static TypeOps type(Class<?> type) {
		return new TypeOpsImpl(type);
	}

	public interface TypeOps {

		Class<?> getType();

		default boolean isPartOf(String... packageNames) {
			return isPartOf(TypeUtils.PackageFilter.of(packageNames));
		}

		default boolean isPartOf(PackageFilter... packageFilters) {
			for (PackageFilter filter : packageFilters) {
				if (filter.matches(getType().getName())) {
					return true;
				}
			}
			return false;
		}

		default Set<Class<?>> signatureTypes() {
			return TypeUtils.resolveTypesInSignature(getType());
		}

		interface PackageFilter {

			default boolean matches(Class<?> type) {
				return matches(type.getName());
			}

			boolean matches(String typeName);

			static PackageFilter of(String... packages) {
				return TypeUtils.PackageFilter.of(packages);
			}
		}
	}

	private static class TypeOpsImpl implements TypeOps {

		private Class<?> type;

		TypeOpsImpl(Class<?> type) {
			this.type = type;
		}

		public Class<?> getType() {
			return type;
		}
	}

	private static class PackageFilter implements TypeOps.PackageFilter {

		Set<String> packageNames;

		PackageFilter(Set<String> packageNames) {
			this.packageNames = packageNames;
		}

		static PackageFilter of(String... packageNames) {
			Set<String> target = new LinkedHashSet<>();
			for (String pkgName : packageNames) {
				target.add(pkgName.endsWith(".") ? pkgName : (pkgName + '.'));
			}
			return new PackageFilter(target);
		}

		@Override
		public boolean matches(String typeName) {
			for (String pgkName : packageNames) {
				if (typeName.startsWith(pgkName)) {
					return true;
				}
			}
			return false;
		}
	}
}
