/*
 * Copyright 2015-2025 the original author or authors.
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
package org.springframework.data.web;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyValues;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * Unit tests for {@link MapDataBinder}.
 *
 * @author Oliver Gierke
 */
class MapDataBinderUnitTests {

	@Test // DATACMNS-630
	void honorsFormattingAnnotationOnAccessor() {

		var reference = new Date();

		var values = new MutablePropertyValues();
		values.add("foo.date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(reference));

		Map<String, Object> nested = new HashMap<>();
		nested.put("date", reference);

		assertThat(bind(values)).containsEntry("foo", nested);
	}

	@Test // DATACMNS-630
	void bindsNestedCollectionElement() {

		var values = new MutablePropertyValues();
		values.add("foo.bar.fooBar[0]", "String");

		var result = bind(values);

		List<String> list = new ArrayList<>();
		list.add("String");

		assertThat(result).isEqualTo(singletonMap("foo", singletonMap("bar", singletonMap("fooBar", list))));
	}

	@Test // DATACMNS-630
	void bindsNestedPrimitive() {

		var values = new MutablePropertyValues();
		values.add("foo.firstname", "Dave");
		values.add("foo.lastname", "Matthews");

		var result = bind(values);

		Map<String, Object> dave = new HashMap<>();
		dave.put("firstname", "Dave");
		dave.put("lastname", "Matthews");

		assertThat(result).isEqualTo(singletonMap("foo", dave));
	}

	@Test // DATACMNS-630
	void skipsPropertyNotExposedByTheTypeHierarchy() {

		var values = new MutablePropertyValues();
		values.add("somethingWeird", "Value");

		assertThat(bind(values)).isEqualTo(Collections.emptyMap());
	}

	@Test // DATACMNS-1264
	void dropsMapExpressionsForCollectionReferences() {

		var accessor = new MapDataBinder(Bar.class, new DefaultFormattingConversionService())
				.getPropertyAccessor();

		assertThatExceptionOfType(NotWritablePropertyException.class) //
				.isThrownBy(() -> accessor.setPropertyValue("fooBar['foo']", null)) //
				.withCauseInstanceOf(SpelEvaluationException.class);
	}

	@Test // DATACMNS-1264
	void rejectsExpressionContainingTypeExpression() {

		var accessor = new MapDataBinder(Bar.class, new DefaultFormattingConversionService())
				.getPropertyAccessor();

		assertThatExceptionOfType(NotWritablePropertyException.class) //
				.isThrownBy(() -> accessor.setPropertyValue("fooBar[T(java.lang.String)]", null)) //
				.withCauseInstanceOf(SpelEvaluationException.class);
	}

	private static Map<String, Object> bind(PropertyValues values) {

		var binder = new MapDataBinder(Root.class, new DefaultFormattingConversionService());
		binder.bind(values);

		return binder.getTarget();
	}

	interface Root {

		Foo getFoo();

		Bar getBar();
	}

	interface Foo {

		Bar getBar();

		String getLastname();

		String getFirstname();

		@DateTimeFormat(iso = ISO.DATE_TIME)
		Date getDate();
	}

	interface Bar {
		Collection<String> getFooBar();
	}
}
