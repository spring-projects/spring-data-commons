/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data;

import static de.schauderhaft.degraph.check.JCheck.*;
import static org.junit.Assert.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Jens Schauder
 */
@Disabled("Requires newer version of ASM 5.1")
public class DependencyTests {

	@Test
	public void noInternalPackageCycles() {

		assertThat(
				classpath() //
						.noJars() //
						.including("org.springframework.data.**") //
						.filterClasspath("*target/classes") //
						.printOnFailure("degraph.graphml"), //
				violationFree() //
		);
	}

}
