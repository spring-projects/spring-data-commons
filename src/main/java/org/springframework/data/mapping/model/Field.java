/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.data.mapping.model;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Christoph Strobl
 * @since 2020/10
 */
public class Field<OWNER, TYPE> implements AnnotationAware {

	private @Nullable Class<OWNER> owner;
	private final String propertyName;

	private final TypeInformation<TYPE> typeInformation;
	private @Nullable TypeInformation<?> componentType;
	private @Nullable TypeInformation<?> keyType;

	private final MultiValueMap<Class<? extends Annotation>, Annotation> annotations;

	private @Nullable Function<OWNER, TYPE> getterFunction;
	private @Nullable BiFunction<OWNER, TYPE, OWNER> setterFunction;

	public Field(String propertyName, TypeInformation<TYPE> propertyTypeInformation) {

		this.propertyName = propertyName;
		this.typeInformation = propertyTypeInformation;
		this.annotations = new LinkedMultiValueMap<>();
	}

	public static <OWNER, TYPE> Field<OWNER, TYPE> simpleField(String propertyName, Class<TYPE> type) {

		if (SimpleConfiguredTypes.isKownSimpleConfiguredType(type)) {
			return new Field<>(propertyName, SimpleConfiguredTypes.get(type));
		}

		return new Field<>(propertyName, new ConfigurableTypeInformation(type));
	}

	public static <OWNER> Field<OWNER, String> stringField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.stringType());
	}

	public static <OWNER> Field<OWNER, Long> longField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.longType());
	}

	public static <OWNER> Field<OWNER, Integer> intField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.intType());
	}

	public static <OWNER> Field<OWNER, Double> doubleField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.doubleType());
	}

	public static <OWNER> Field<OWNER, Float> floatField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.floatType());
	}

	public static <OWNER> Field<OWNER, Date> dateField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.dateType());
	}

	public static <OWNER> Field<OWNER, Character> charField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.charType());
	}

	public static <OWNER> Field<OWNER, Short> shortField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.shortType());
	}

	public static <OWNER> Field<OWNER, Byte> byteField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.byteType());
	}

	public static <OWNER> Field<OWNER, Boolean> booleanField(String propertyName) {
		return new Field<>(propertyName, SimpleConfiguredTypes.booleanType());
	}

	public static <OWNER, TYPE> Field<OWNER, TYPE> type(String propertyName, TypeInformation<TYPE> type) {
		return new Field<>(propertyName, type);
	}

	public Field<OWNER, TYPE> annotation(Annotation annotation) {

		annotations.add(annotation.annotationType(), annotation);
		return this;
	}

	public Field<OWNER, TYPE> wither(BiFunction<OWNER, TYPE, OWNER> setterFunction) {

		this.setterFunction = setterFunction;
		return this;
	}

	public Field<OWNER, TYPE> setter(BiConsumer<OWNER, TYPE> setterFunction) {

		return wither((OWNER, TYPE) -> {

			setterFunction.accept(OWNER, TYPE);
			return OWNER;
		});
	}

	public Field<OWNER, TYPE> getter(Function<OWNER, TYPE> getterFunction) {

		this.getterFunction = getterFunction;
		return this;
	}

	public Field<OWNER, TYPE> valueType(TypeInformation<?> valueTypeInformation) {
		this.componentType = valueTypeInformation;
		return this;
	}

	public Field<OWNER, TYPE> owner(Class<OWNER> owner) {

		this.owner = owner;
		return this;
	}

	public Field<OWNER, TYPE> annotatedWithAtId() {

		this.annotation(AnnotationAware.idAnnotation());
		return this;
	}

	public Field<OWNER, TYPE> annotatedWithAtPersistent() {

		this.annotation(AnnotationAware.persistentAnnotation());
		return this;
	}

	public Field<OWNER, TYPE> annotatedWithAtTransient() {

		this.annotation(AnnotationAware.transientAnnotation());
		return this;
	}

	public TypeInformation<?> getValueType() {
		return componentType != null ? componentType : typeInformation;
	}

	public String getFieldName() {
		return propertyName;
	}

	public TypeInformation<TYPE> getTypeInformation() {
		return typeInformation;
	}

	public boolean hasSetter() {
		return setterFunction != null;
	}

	public boolean hasGetter() {
		return getterFunction != null;
	}

	public BiFunction<OWNER, TYPE, OWNER> getSetter() {
		return setterFunction;
	}

	@Nullable
	public Function<OWNER, TYPE> getGetter() {
		return getterFunction;
	}

	@Override
	public List<Annotation> getAnnotations() {
		List<Annotation> all = new ArrayList<>();
		annotations.values().forEach(all::addAll);
		return all;
	}

	@Override
	public boolean hasAnnotation(Class<?> annotationType) {
		return annotations.containsKey(annotationType);
	}

	@Override
	public <T extends Annotation> List<T> findAnnotation(Class<T> annotation) {
		return (List<T>) annotations.getOrDefault(annotation, Collections.emptyList());
	}
}
