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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

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

        new PartTree("firstnameOrderByLastnameOrderByFirstname", User.class);
    }


    @Test
    public void parsesSimplePropertyCorrectly() throws Exception {

        PartTree partTree = new PartTree("firstname", User.class);
        assertPart(partTree, new Part[] { new Part("firstname", User.class) });
    }


    @Test
    public void parsesAndPropertiesCorrectly() throws Exception {

        PartTree partTree = new PartTree("firstnameAndLastname", User.class);
        assertPart(partTree, new Part[] { new Part("firstname", User.class),
                new Part("lastname", User.class) });
        assertThat(partTree.getSort(), is(nullValue()));
    }


    @Test
    public void parsesOrPropertiesCorrectly() throws Exception {

        PartTree partTree = new PartTree("firstnameOrLastname", User.class);
        assertPart(partTree, new Part[] { new Part("firstname", User.class) },
                new Part[] { new Part("lastname", User.class) });
        assertThat(partTree.getSort(), is(nullValue()));
    }


    @Test
    public void parsesCombinedAndAndOrPropertiesCorrectly() throws Exception {

        PartTree tree =
                new PartTree("firstnameAndLastnameOrLastname", User.class);
        assertPart(tree, new Part[] { new Part("firstname", User.class),
                new Part("lastname", User.class) }, new Part[] { new Part(
                "lastname", User.class) });
    }


    @Test
    public void hasSortIfOrderByIsGiven() throws Exception {

        PartTree partTree =
                new PartTree("firstnameOrderByLastnameDesc", User.class);
        assertThat(partTree.getSort(), is(new Sort(Direction.DESC, "lastname")));
    }


    @Test
    public void detectsDistinctCorrectly() throws Exception {

        PartTree tree = new PartTree("findDistinctByLastname", User.class);
        assertThat(tree.isDistinct(), is(true));

        tree = new PartTree("findUsersDistinctByLastname", User.class);
        assertThat(tree.isDistinct(), is(true));

        tree = new PartTree("findDistinctUsersByLastname", User.class);
        assertThat(tree.isDistinct(), is(true));

        tree = new PartTree("findUsersByLastname", User.class);
        assertThat(tree.isDistinct(), is(false));

        tree = new PartTree("findByLastname", User.class);
        assertThat(tree.isDistinct(), is(false));

        // Check it's non-greedy (would strip everything until Order*By*
        // otherwise)
        tree = new PartTree("findByLastnameOrderByFirstnameDesc", User.class);
        assertThat(tree.isDistinct(), is(false));
        assertThat(tree.getSort(), is(new Sort(Direction.DESC, "firstname")));
    }

    
    @Test
    public void parsesWithinCorrectly () {
      PartTree tree = new PartTree("findByLocationWithin", User.class);
      for (Part part : tree.getParts()) {
        assertThat(part.getType(), is(Type.WITHIN));
        assertThat(part.getProperty(), is(new Property("location", User.class)));
      }
    }
    
    @Test
    public void parsesNearCorrectly () {
      PartTree tree = new PartTree("findByLocationNear", User.class);
      for (Part part : tree.getParts()) {
        assertThat(part.getType(), is(Type.NEAR));
        assertThat(part.getProperty(), is(new Property("location", User.class)));
      }
    }

    private void assertPart(PartTree tree, Part[]... parts) {

        Iterator<OrPart> iterator = tree.iterator();
        for (Part[] part : parts) {
            assertThat(iterator.hasNext(), is(true));
            Iterator<Part> partIterator = iterator.next().iterator();
            for (int k = 0; k < part.length; k++) {
                assertThat(String.format("Expected %d parts but have %d",
                        part.length, k + 1), partIterator.hasNext(), is(true));
                Part next = partIterator.next();
                assertThat(
                        String.format("Expected %s but got %s!", part[k], next),
                        part[k], is(next));
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
