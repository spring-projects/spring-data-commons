/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Copy of Spring Frameworks {@link org.springframework.instrument.classloading.ShadowingClassLoader} tweaked to not
 * register default exclusions. ClassLoader decorator that shadows an enclosing ClassLoader, applying registered
 * transformers to all affected classes.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author Jens Schauder
 * @author Oliver Gierke
 * @since 2.0
 * @see #addTransformer
 * @see org.springframework.core.OverridingClassLoader
 * @see https://jira.spring.io/browse/SPR-15439
 */
public class ShadowingClassLoader extends DecoratingClassLoader {

	private final ClassLoader enclosingClassLoader;

	private final List<ClassFileTransformer> classFileTransformers = new LinkedList<ClassFileTransformer>();

	private final Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();

	/**
	 * Create a new ShadowingClassLoader, decorating the given ClassLoader.
	 * 
	 * @param enclosingClassLoader the ClassLoader to decorate
	 */
	public ShadowingClassLoader(ClassLoader enclosingClassLoader) {
		Assert.notNull(enclosingClassLoader, "Enclosing ClassLoader must not be null");
		this.enclosingClassLoader = enclosingClassLoader;
	}

	/**
	 * Add the given ClassFileTransformer to the list of transformers that this ClassLoader will apply.
	 * 
	 * @param transformer the ClassFileTransformer
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		this.classFileTransformers.add(transformer);
	}

	/**
	 * Copy all ClassFileTransformers from the given ClassLoader to the list of transformers that this ClassLoader will
	 * apply.
	 * 
	 * @param other the ClassLoader to copy from
	 */
	public void copyTransformers(ShadowingClassLoader other) {
		Assert.notNull(other, "Other ClassLoader must not be null");
		this.classFileTransformers.addAll(other.classFileTransformers);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (shouldShadow(name)) {
			System.out.println("shadowing " + name);
			Class<?> cls = this.classCache.get(name);
			if (cls != null) {
				return cls;
			}
			return doLoadClass(name);
		} else {
			return this.enclosingClassLoader.loadClass(name);
		}
	}

	/**
	 * Determine whether the given class should be excluded from shadowing.
	 * 
	 * @param className the name of the class
	 * @return whether the specified class should be shadowed
	 */
	private boolean shouldShadow(String className) {
		return (!className.equals(getClass().getName()) && !className.endsWith("ShadowingClassLoader")
				&& isEligibleForShadowing(className));
	}

	/**
	 * Determine whether the specified class is eligible for shadowing by this class loader.
	 * 
	 * @param className the class name to check
	 * @return whether the specified class is eligible
	 * @see #isExcluded
	 */
	protected boolean isEligibleForShadowing(String className) {
		return isExcluded(className);
	}

	private Class<?> doLoadClass(String name) throws ClassNotFoundException {
		String internalName = StringUtils.replace(name, ".", "/") + ".class";
		InputStream is = this.enclosingClassLoader.getResourceAsStream(internalName);
		if (is == null) {
			throw new ClassNotFoundException(name);
		}
		try {
			byte[] bytes = FileCopyUtils.copyToByteArray(is);
			bytes = applyTransformers(name, bytes);
			Class<?> cls = defineClass(name, bytes, 0, bytes.length);
			// Additional check for defining the package, if not defined yet.
			if (cls.getPackage() == null) {
				int packageSeparator = name.lastIndexOf('.');
				if (packageSeparator != -1) {
					String packageName = name.substring(0, packageSeparator);
					definePackage(packageName, null, null, null, null, null, null, null);
				}
			}
			this.classCache.put(name, cls);
			return cls;
		} catch (IOException ex) {
			throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
		}
	}

	private byte[] applyTransformers(String name, byte[] bytes) {
		String internalName = StringUtils.replace(name, ".", "/");
		try {
			for (ClassFileTransformer transformer : this.classFileTransformers) {
				byte[] transformed = transformer.transform(this, internalName, null, null, bytes);
				bytes = (transformed != null ? transformed : bytes);
			}
			return bytes;
		} catch (IllegalClassFormatException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public URL getResource(String name) {
		return this.enclosingClassLoader.getResource(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return this.enclosingClassLoader.getResourceAsStream(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return this.enclosingClassLoader.getResources(name);
	}
}
