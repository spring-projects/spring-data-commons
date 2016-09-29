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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests for rebalance methods.
 *
 * @author Janne Valkealahti
 *
 */
public class RebalanceTests extends AbstractRebalanceTests {

	@Test
	public void testSimpleOperations() {
		MockManagedContainerGridGroups groups = new MockManagedContainerGridGroups();
		MockContainerGroup group1 = new MockContainerGroup("mockgroup1");
		MockContainerGroup group2 = new MockContainerGroup("mockgroup2");
		groups.addGroup(group1);
		groups.addGroup(group2);

		assertThat(groups.getGroupsRebalanceData(), instanceOf(ExtendedGroupsRebalanceData.class));
		assertThat(groups.getGroupsRebalanceData().getRebalanceObject(), instanceOf(String.class));
		assertThat((String)groups.getGroupsRebalanceData().getRebalanceObject(), is("rebalanceObject"));

		// no functional test results, just test that methods are there
		assertThat(groups.setGroupSize("mockgroup1", 0), is(false));
	}

}
