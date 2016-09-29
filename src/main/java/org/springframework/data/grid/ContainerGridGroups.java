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
 * Container groups is an extension on top of {@link ContainerGrid} to
 * introduce functionality of grouping of containers.
 * <p>
 * While this interface adds methods for groups the actual implementation
 * should handle group resolving when new Container nodes are added to
 * the grid using methods from a {@link ContainerGrid}.
 *
 * @author Janne Valkealahti
 *
 * @param <GID> the type of {@link ContainerGroup} identifier
 * @param <NID> the type of {@link ContainerNode} identifier
 * @param <CN> the type of {@link ContainerNode}
 * @param <CG> the type of {@link ContainerGroup}
 * @see ContainerGrid
 */
public interface ContainerGridGroups<GID, NID, CN extends ContainerNode<NID>, CG extends ContainerGroup<GID, NID, CN>> extends ContainerGrid<NID, CN> {

	/**
	 * Adds a new group.
	 * <p>
     * If a grid refuses to add a particular grid for any reason
     * other than that it already contains the group, it <i>must</i> throw
     * an exception (rather than returning <tt>false</tt>).  This preserves
     * the invariant that a grid always contains the specified group
     * after this call returns.
	 *
	 * @param group the container group
	 * @return <tt>true</tt> if this grid groups changed as a result of the call
	 */
	boolean addGroup(CG group);

	/**
	 * Removes a group by its id.
	 * <p>
     * Removes a single instance of the specified group from this
     * grid, if it is present.
	 *
	 * @param id the container group identifier
	 * @return <tt>true</tt> if a group was removed as a result of this call
	 */
	boolean removeGroup(GID id);

	/**
	 * Gets a Container group by its identifier.
	 *
	 * @param id the container group identifier
	 * @return Container group
	 */
	CG getGroup(GID id);

	/**
	 * Gets a collection of Container groups or empty collection of there
	 * are no groups defined.
	 *
	 * @return the collection of container groups
	 */
	Collection<CG> getGroups();

	/**
	 * Gets a Container Group where Container node belongs to.
	 *
	 * @param id the container node identifier
	 * @return the container group or <code>NULL</code> if not match
	 */
	CG getGroupByNode(NID id);

	/**
	 * Adds a listener to be notified of Container Group events.
	 *
	 * @param listener the container group listener
	 */
	void addContainerGridGroupsListener(ContainerGridGroupsListener<GID, NID, CN, CG> listener);

}
