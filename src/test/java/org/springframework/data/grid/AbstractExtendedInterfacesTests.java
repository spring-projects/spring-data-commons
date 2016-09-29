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
 * Base test class sharing extended interfaces and mocked objects.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractExtendedInterfacesTests {

	protected interface ExtendedContainerNode<NID> extends ContainerNode<NID> {
		Object getNodeObject();
	}

	protected interface ExtendedContainerGroup<GID, NID> extends ContainerGroup<GID, NID, ExtendedContainerNode<NID>> {
		Object getGroupObject();
	}

	protected interface ExtendedContainerGrid<NID> extends ContainerGrid<NID, ExtendedContainerNode<NID>> {
		Object getGridObject();
	}

	protected interface ExtendedContainerGridGroup<GID, NID>
			extends ContainerGridGroups<GID, NID, ExtendedContainerNode<NID>, ExtendedContainerGroup<GID, NID>> {
		Object getGridGroupObject();
	}

	protected static class MockContainerGridListener implements ContainerGridListener<String, ExtendedContainerNode<String>> {

		List<ExtendedContainerNode<String>> added = new ArrayList<ExtendedContainerNode<String>>();
		List<ExtendedContainerNode<String>> removed = new ArrayList<ExtendedContainerNode<String>>();

		@Override
		public void nodeAdded(ExtendedContainerNode<String> node) {
			added.add(node);
		}

		@Override
		public void nodeRemoved(ExtendedContainerNode<String> node) {
			removed.add(node);
		}

	}

	protected static class MockContainerGridGroupsListener
			implements ContainerGridGroupsListener<String, String, ExtendedContainerNode<String>, ExtendedContainerGroup<String, String>> {

		List<ExtendedContainerGroup<String, String>> groupAdded = new ArrayList<ExtendedContainerGroup<String,String>>();
		List<ExtendedContainerGroup<String, String>> groupRemoved = new ArrayList<ExtendedContainerGroup<String,String>>();
		Map<String, List<ExtendedContainerNode<String>>> groupMemberAdded = new HashMap<String, List<ExtendedContainerNode<String>>>();
		Map<String, List<ExtendedContainerNode<String>>> groupMemberRemoved = new HashMap<String, List<ExtendedContainerNode<String>>>();

		@Override
		public void groupAdded(ExtendedContainerGroup<String, String> group) {
			groupAdded.add(group);
		}

		@Override
		public void groupRemoved(ExtendedContainerGroup<String, String> group) {
			groupRemoved.add(group);
		}

		@Override
		public void nodeAdded(ExtendedContainerGroup<String, String> group, ExtendedContainerNode<String> node) {
			List<ExtendedContainerNode<String>> list = groupMemberAdded.get(group.getId());
			if (list == null) {
				list = new ArrayList<ExtendedContainerNode<String>>();
				groupMemberAdded.put(group.getId(), list);
			}
			list.add(node);
		}

		@Override
		public void nodeRemoved(ExtendedContainerGroup<String, String> group, ExtendedContainerNode<String> node) {
			List<ExtendedContainerNode<String>> list = groupMemberRemoved.get(group.getId());
			if (list == null) {
				list = new ArrayList<ExtendedContainerNode<String>>();
				groupMemberRemoved.put(group.getId(), list);
			}
			list.add(node);
		}

	}

	protected static class MockContainerGridGroups extends MockContainerGrid implements ExtendedContainerGridGroup<String, String> {

		Map<String, ExtendedContainerGroup<String, String>> groups = new HashMap<String, ExtendedContainerGroup<String, String>>();
		DefaultContainerGridGroupsListener<String, String, ExtendedContainerNode<String>, ExtendedContainerGroup<String, String>> glistener =
				new DefaultContainerGridGroupsListener<String, String, ExtendedContainerNode<String>, ExtendedContainerGroup<String, String>>();

		@Override
		public boolean addGroup(ExtendedContainerGroup<String, String> group) {
			ExtendedContainerGroup<String, String> put = groups.put(group.getId(), group);
			glistener.groupAdded(group);
			if (group instanceof MockContainerGroup) {
				((MockContainerGroup)group).glistener = glistener;
			}
			return put == null;
		}

		@Override
		public boolean removeGroup(String id) {
			ExtendedContainerGroup<String, String> removed = groups.remove(id);
			if (removed != null) {
				glistener.groupRemoved(removed);
			}
			if (removed instanceof MockContainerGroup) {
				((MockContainerGroup)removed).glistener = null;
			}
			return removed != null;
		}

		@Override
		public ExtendedContainerGroup<String, String> getGroup(String id) {
			return groups.get(id);
		}

		@Override
		public Collection<ExtendedContainerGroup<String, String>> getGroups() {
			return groups.values();
		}

		@Override
		public ExtendedContainerGroup<String, String> getGroupByNode(String id) {
			Iterator<ExtendedContainerGroup<String, String>> iterator = groups.values().iterator();
			while (iterator.hasNext()) {
				ExtendedContainerGroup<String, String> group = iterator.next();
				if (group.hasNode(id)) {
					return group;
				}
			}
			return null;
		}

		@Override
		public void addContainerGridGroupsListener(
				ContainerGridGroupsListener<String, String, ExtendedContainerNode<String>, ExtendedContainerGroup<String, String>> listener) {
			glistener.register(listener);
		}

		@Override
		public Object getGridGroupObject() {
			return "gridGroupObject";
		}

	}

	protected static class MockContainerGrid implements ExtendedContainerGrid<String> {

		Map<String, ExtendedContainerNode<String>> nodes = new HashMap<String, ExtendedContainerNode<String>>();
		DefaultContainerGridListener<String, ExtendedContainerNode<String>> listener =
				new DefaultContainerGridListener<String, ExtendedContainerNode<String>>();

		@Override
		public Collection<ExtendedContainerNode<String>> getNodes() {
			return nodes.values();
		}

		@Override
		public ExtendedContainerNode<String> getNode(String id) {
			return nodes.get(id);
		}

		@Override
		public boolean addNode(ExtendedContainerNode<String> node) {
			ExtendedContainerNode<String> put = nodes.put(node.getId(), node);
			listener.nodeAdded(node);
			return put == null;
		}

		@Override
		public boolean removeNode(String id) {
			ExtendedContainerNode<String> removed = nodes.remove(id);
			if (removed != null) {
				listener.nodeRemoved(removed);
			}
			return removed != null;
		}

		@Override
		public void addContainerGridListener(ContainerGridListener<String, ExtendedContainerNode<String>> listener) {
			this.listener.register(listener);
		}

		@Override
		public Object getGridObject() {
			return "gridObject";
		}

	}

	protected static class MockContainerNode implements ExtendedContainerNode<String> {
		String name;

		MockContainerNode(String name) {
			this.name = name;
		}

		@Override
		public String getId() {
			return name;
		}

		@Override
		public Object getNodeObject() {
			return "nodeObject";
		}
	}

	protected static class MockContainerGroup implements ExtendedContainerGroup<String,String> {
		String name;
		Map<String, ExtendedContainerNode<String>> nodes = new HashMap<String, ExtendedContainerNode<String>>();
		DefaultContainerGridGroupsListener<String, String, ExtendedContainerNode<String>, ExtendedContainerGroup<String, String>> glistener;

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
		public boolean addNode(ExtendedContainerNode<String> member) {
			ExtendedContainerNode<String> put = nodes.put(member.getId(), member);
			if (glistener != null) {
				glistener.nodeAdded(this, member);
			}
			return put == null;
		}

		@Override
		public ExtendedContainerNode<String> removeNode(String id) {
			ExtendedContainerNode<String> removed = nodes.remove(id);
			if (glistener != null && removed != null) {
				glistener.nodeRemoved(this, removed);
			}
			return removed;
		}

		@Override
		public ExtendedContainerNode<String> getNode(String id) {
			return nodes.get(id);
		}

		@Override
		public Collection<ExtendedContainerNode<String>> getNodes() {
			return nodes.values();
		}

		@Override
		public Object getGroupObject() {
			return "groupObject";
		}

	}

}
