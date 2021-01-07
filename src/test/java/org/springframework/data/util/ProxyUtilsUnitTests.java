/*
 * Copyright 2018-2021 the original author or authors.
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
package org.springframework.data.util;

import static org.assertj.core.api.Assertions.*;

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.util.ProxyUtils.ProxyDetector;

/**
 * Unit tests for {@link ProxyUtils}.
 *
 * @author Oliver Gierke
 * @soundtrack Victor Wooten - The 13th Floor (Trypnotix)
 */
public class ProxyUtilsUnitTests {

	@Test // DATACMNS-1324
	public void detectsStandardProxy() {

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(new Sample());
		Object proxy = factory.getProxy();

		assertThat(proxy.getClass()).isNotEqualTo(Sample.class);
		assertThat(ProxyUtils.getUserClass(proxy)).isEqualTo(Sample.class);
	}

	@Test // DATACMNS-1324
	public void usesCustomProxyDetector() {

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(new AnotherSample());
		Object proxy = factory.getProxy();

		assertThat(ProxyUtils.getUserClass(proxy)).isEqualTo(UserType.class);
	}

	@Test // DATACMNS-1341
	public void detectsTargetTypeOfJdkProxy() {

		ProxyFactory factory = new ProxyFactory();
		factory.setTarget(new SomeTypeWithInterface());
		factory.setInterfaces(Serializable.class);
		Object proxy = factory.getProxy();

		assertThat(ProxyUtils.getUserClass(proxy)).isEqualTo(SomeTypeWithInterface.class);
	}

	static class SampleProxyDetector implements ProxyDetector {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.util.ProxyUtils.ProxyDetector#getUserType(java.lang.Class)
		 */
		@Override
		public Class<?> getUserType(Class<?> type) {
			return AnotherSample.class.isAssignableFrom(type) ? UserType.class : type;
		}
	}

	static class Sample {}

	static class UserType {}

	static class AnotherSample {}

	static class SomeTypeWithInterface implements Serializable {}
}
