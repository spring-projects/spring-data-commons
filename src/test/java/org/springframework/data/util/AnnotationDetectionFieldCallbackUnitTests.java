/*
 * Copyright 2014-2017 the original author or authors.
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
package org.springframework.data.util;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AnnotationDetectionFieldCallback}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationDetectionFieldCallbackUnitTests {

	@Test(expected = IllegalArgumentException.class) // DATACMNS-616
	public void rejectsNullAnnotationType() {
		new AnnotationDetectionFieldCallback(null);
	}

	@Test // DATACMNS-616
	@SuppressWarnings("rawtypes")
	public void looksUpValueFromPrivateField() {

		AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(Autowired.class);
		ReflectionUtils.doWithFields(Sample.class, callback);

		assertThat(callback.getType(), is(equalTo((Class) String.class)));
		assertThat(callback.getValue(new Sample("foo")), is((Object) "foo"));
	}

	@Test // DATACMNS-616
	public void returnsNullForObjectNotContainingAFieldWithTheConfiguredAnnotation() {

		AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(Autowired.class);
		ReflectionUtils.doWithFields(Empty.class, callback);

		assertThat(callback.getType(), is(nullValue()));
		assertThat(callback.getValue(new Empty()), is(nullValue()));
	}

	static class Sample {

		@Autowired private final String value;

		public Sample(String value) {
			this.value = value;
		}
	}

	static class Empty {}
}
