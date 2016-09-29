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

import java.util.Collection;

/**
 * Interface representing a container group.
 *
 * @author Janne Valkealahti
 *
 * @param <GID> the type of {@link ContainerGroup} identifier
 * @param <NID> the type of {@link ContainerNode} identifier
 * @param <CN> the type of {@link ContainerNode}
 */
public interface ContainerGroup<GID, NID, CN extends ContainerNode<NID>> {

	/**
	 * Gets a container group identifier.
	 *
	 * @return container group identifier
	 */
	GID getId();

	/**
	 * Checks if node is a member of this group.
	 *
	 * @param id the node identifier
	 * @return true, if node is a member of group
	 */
	boolean hasNode(NID id);

	/**
	 * Adds the container node.
	 * <p>
     * If a group refuses to add a particular node for any reason
     * other than that it already contains the node, it <i>must</i> throw
     * an exception (rather than returning <tt>false</tt>).  This preserves
     * the invariant that a group always contains the specified node
     * after this call returns.
     * <p>
     * Implementation itself should define if a node added in a group is
     * also added to a grid itself.
	 *
	 * @param node the node
	 * @return <tt>true</tt> if this group changed as a result of the call
	 */
	boolean addNode(CN node);

	/**
	 * Removes the node from this group if it is present. Returns the {@code ContainerNode} to which
	 * given node identifier was previously associated, or null if node wasn't a member of this group.
     * <p>
     * Implementation itself should define if a node removed from a group is
     * also removed from a grid itself.
	 *
	 * @param id the node identifier
	 * @return the removed container node, or <code>NULL</code> if node wasn't a member of this group.
	 */
	CN removeNode(NID id);

	/**
	 * Gets the container node.
	 *
	 * @param id the container node id
	 * @return the node, or <code>NULL</code> if node wasn't a member of this group.
	 */
	CN getNode(NID id);

	/**
	 * Gets the members of this group as {@code Collection}.
	 *
	 * @return the nodes
	 */
	Collection<CN> getNodes();

}
