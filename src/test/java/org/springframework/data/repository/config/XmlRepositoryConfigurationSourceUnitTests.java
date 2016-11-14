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
package org.springframework.data.repository.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Element;

/**
 * Unit tests for {@link XmlRepositoryConfigurationSource}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class XmlRepositoryConfigurationSourceUnitTests {

	@Mock Element element;

	/**
	 * @see DATACMNS-456
	 */
	@Test
	public void translatesCamelCaseAttributeNameIntoXmlEquivalent() {

		RepositoryConfigurationSource source = mock(XmlRepositoryConfigurationSource.class);
		ReflectionTestUtils.setField(source, "element", element);
		when(source.getAttribute(anyString())).thenCallRealMethod();

		when(element.getAttribute("some-xml-attribute")).thenReturn("value");

		assertThat(source.getAttribute("someXmlAttribute")).isEqualTo("value");
	}
}
