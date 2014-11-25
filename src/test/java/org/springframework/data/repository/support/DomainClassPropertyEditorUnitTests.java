/*
 * Copyright 2008-2013 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.beans.PropertyEditor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Unit test for {@link DomainClassPropertyEditor}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DomainClassPropertyEditorUnitTests {

	DomainClassPropertyEditor<User, Integer> editor;

	@Mock PropertyEditorRegistry registry;
	@Mock RepositoryInvoker invoker;
	@Mock EntityInformation<User, Integer> information;

	@Before
	public void setUp() {

		when(information.getIdType()).thenReturn(Integer.class);
		editor = new DomainClassPropertyEditor<User, Integer>(invoker, information, registry);
	}

	@Test
	public void convertsPlainIdTypeCorrectly() throws Exception {

		User user = new User(1);
		when(information.getId(user)).thenReturn(user.getId());
		when(invoker.invokeFindOne(1)).thenReturn(user);

		editor.setAsText("1");

		verify(invoker, times(1)).invokeFindOne(1);
	}

	@Test
	public void convertsEntityToIdCorrectly() throws Exception {

		User user = new User(1);
		editor.setValue(user);
		when(information.getId(user)).thenReturn(user.getId());
		assertThat(editor.getAsText(), is("1"));
	}

	@Test
	public void usesCustomEditorIfConfigured() throws Exception {

		PropertyEditor customEditor = mock(PropertyEditor.class);
		when(customEditor.getValue()).thenReturn(1);

		when(registry.findCustomEditor(Integer.class, null)).thenReturn(customEditor);

		convertsPlainIdTypeCorrectly();

		verify(customEditor, times(1)).setAsText("1");
	}

	@Test
	public void returnsNullIdIfNoEntitySet() throws Exception {

		editor.setValue(null);
		assertThat(editor.getAsText(), is(nullValue()));
	}

	@Test
	public void resetsValueToNullAfterEmptyStringConversion() throws Exception {

		assertValueResetToNullAfterConverting("");
	}

	@Test
	public void resetsValueToNullAfterNullStringConversion() throws Exception {

		assertValueResetToNullAfterConverting(null);
	}

	private void assertValueResetToNullAfterConverting(String source) throws Exception {

		convertsPlainIdTypeCorrectly();
		assertThat(editor.getValue(), is(notNullValue()));

		editor.setAsText(source);
		assertThat(editor.getValue(), is(nullValue()));
	}

	/**
	 * Sample entity.
	 * 
	 * @author Oliver Gierke
	 */
	@SuppressWarnings("serial")
	private static class User implements Persistable<Integer> {

		private Integer id;

		public User(Integer id) {

			this.id = id;
		}

		/*
						 * (non-Javadoc)
						 *
						 * @see org.springframework.data.domain.Persistable#getId()
						 */
		public Integer getId() {

			return id;
		}

		/*
						 * (non-Javadoc)
						 *
						 * @see org.springframework.data.domain.Persistable#isNew()
						 */
		public boolean isNew() {

			return getId() != null;
		}
	}
}
