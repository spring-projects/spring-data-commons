/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.grid;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests for {@code ContainerGrid} interfaces.
 *
 * @author Janne Valkealahti
 *
 */
public class ContainerGridTests extends AbstractContainerGridTests {

	@Test
	public void testSimpleOperations() {
		ContainerGrid<String, ContainerNode<String>> grid = new MockContainerGrid();
		grid.addNode(new MockContainerNode("mock1"));
		grid.addNode(new MockContainerNode("mock2"));
		assertThat(grid.getNodes().size(), is(2));
		assertThat(grid.getNode("mock1"), notNullValue());
		assertThat(grid.getNode("mock2"), notNullValue());
		assertThat(grid.getNode("mock1").getId(), is("mock1"));
		assertThat(grid.getNode("mock2").getId(), is("mock2"));
		grid.removeNode("mock2");
		assertThat(grid.getNodes().size(), is(1));
		grid.removeNode("mock1");
		assertThat(grid.getNodes().size(), is(0));
		assertThat(grid.getNode("mock1"), nullValue());
		assertThat(grid.getNode("mock2"), nullValue());
	}

	@Test
	public void testListener() {
		MockContainerGridListener listener = new MockContainerGridListener();
		ContainerGrid<String, ContainerNode<String>> grid = new MockContainerGrid();
		grid.addContainerGridListener(listener);
		grid.addNode(new MockContainerNode("mock1"));
		grid.addNode(new MockContainerNode("mock2"));
		assertThat(listener.added.size(), is(2));
		assertThat(listener.removed.size(), is(0));
		grid.removeNode("mock1");
		grid.removeNode("mock2");
		assertThat(listener.added.size(), is(2));
		assertThat(listener.removed.size(), is(2));
	}

}
