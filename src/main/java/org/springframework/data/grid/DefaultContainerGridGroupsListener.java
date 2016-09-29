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

import java.util.Iterator;

/**
 * Composite listener for handling container group events.
 *
 * @author Janne Valkealahti
 *
 * @param <GID> the type of {@link ContainerGroup} identifier
 * @param <NID> the type of {@link ContainerNode} identifier
 * @param <CN> the type of {@link ContainerNode}
 * @param <CG> the type of {@link ContainerGroup}
 */
public class DefaultContainerGridGroupsListener<GID, NID, CN extends ContainerNode<NID>, CG extends ContainerGroup<GID, NID, CN>> extends
		AbstractCompositeListener<ContainerGridGroupsListener<GID, NID, CN, CG>> implements ContainerGridGroupsListener<GID, NID, CN, CG> {

	@Override
	public void groupAdded(CG group) {
		for (Iterator<ContainerGridGroupsListener<GID, NID, CN, CG>> iterator = getListeners().reverse(); iterator.hasNext();) {
			iterator.next().groupAdded(group);
		}
	}

	@Override
	public void groupRemoved(CG group) {
		for (Iterator<ContainerGridGroupsListener<GID, NID, CN, CG>> iterator = getListeners().reverse(); iterator.hasNext();) {
			iterator.next().groupRemoved(group);
		}
	}

	@Override
	public void nodeAdded(CG group, CN node) {
		for (Iterator<ContainerGridGroupsListener<GID, NID, CN, CG>> iterator = getListeners().reverse(); iterator.hasNext();) {
			iterator.next().nodeAdded(group, node);
		}
	}

	@Override
	public void nodeRemoved(CG group, CN node) {
		for (Iterator<ContainerGridGroupsListener<GID, NID ,CN ,CG>> iterator = getListeners().reverse(); iterator.hasNext();) {
			iterator.next().nodeRemoved(group, node);
		}
	}

}
