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
package org.springframework.data.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.stereotype.Component;

/**
 * Unit tests for {@link AnnotationAttribute}.
 * 
 * @author Oliver Gierke
 */
public class AnnotationAttributeUnitTests {

	@Test(expected = IllegalArgumentException.class) // DATACMNS-607
	public void rejectsNullAnnotationType() {
		new AnnotationAttribute(null);
	}

	@Test(expected = IllegalArgumentException.class) // DATACMNS-607
	public void rejectsNullAnnotationTypeForAnnotationAndAttributeName() {
		new AnnotationAttribute(null, "name");
	}

	@Test // DATACMNS-607
	public void looksUpAttributeFromAnnotatedElement() {

		AnnotationAttribute attribute = new AnnotationAttribute(Component.class);
		assertThat(attribute.getValueFrom(Sample.class), is((Object) "foo"));
	}

	@Component("foo")
	static class Sample {

	}
}
