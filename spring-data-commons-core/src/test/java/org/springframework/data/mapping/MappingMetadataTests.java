/*
 * Copyright (c) 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.mapping;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.mapping.model.Association;
import org.springframework.data.mapping.model.PersistentEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.Assert.*;

/**
 * @author Jon Brisbin <jbrisbin@vmware.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:mapping.xml")
public class MappingMetadataTests {

  BasicMappingContext ctx;

  @Before
  public void setup() {
    ctx = new BasicMappingContext();
  }

  @Test
  public void testDoesRecognizePersistentEntities() {
    boolean persistentEntity = ctx.isPersistentEntity(PersonNoId.class);
    assertFalse(persistentEntity);

    persistentEntity = ctx.isPersistentEntity(PersonPersistent.class);
    assertTrue(persistentEntity);
  }

  @Test
  public void testPojoWithId() {
    PersistentEntity<PersonWithId> person = ctx.addPersistentEntity(PersonWithId.class);
    assertNotNull(person.getIdProperty());
    assertEquals(String.class, person.getIdProperty().getType());
  }

  @Test
  public void testPojoWithoutId() {
    PersistentEntity<PersonNoId> person = ctx.addPersistentEntity(PersonNoId.class);
    assertNull(person.getIdProperty());
  }

  @Test
  public void testAssociations() {
    PersistentEntity<PersonWithChildren> person = ctx.addPersistentEntity(PersonWithChildren.class);
    assertNotNull(person.getAssociations());

    for (Association association : person.getAssociations()) {
      assertEquals(Child.class, association.getInverse().getComponentType());
    }
  }

  @Test
  public void testDoesRecognizeDocumentEntities() {
    // PersistentEntity<PersonDocument> person = ctx.addPersistentEntity(PersonDocument.class);
    boolean persistentEntity = ctx.isPersistentEntity(PersonDocument.class);
    assertTrue(persistentEntity);
  }

}