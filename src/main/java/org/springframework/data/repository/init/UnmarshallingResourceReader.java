/*
 * Copyright 2012-2018 the original author or authors.
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

import java.io.IOException;

import javax.xml.transform.stream.StreamSource;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
public class UnmarshallingResourceReader implements ResourceReader {

	private final Unmarshaller unmarshaller;

	/**
	 * @param unmarshaller
	 */
	public UnmarshallingResourceReader(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.init.ResourceReader#readFrom(org.springframework.core.io.Resource, java.lang.ClassLoader)
	 */
	public Object readFrom(Resource resource, @Nullable ClassLoader classLoader) throws IOException {

		Assert.notNull(resource, "Resource must not be null!");

		StreamSource source = new StreamSource(resource.getInputStream());
		return unmarshaller.unmarshal(source);
	}
}
