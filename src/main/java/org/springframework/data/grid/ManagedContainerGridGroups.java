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
 * Managed container groups is an extension on top of {@link ContainerGridGroups}
 * to introduce functionality of managing the group sizes.
 * <p>
 * Main purpose of this interface is to allow user to resize the Container
 * Group and request instructions of how group should be resized.
 * Implementation may do these automatically but in case it relies
 * other party to do it externally the {@link GroupsRebalanceData}
 * returned from a method {@link #getGroupsRebalanceData()} should
 * have enough instructions order to do it.
 *
 * @author Janne Valkealahti
 *
 * @param <GID> the type of {@link ContainerGroup} identifier
 * @param <NID> the type of {@link ContainerNode} identifier
 * @param <CN> the type of {@link ContainerNode}
 * @param <CG> the type of {@link ContainerGroup}
 * @param <GRD> the type of {@link GroupsRebalanceData}
 * @see ContainerGridGroups
 * @see ContainerGrid
 */
public interface ManagedContainerGridGroups<GID, NID, CN extends ContainerNode<NID>, CG extends ContainerGroup<GID, NID, CN>, GRD extends GroupsRebalanceData>
		extends ContainerGridGroups<GID, NID, CN, CG> {

	/**
	 * Sets the projected container group size.
	 *
	 * @param id Container group identifier
	 * @param size New size of the Container group
	 * @return true, if group size was modified
	 */
	boolean setGroupSize(GID id, int size);

	/**
	 * Gets the groups rebalance data.
	 * <p>
	 * Data returned from this method would reflect instructions to rebalance
	 * grouping sizes to satisfy a difference between what the situation is
	 * now and what it is expected to be.
	 *
	 * @return the groups rebalance data
	 */
	GRD getGroupsRebalanceData();

}
