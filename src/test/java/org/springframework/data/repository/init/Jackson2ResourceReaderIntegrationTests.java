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
package org.springframework.data.repository.init;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Integration tests for {@link JacksonResourceReader}.
 * 
 * @author Oliver Gierke
 * @since 1.6
 */
public class Jackson2ResourceReaderIntegrationTests {

	@Test
	public void readsFileWithMultipleObjects() throws Exception {

		ResourceReader reader = new Jackson2ResourceReader();
		Object result = reader.readFrom(new ClassPathResource("data.json", getClass()), null);

		assertThat(result).isInstanceOf(Collection.class);
		assertThat((Collection<?>) result).hasSize(1);
	}
}
