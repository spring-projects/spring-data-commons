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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Base test class sharing mock components for {@code ContainerGridGroups}.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractContainerGridGroupsTests extends AbstractContainerGridTests {

	protected static class MockContainerGridGroupsListener
			implements ContainerGridGroupsListener<String, String, ContainerNode<String>, ContainerGroup<String, String, ContainerNode<String>>> {

		List<ContainerGroup<String, String, ContainerNode<String>>> groupAdded = new ArrayList<ContainerGroup<String,String,ContainerNode<String>>>();
		List<ContainerGroup<String, String, ContainerNode<String>>> groupRemoved = new ArrayList<ContainerGroup<String,String,ContainerNode<String>>>();
		Map<String, List<ContainerNode<String>>> groupMemberAdded = new HashMap<String, List<ContainerNode<String>>>();
		Map<String, List<ContainerNode<String>>> groupMemberRemoved = new HashMap<String, List<ContainerNode<String>>>();

		@Override
		public void groupAdded(ContainerGroup<String, String, ContainerNode<String>> group) {
			groupAdded.add(group);
		}

		@Override
		public void groupRemoved(ContainerGroup<String, String, ContainerNode<String>> group) {
			groupRemoved.add(group);
		}

		@Override
		public void nodeAdded(ContainerGroup<String, String, ContainerNode<String>> group, ContainerNode<String> node) {
			List<ContainerNode<String>> list = groupMemberAdded.get(group.getId());
			if (list == null) {
				list = new ArrayList<ContainerNode<String>>();
				groupMemberAdded.put(group.getId(), list);
			}
			list.add(node);
		}

		@Override
		public void nodeRemoved(ContainerGroup<String, String, ContainerNode<String>> group, ContainerNode<String> node) {
			List<ContainerNode<String>> list = groupMemberRemoved.get(group.getId());
			if (list == null) {
				list = new ArrayList<ContainerNode<String>>();
				groupMemberRemoved.put(group.getId(), list);
			}
			list.add(node);
		}

	}

	protected static class MockContainerGroup implements ContainerGroup<String, String, ContainerNode<String>> {
		String name;
		Map<String, ContainerNode<String>> nodes = new HashMap<String, ContainerNode<String>>();

		DefaultContainerGridGroupsListener<String, String, ContainerNode<String>, ContainerGroup<String, String, ContainerNode<String>>> glistener;

		MockContainerGroup(String name) {
			this.name = name;
		}

		@Override
		public String getId() {
			return name;
		}

		@Override
		public boolean hasNode(String id) {
			return nodes.containsKey(id);
		}

		@Override
		public ContainerNode<String> getNode(String id) {
			return nodes.get(id);
		}

		@Override
		public Collection<ContainerNode<String>> getNodes() {
			return nodes.values();
		}

		@Override
		public boolean addNode(ContainerNode<String> member) {
			ContainerNode<String> put = nodes.put(member.getId(), member);
			if (glistener != null) {
				glistener.nodeAdded(this, member);
			}
			return put == null;
		}

		@Override
		public ContainerNode<String> removeNode(String id) {
			ContainerNode<String> removed = nodes.remove(id);
			if (glistener != null && removed != null) {
				glistener.nodeRemoved(this, removed);
			}
			return removed;
		}
	}

	protected static class MockContainerGridGroups extends MockContainerGrid
			implements ContainerGridGroups<String, String, ContainerNode<String>, ContainerGroup<String, String, ContainerNode<String>>> {

		Map<String, ContainerGroup<String, String, ContainerNode<String>>> groups = new HashMap<String, ContainerGroup<String, String, ContainerNode<String>>>();
		DefaultContainerGridGroupsListener<String, String, ContainerNode<String>, ContainerGroup<String, String, ContainerNode<String>>> glistener =
				new DefaultContainerGridGroupsListener<String, String, ContainerNode<String>, ContainerGroup<String, String, ContainerNode<String>>>();

		@Override
		public boolean addGroup(ContainerGroup<String, String, ContainerNode<String>> group) {
			ContainerGroup<String, String, ContainerNode<String>> put = groups.put(group.getId(), group);
			glistener.groupAdded(group);
			if (group instanceof MockContainerGroup) {
				((MockContainerGroup)group).glistener = glistener;
			}
			return put == null;
		}

		@Override
		public boolean removeGroup(String id) {
			ContainerGroup<String, String, ContainerNode<String>> removed = groups.remove(id);
			if (removed != null) {
				glistener.groupRemoved(removed);
			}
			if (removed instanceof MockContainerGroup) {
				((MockContainerGroup)removed).glistener = null;
			}
			return removed != null;
		}

		@Override
		public ContainerGroup<String, String, ContainerNode<String>> getGroup(String id) {
			return groups.get(id);
		}

		@Override
		public Collection<ContainerGroup<String, String, ContainerNode<String>>> getGroups() {
			return groups.values();
		}

		@Override
		public ContainerGroup<String, String, ContainerNode<String>> getGroupByNode(String id) {
			Iterator<ContainerGroup<String, String, ContainerNode<String>>> iterator = groups.values().iterator();
			while (iterator.hasNext()) {
				ContainerGroup<String, String, ContainerNode<String>> group = iterator.next();
				if (group.hasNode(id)) {
					return group;
				}
			}
			return null;
		}

		@Override
		public void addContainerGridGroupsListener(
				ContainerGridGroupsListener<String, String, ContainerNode<String>, ContainerGroup<String, String, ContainerNode<String>>> listener) {
			glistener.register(listener);
		}
	}

}
