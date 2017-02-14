/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.data.classloadersupport;

import java.net.URLClassLoader;
import java.util.Collection;

/**
 *  is intended for testing code that depends on the presence/absence of certain classes.
 *
 *  Classes can be:
 *  <ul>
 *      <li>shadowed: reloaded by this classloader no matter if they are loaded already by the SystemClassLoader</li>
 *      <li>hidden: not loaded by this classloader no matter if they are loaded already by the SystemClassLoader. Trying to load these classes results in a {@link ClassNotFoundException}</li>
 *      <li>all other classes get loaded by the SystemClassLoader</li>
 *  </ul>
 *
 * @author Jens Schauder
 */
public class HidingClassLoader extends ShadowingClassLoader {

	private final Collection<String> hidden;

	HidingClassLoader(Collection<String> hidden) {
		super(URLClassLoader.getSystemClassLoader());
		this.hidden = hidden;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {

		checkIfHidden(name);
		return super.loadClass(name);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {

		checkIfHidden(name);
		return super.findClass(name);
	}

	private void checkIfHidden(String name) throws ClassNotFoundException {

		for (String prefix : hidden) {
			if (name.startsWith(prefix)) {
				throw new ClassNotFoundException();
			}
		}
	}
}
