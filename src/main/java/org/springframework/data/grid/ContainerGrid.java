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
 * Container grid allows to track members in a grid. Contract for
 * interacting with user depends on the actual implementation which
 * can be either build-in discovery system or static without any
 * discovery logic.
 * <p>
 * If implementation has build-in discovery a use of setter methods outside
 * of the implementation should be discouraged. Although it is up to
 * the implementation what is actually supported.
 * <p>
 * Implementation can also be static in terms of that there are
 * no logic for discovery and in case of that user is responsible
 * to feed data into the implementation. In case of that the implementation
 * would only have structure to store the necessary information.
 * This is useful in cases where access to grid system needs to be abstracted
 * still keeping the grid logic in its own implementation.
 * <p>
 * This interface and its extended interfaces are strongly typed
 * order to allow extending returned types (i.e. {@link ContainerNode}
 * to suit the needs of a custom implementations.
 *
 *
 * @author Janne Valkealahti
 *
 * @param <NID> the type of {@link ContainerNode} identifier
 * @param <CN> the type of {@link ContainerNode}
 */
public interface ContainerGrid<NID, CN extends ContainerNode<NID>> {

	/**
	 * Gets collection of container nodes know to the grid system or
	 * empty collection if there are no known nodes.
	 *
	 * @return Collection of grid nodes
	 */
	Collection<CN> getNodes();

	/**
	 * Gets a container node by its identifier.
	 *
	 * @param id the container node identifier
	 * @return Container node or <code>NULL</code> if node doesn't exist
	 */
	CN getNode(NID id);

	/**
	 * Adds a new container node.
	 * <p>
     * If a grid refuses to add a particular node for any reason
     * other than that it already contains the node, it <i>must</i> throw
     * an exception (rather than returning <tt>false</tt>).  This preserves
     * the invariant that a grid always contains the specified node
     * after this call returns.
     *
	 * @param node the container node
	 * @return <tt>true</tt> if this grid changed as a result of the call
	 */
	boolean addNode(CN node);

	/**
	 * Removes a container node by its identifier.
	 * <p>
	 * Removes a single instance of the specified node from this
     * grid, if it is present.
	 *
	 * @param id the container node identifier
	 * @return <tt>true</tt> if a node was removed as a result of this call
	 */
	boolean removeNode(NID id);

	/**
	 * Adds a listener to be notified of grid container node events.
	 *
	 * @param listener the container grid listener
	 */
	void addContainerGridListener(ContainerGridListener<NID, CN> listener);

}
