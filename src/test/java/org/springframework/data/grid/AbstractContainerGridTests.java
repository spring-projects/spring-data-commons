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
import java.util.List;
import java.util.Map;

/**
 * Base test class sharing mock components for {@code ContainerGrid}.
 *
 * @author Janne Valkealahti
 *
 */
public abstract class AbstractContainerGridTests {

	protected static class MockContainerGridListener implements ContainerGridListener<String, ContainerNode<String>> {

		List<ContainerNode<String>> added = new ArrayList<ContainerNode<String>>();
		List<ContainerNode<String>> removed = new ArrayList<ContainerNode<String>>();

		@Override
		public void nodeAdded(ContainerNode<String> node) {
			added.add(node);
		}

		@Override
		public void nodeRemoved(ContainerNode<String> node) {
			removed.add(node);
		}

	}

	protected static class MockContainerNode implements ContainerNode<String> {
		String name;
		MockContainerNode(String name) {
			this.name = name;
		}
		@Override
		public String getId() {
			return name;
		}
	}

	protected static class MockContainerGrid implements ContainerGrid<String, ContainerNode<String>> {

		Map<String, ContainerNode<String>> nodes = new HashMap<String, ContainerNode<String>>();
		DefaultContainerGridListener<String, ContainerNode<String>> listener =
				new DefaultContainerGridListener<String, ContainerNode<String>>();

		@Override
		public Collection<ContainerNode<String>> getNodes() {
			return nodes.values();
		}

		@Override
		public ContainerNode<String> getNode(String id) {
			return nodes.get(id);
		}

		@Override
		public boolean addNode(ContainerNode<String> node) {
			ContainerNode<String> put = nodes.put(node.getId(), node);
			listener.nodeAdded(node);
			return put == null;
		}

		@Override
		public boolean removeNode(String id) {
			ContainerNode<String> removed = nodes.remove(id);
			if (removed != null) {
				listener.nodeRemoved(removed);
			}
			return removed != null;
		}

		@Override
		public void addContainerGridListener(ContainerGridListener<String, ContainerNode<String>> listener) {
			this.listener.register(listener);
		}

	}

}
