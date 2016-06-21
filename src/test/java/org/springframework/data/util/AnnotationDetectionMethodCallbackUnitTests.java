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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ReflectionUtils;

/**
 * Unit tests for {@link AnnotationDetectionMethodCallback}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationDetectionMethodCallbackUnitTests {

	public @Rule ExpectedException exception = ExpectedException.none();

	/**
	 * @see DATACMNS-452
	 */
	@Test
	public void findsMethodWithAnnotation() throws Exception {

		AnnotationDetectionMethodCallback<Value> callback = new AnnotationDetectionMethodCallback<>(Value.class);
		ReflectionUtils.doWithMethods(Sample.class, callback);

		assertThat(callback.hasFoundAnnotation()).isTrue();
		assertThat(callback.getMethod()).isEqualTo(Sample.class.getMethod("getValue"));
		assertThat(callback.getAnnotation()).isNotNull();
		assertThat(callback.getAnnotation().value()).isEqualTo("#{null}");
	}

	/**
	 * @see DATACMNS-452
	 */
	@Test
	public void detectsAmbiguousAnnotations() {

		exception.expect(IllegalStateException.class);
		exception.expectMessage("Value");
		exception.expectMessage("getValue");
		exception.expectMessage("getOtherValue");

		AnnotationDetectionMethodCallback<Value> callback = new AnnotationDetectionMethodCallback<>(Value.class, true);
		ReflectionUtils.doWithMethods(Multiple.class, callback);
	}

	interface Sample {

		@Value("#{null}")
		Object getValue();
	}

	interface Multiple {

		@Value("#{null}")
		Object getValue();

		@Value("#{null}")
		Object getOtherValue();
	}
}
