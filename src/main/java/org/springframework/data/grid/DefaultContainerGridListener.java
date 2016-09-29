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
 * Composite listener for handling grid container node events.
 *
 * @author Janne Valkealahti
 *
 * @param <NID> the type of {@link ContainerNode} identifier
 * @param <CN> the type of {@link ContainerNode}
 */
public class DefaultContainerGridListener<NID, CN extends ContainerNode<NID>> extends
		AbstractCompositeListener<ContainerGridListener<NID, CN>> implements ContainerGridListener<NID, CN> {

	@Override
	public void nodeAdded(CN node) {
		for (Iterator<ContainerGridListener<NID, CN>> iterator = getListeners().reverse(); iterator.hasNext();) {
			iterator.next().nodeAdded(node);
		}
	}

	@Override
	public void nodeRemoved(CN node) {
		for (Iterator<ContainerGridListener<NID, CN>> iterator = getListeners().reverse(); iterator.hasNext();) {
			iterator.next().nodeRemoved(node);
		}
	}

}
