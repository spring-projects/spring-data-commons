/*
 * Copyright 2016 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import lombok.Getter;

import java.util.Collection;

import org.junit.Test;
import org.springframework.data.util.MethodInvocationRecorder.Recorded;

/**
 * Unit tests for {@link MethodInvocationRecorder}.
 * 
 * @author Oliver Gierke
 */
public class MethodInvocationRecorderUnitTests {

	Recorded<Foo> recorder = MethodInvocationRecorder.forProxyOf(Foo.class);

	@Test
	public void createsPropertyPathForSimpleMethodReference() {

		Recorded<Bar> wrapper = recorder.record(Foo::getBar);

		assertThat(wrapper.getPropertyPath(), is("bar"));
	}

	@Test
	public void createsPropertyPathForNestedMethodReference() {

		Recorded<FooBar> wrapper = recorder.record(Foo::getBar).record(Bar::getFooBar);

		assertThat(wrapper.getPropertyPath(), is("bar.fooBar"));
	}

	@Test
	public void createsPropertyPathForNestedCall() {

		Recorded<FooBar> wrapper = recorder.record((Foo source) -> source.getBar().getFooBar());

		assertThat(wrapper.getPropertyPath(), is("bar.fooBar"));
	}

	@Test
	public void usesCustomPropertyNamingStrategy() {

		Recorded<Bar> recorded = MethodInvocationRecorder.forProxyOf(Foo.class).record(Foo::getBar);

		assertThat(recorded.getPropertyPath(method -> method.getName()), is("getBar"));
	}

	@Getter
	static class Foo {
		Bar bar;
		Collection<Bar> bars;
	}

	@Getter
	static class Bar {
		FooBar fooBar;
	}

	static class FooBar {}
}
