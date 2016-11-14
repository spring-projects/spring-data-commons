/*
 * Copyright 2010-2012 the original author or authors.
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
package org.springframework.data.config;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.config.TypeFilterParser.Type;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Unit test for {@link TypeFilterParser}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class TypeFilterParserUnitTests {

	TypeFilterParser parser;
	Element documentElement;

	@Mock ReaderContext context;
	@Mock ClassLoader classLoader;

	@Mock ClassPathScanningCandidateComponentProvider scanner;

	@Before
	public void setUp() throws SAXException, IOException, ParserConfigurationException {

		parser = new TypeFilterParser(context, classLoader);

		Resource sampleXmlFile = new ClassPathResource("type-filter-test.xml", TypeFilterParserUnitTests.class);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);

		documentElement = factory.newDocumentBuilder().parse(sampleXmlFile.getInputStream()).getDocumentElement();
	}

	@Test
	public void parsesIncludesCorrectly() throws Exception {

		Element element = DomUtils.getChildElementByTagName(documentElement, "firstSample");

		Iterable<TypeFilter> filters = parser.parseTypeFilters(element, Type.INCLUDE);
		assertThat(filters).hasAtLeastOneElementOfType(AssignableTypeFilter.class);
	}

	@Test
	public void parsesExcludesCorrectly() throws Exception {

		Element element = DomUtils.getChildElementByTagName(documentElement, "secondSample");

		Iterable<TypeFilter> filters = parser.parseTypeFilters(element, Type.EXCLUDE);
		assertThat(filters).hasAtLeastOneElementOfType(AssignableTypeFilter.class);
	}
}
