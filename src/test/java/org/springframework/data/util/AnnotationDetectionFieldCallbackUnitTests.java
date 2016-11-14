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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import lombok.Value;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AnnotationDetectionFieldCallback}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationDetectionFieldCallbackUnitTests {

	/**
	 * @see DATACMNS-616
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullAnnotationType() {
		new AnnotationDetectionFieldCallback(null);
	}

	/**
	 * @see DATACMNS-616
	 */
	@Test
	public void looksUpValueFromPrivateField() {

		AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(Autowired.class);
		ReflectionUtils.doWithFields(Sample.class, callback);

		assertThat(callback.getType()).isEqualTo(String.class);
		assertThat(callback.<String> getValue(new Sample("foo"))).isEqualTo("foo");
	}

	/**
	 * @see DATACMNS-616
	 */
	@Test
	public void returnsNullForObjectNotContainingAFieldWithTheConfiguredAnnotation() {

		AnnotationDetectionFieldCallback callback = new AnnotationDetectionFieldCallback(Autowired.class);
		ReflectionUtils.doWithFields(Empty.class, callback);

		assertThat(callback.getType()).isNull();
		assertThat(callback.<Object> getValue(new Empty())).isNull();
	}

	@Value
	static class Sample {
		@Autowired String value;
	}

	static class Empty {}
}
