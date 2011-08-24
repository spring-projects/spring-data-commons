/*
 * Copyright 2008-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.data.repository.query.parser;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Iterator;

import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree.OrPart;

/**
 * Unit tests for {@link PartTree}.
 *
 * @author Oliver Gierke
 */
public class PartTreeUnitTests {

	private String[] PREFIXES = { "find", "read", "get" };

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullSource() throws Exception {
		new PartTree(null, getClass());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullDomainClass() throws Exception {
		new PartTree("test", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsMultipleOrderBy() throws Exception {
		partTree("firstnameOrderByLastnameOrderByFirstname");
	}

	@Test
	public void parsesSimplePropertyCorrectly() throws Exception {
		PartTree partTree = partTree("firstname");
		assertPart(partTree, parts("firstname"));
	}

	@Test
	public void parsesAndPropertiesCorrectly() throws Exception {
		PartTree partTree = partTree("firstnameAndLastname");
		assertPart(partTree, parts("firstname", "lastname"));
		assertThat(partTree.getSort(), is(nullValue()));
	}

	@Test
	public void parsesOrPropertiesCorrectly() throws Exception {
		PartTree partTree = partTree("firstnameOrLastname");
		assertPart(partTree, parts("firstname"), parts("lastname"));
		assertThat(partTree.getSort(), is(nullValue()));
	}

	@Test
	public void parsesCombinedAndAndOrPropertiesCorrectly() throws Exception {
		PartTree tree = partTree("firstnameAndLastnameOrLastname");
		assertPart(tree, parts("firstname", "lastname"), parts("lastname"));
	}

	@Test
	public void hasSortIfOrderByIsGiven() throws Exception {
		PartTree partTree = partTree("firstnameOrderByLastnameDesc");
		assertThat(partTree.getSort(), is(new Sort(Direction.DESC, "lastname")));
	}

	@Test
	public void hasSortIfOrderByIsGivenWithAllIgnoreCase() throws Exception {
		hasSortIfOrderByIsGivenWithAllIgnoreCase("firstnameOrderByLastnameDescAllIgnoreCase");
		hasSortIfOrderByIsGivenWithAllIgnoreCase("firstnameOrderByLastnameDescAllIgnoringCase");
		hasSortIfOrderByIsGivenWithAllIgnoreCase("firstnameAllIgnoreCaseOrderByLastnameDesc");
	}

	private void hasSortIfOrderByIsGivenWithAllIgnoreCase(String source) throws Exception {
		PartTree partTree= partTree(source);
		assertThat(partTree.getSort(), is(new Sort(Direction.DESC, "lastname")));
	}

	@Test
	public void detectsDistinctCorrectly() throws Exception {
		for (String prefix : PREFIXES) {
			detectsDistinctCorrectly(prefix + "DistinctByLastname", true);
			detectsDistinctCorrectly(prefix + "UsersDistinctByLastname", true);
			detectsDistinctCorrectly(prefix + "DistinctUsersByLastname", true);
			detectsDistinctCorrectly(prefix + "UsersByLastname", false);
			detectsDistinctCorrectly(prefix + "ByLastname", false);
			// Check it's non-greedy (would strip everything until Order*By*
			// otherwise)
			PartTree tree = detectsDistinctCorrectly(prefix + "ByLastnameOrderByFirstnameDesc", false);
			assertThat(tree.getSort(), is(new Sort(Direction.DESC, "firstname")));
		}
	}

	private PartTree detectsDistinctCorrectly(String source, boolean expected) {
		PartTree tree = partTree(source);
		assertThat("Unexpected distinct value for '" + source + "'", tree.isDistinct(), is(expected));
		return tree;
	}

	@Test
	public void parsesWithinCorrectly() {
		PartTree tree = partTree("findByLocationWithin");
		for (Part part : tree.getParts()) {
			assertThat(part.getType(), is(Type.WITHIN));
			assertThat(part.getProperty(), is(newProperty("location")));
		}
	}

	@Test
	public void parsesNearCorrectly() {
		PartTree tree = partTree("findByLocationNear");
		for (Part part : tree.getParts()) {
			assertThat(part.getType(), is(Type.NEAR));
			assertThat(part.getProperty(), is(newProperty("location")));
		}
	}

	@Test
	public void supportToStringWithoutSortOrder() throws Exception {
		PartTree tree = partTree("firstname");
		assertThat(tree.toString(), is(equalTo("firstname SIMPLE_PROPERTY")));
	}

	@Test
	public void supportToStringWithSortOrder() throws Exception {
		PartTree tree = partTree("firstnameOrderByLastnameDesc");
		assertThat(tree.toString(), is(equalTo("firstname SIMPLE_PROPERTY Order By lastname: DESC")));
	}

	@Test
	public void detectsIgnoreAllCase() throws Exception {
		detectsIgnoreAllCase("firstnameOrderByLastnameDescAllIgnoreCase",true);
		detectsIgnoreAllCase("firstnameOrderByLastnameDescAllIgnoringCase",true);
		detectsIgnoreAllCase("firstnameAllIgnoreCaseOrderByLastnameDesc",true);
		detectsIgnoreAllCase("getByFirstnameAllIgnoreCase",true);
		detectsIgnoreAllCase("getByFirstname",false);
		detectsIgnoreAllCase("firstnameOrderByLastnameDesc",false);
	}

	private void detectsIgnoreAllCase(String source, boolean expected) throws Exception {
		PartTree tree = partTree(source);
		assertThat(tree.shouldAlwaysIgnoreCase(), is(expected));
		for (Part part : tree.getParts()) {
			assertThat(part.shouldIgnoreCase(), is(expected));
		}
	}

	@Test
	public void detectsSpecificIgnoreCase() throws Exception {
		PartTree tree = partTree("findByFirstnameIgnoreCaseAndLastname");
		assertPart(tree, parts("firstname","lastname"));
		Iterator<Part> parts = tree.getParts().iterator();
		assertThat(parts.next().shouldIgnoreCase(), is(true));
		assertThat(parts.next().shouldIgnoreCase(), is(false));
	}

	@Test
	public void detectsSpecificIgnoringCase() throws Exception {
		PartTree tree = partTree("findByFirstnameIgnoringCaseAndLastname");
		assertPart(tree, parts("firstname","lastname"));
		Iterator<Part> parts = tree.getParts().iterator();
		assertThat(parts.next().shouldIgnoreCase(), is(true));
		assertThat(parts.next().shouldIgnoreCase(), is(false));
	}

	@Test
	public void doesNotIgnoreCaseIfNotStringProperty() throws Exception {
		PartTree tree = partTree("findByLocationIgnoringCase");
		assertPart(tree, parts("location"));
		Iterator<Part> parts = tree.getParts().iterator();
		assertThat(parts.next().shouldIgnoreCase(), is(false));
	}

	private PartTree partTree(String source) {
		return new PartTree(source, User.class);
	}

	private Part part(String part) {
		return new Part(part, User.class);
	}

	private Part[] parts(String... part) {
		Part[] parts = new Part[part.length];
		for (int i = 0; i < parts.length; i++) {
			parts[i] = part(part[i]);
		}
		return parts;
	}

	private Property newProperty(String name) {
		return new Property(name, User.class);
	}

	private void assertPart(PartTree tree, Part[]... parts) {
		Iterator<OrPart> iterator = tree.iterator();
		for (Part[] part : parts) {
			assertThat(iterator.hasNext(), is(true));
			Iterator<Part> partIterator = iterator.next().iterator();
			for (int k = 0; k < part.length; k++) {
				assertThat(String.format("Expected %d parts but have %d", part.length, k), partIterator.hasNext(),
						is(true));
				Part next = partIterator.next();
				assertThat(String.format("Expected %s but got %s!", part[k], next), part[k], is(next));
			}
			assertThat("Too many parts!", partIterator.hasNext(), is(false));
		}
		assertThat("Too many or parts!", iterator.hasNext(), is(false));
	}

	class User {
		String firstname;
		String lastname;
		double[] location;
	}
}
