/*
 * Copyright 2011 the original author or authors.
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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link ParameterizedTypeInformation}.
 *
 * @author Oliver Gierke
 */
public class ParameterizedTypeUnitTests {

	@Test
	public void considersTypeInformationsWithDifferingParentsNotEqual() {
		
		TypeDiscoverer<String> stringParent = new TypeDiscoverer<String>(String.class, null);
		TypeDiscoverer<Object> objectParent = new TypeDiscoverer<Object>(Object.class, null);
		
		ParameterizedTypeInformation<Object> first = new ParameterizedTypeInformation<Object>(Object.class, stringParent);
		ParameterizedTypeInformation<Object> second = new ParameterizedTypeInformation<Object>(Object.class, objectParent);
		
		assertFalse(first.equals(second));
	}
	
	@Test
	public void considersTypeInformationsWithSameParentsNotEqual() {
		
		TypeDiscoverer<String> stringParent = new TypeDiscoverer<String>(String.class, null);
		
		ParameterizedTypeInformation<Object> first = new ParameterizedTypeInformation<Object>(Object.class, stringParent);
		ParameterizedTypeInformation<Object> second = new ParameterizedTypeInformation<Object>(Object.class, stringParent);
		
		assertTrue(first.equals(second));
	}
}
