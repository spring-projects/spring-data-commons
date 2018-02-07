/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.data.repository.config;

import lombok.Value;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Value object for a discovered Repository fragment interface.
 * 
 * @author Mark Paluch
 * @since 2.1
 */
@Value(staticConstructor = "of")
public class FragmentMetadata {

	private String fragmentInterfaceName;
	private RepositoryFragmentDiscovery configuration;

	/**
	 * Returns whether the given interface is a fragment candidate.
	 *
	 * @param interfaceName must not be {@literal null} or empty.
	 * @param factory must not be {@literal null}.
	 * @return
	 */
	public static boolean isCandidate(String interfaceName, MetadataReaderFactory factory) {

		Assert.hasText(interfaceName, "Interface name must not be null or empty!");
		Assert.notNull(factory, "MetadataReaderFactory must not be null!");

		AnnotationMetadata metadata = getAnnotationMetadata(interfaceName, factory);

		return !metadata.hasAnnotation(NoRepositoryBean.class.getName());
	}

	/**
	 * Returns the exclusions to be used when scanning for fragment implementations.
	 *
	 * @return
	 */
	public List<TypeFilter> getExclusions() {

		Stream<TypeFilter> configurationExcludes = configuration.getExcludeFilters().stream();
		Stream<AnnotationTypeFilter> noRepositoryBeans = Stream.of(new AnnotationTypeFilter(NoRepositoryBean.class));

		return Stream.concat(configurationExcludes, noRepositoryBeans).collect(StreamUtils.toUnmodifiableList());
	}

	/**
	 * Returns the name of the implementation class to be detected for the fragment interface.
	 *
	 * @return
	 */
	public String getFragmentImplementationClassName() {

		String postfix = configuration.getRepositoryImplementationPostfix().orElse("Impl");

		return ClassUtils.getShortName(fragmentInterfaceName).concat(postfix);
	}

	/**
	 * Returns the base packages to be scanned to find implementations of the current fragment interface.
	 *
	 * @return
	 */
	public Iterable<String> getBasePackages() {
		return Collections.singleton(ClassUtils.getPackageName(fragmentInterfaceName));
	}

	private static AnnotationMetadata getAnnotationMetadata(String className,
			MetadataReaderFactory metadataReaderFactory) {

		try {
			return metadataReaderFactory.getMetadataReader(className).getAnnotationMetadata();
		} catch (IOException e) {
			throw new BeanDefinitionStoreException(String.format("Cannot parse %s metadata.", className), e);
		}
	}
}
