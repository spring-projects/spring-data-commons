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
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests for extended interfaces.
 *
 * @author Janne Valkealahti
 *
 */
public class ExtendedInterfacesTests extends AbstractExtendedInterfacesTests {

	@SuppressWarnings("rawtypes")
	@Test
	public void testSimpleOperations() {
		MockContainerNode node1 = new MockContainerNode("mocknode1");
		MockContainerNode node2 = new MockContainerNode("mocknode2");
		MockContainerGroup group1 = new MockContainerGroup("mockgroup1");
		MockContainerGroup group2 = new MockContainerGroup("mockgroup2");
		MockContainerGridGroups groups = new MockContainerGridGroups();

		assertThat(node1, instanceOf(ExtendedContainerNode.class));
		assertThat(node2, instanceOf(ExtendedContainerNode.class));
		assertThat(group1, instanceOf(ExtendedContainerGroup.class));
		assertThat(group2, instanceOf(ExtendedContainerGroup.class));
		assertThat(groups, instanceOf(ExtendedContainerGrid.class));
		assertThat(groups, instanceOf(ExtendedContainerGridGroup.class));

		assertThat(((ExtendedContainerNode)node1).getNodeObject(), instanceOf(String.class));
		assertThat((String)((ExtendedContainerNode)node1).getNodeObject(), is("nodeObject"));
		assertThat(((ExtendedContainerNode)node2).getNodeObject(), instanceOf(String.class));
		assertThat((String)((ExtendedContainerNode)node2).getNodeObject(), is("nodeObject"));

		assertThat(((ExtendedContainerGroup)group1).getGroupObject(), instanceOf(String.class));
		assertThat((String)((ExtendedContainerGroup)group1).getGroupObject(), is("groupObject"));
		assertThat(((ExtendedContainerGroup)group2).getGroupObject(), instanceOf(String.class));
		assertThat((String)((ExtendedContainerGroup)group2).getGroupObject(), is("groupObject"));

		assertThat(((ExtendedContainerGrid)groups).getGridObject(), instanceOf(String.class));
		assertThat((String)((ExtendedContainerGrid)groups).getGridObject(), is("gridObject"));
		assertThat(((ExtendedContainerGridGroup)groups).getGridGroupObject(), instanceOf(String.class));
		assertThat((String)((ExtendedContainerGridGroup)groups).getGridGroupObject(), is("gridGroupObject"));

		// adding nodes to grid
		groups.addNode(node1);
		groups.addNode(node2);
		assertThat(groups.getNodes().size(), is(2));

		// adding nodes to groups
		groups.addGroup(group1);
		groups.addGroup(group2);
		group1.addNode(node1);
		group2.addNode(node2);
		assertThat(groups.getNode("mocknode1"), notNullValue());
		assertThat(groups.getNode("mocknode2"), notNullValue());
		assertThat(groups.getNode("mocknode1").getId(), is("mocknode1"));
		assertThat(groups.getNode("mocknode2").getId(), is("mocknode2"));
		assertThat(groups.getGroups().size(), is(2));
		assertThat(groups.getGroup("mockgroup1").getNodes().size(), is(1));
		assertThat(groups.getGroup("mockgroup1").hasNode("mocknode1"), is(true));
		assertThat(groups.getGroup("mockgroup1").hasNode("mocknode2"), is(false));
		assertThat(groups.getGroup("mockgroup1").getNode("mocknode1"), notNullValue());
		assertThat(groups.getGroup("mockgroup1").getNode("mocknode1").getId(), is("mocknode1"));

		// start removing nodes from a grid and groups
		assertThat(group1.removeNode("mocknode1"), notNullValue());
		assertThat(group1.removeNode("mocknode2"), nullValue());
		assertThat(groups.getGroup("mockgroup1").getNodes().size(), is(0));
		groups.removeNode("mocknode1");
		assertThat(groups.getNodes().size(), is(1));

		assertThat(group2.removeNode("mocknode2"), notNullValue());
		assertThat(group2.removeNode("mocknode1"), nullValue());
		assertThat(groups.getGroup("mockgroup2").getNodes().size(), is(0));
		groups.removeNode("mocknode2");
		assertThat(groups.getNodes().size(), is(0));

		assertThat(groups.getNode("mocknode1"), nullValue());
		assertThat(groups.getNode("mocknode2"), nullValue());
	}

	@Test
	public void testListener() {
		MockContainerGridListener listener = new MockContainerGridListener();
		MockContainerGridGroupsListener glistener = new MockContainerGridGroupsListener();
		MockContainerNode node1 = new MockContainerNode("mocknode1");
		MockContainerNode node2 = new MockContainerNode("mocknode2");
		MockContainerGroup group1 = new MockContainerGroup("mockgroup1");
		MockContainerGroup group2 = new MockContainerGroup("mockgroup2");
		MockContainerGridGroups groups = new MockContainerGridGroups();

		groups.addContainerGridListener(listener);
		groups.addContainerGridGroupsListener(glistener);

		groups.addNode(node1);
		groups.addNode(node2);
		assertThat(listener.added.size(), is(2));
		assertThat(listener.removed.size(), is(0));

		groups.addGroup(group1);
		groups.addGroup(group2);
		group1.addNode(node1);
		group2.addNode(node2);
		assertThat(glistener.groupAdded.size(), is(2));
		assertThat(glistener.groupMemberAdded.size(), is(2));
		assertThat(glistener.groupMemberAdded.get("mockgroup1").size(), is(1));
		assertThat(glistener.groupMemberAdded.get("mockgroup2").size(), is(1));

		group1.removeNode("mocknode1");
		group2.removeNode("mocknode2");
		groups.removeNode("mocknode1");
		groups.removeNode("mocknode2");
		groups.removeGroup("mockgroup1");
		groups.removeGroup("mockgroup2");
		assertThat(listener.added.size(), is(2));
		assertThat(listener.removed.size(), is(2));
		assertThat(glistener.groupRemoved.size(), is(2));
		assertThat(glistener.groupMemberRemoved.size(), is(2));
		assertThat(glistener.groupMemberRemoved.get("mockgroup1").size(), is(1));
		assertThat(glistener.groupMemberRemoved.get("mockgroup2").size(), is(1));
	}

}
