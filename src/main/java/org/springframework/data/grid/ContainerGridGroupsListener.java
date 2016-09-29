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

/**
 * Listener for grid container group events.
 *
 * @author Janne Valkealahti
 *
 * @param <GID> the type of {@link ContainerGroup} identifier
 * @param <NID> the type of {@link ContainerNode} identifier
 * @param <CN> the type of {@link ContainerNode}
 * @param <CG> the type of {@link ContainerGroup}
 */
public interface ContainerGridGroupsListener<GID, NID, CN extends ContainerNode<NID>, CG extends ContainerGroup<GID, NID, CN>> {

	/**
	 * Invoked when group is added.
	 *
	 * @param group the {@link ContainerGroup}
	 */
	void groupAdded(CG group);

	/**
	 * Invoked when group is removed.
	 *
	 * @param group the {@link ContainerGroup}
	 */
	void groupRemoved(CG group);

	/**
	 * Invoked when node is added into a group.
	 *
	 * @param group the {@link ContainerGroup} node belongs to
	 * @param node the {@link ContainerNode}
	 */
	void nodeAdded(CG group, CN node);

	/**
	 * Invoked when node is removed from a group.
	 *
	 * @param group the {@link ContainerGroup} node belongs to
	 * @param node the {@link ContainerNode}
	 */
	void nodeRemoved(CG group, CN node);

}
