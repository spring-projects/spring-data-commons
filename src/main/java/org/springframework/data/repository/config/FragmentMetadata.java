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
package org.springframework.data.repository.config;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.repository.JpaRepositoryCombination;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;


/**
 * Value object for a discovered Repository fragment interface.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author CaoMingjie
 * @since 2.1
 */
public class FragmentMetadata {

	private final MetadataReaderFactory factory;

	public FragmentMetadata(MetadataReaderFactory factory) {
		this.factory = factory;
	}

	/**
	 * Returns all interfaces to be considered fragment ones for the given source interface.
	 *
	 * @param interfaceName must not be {@literal null} or empty.
	 * @return
	 */
	public Stream<String> getFragmentInterfaces(String interfaceName) {

		Assert.hasText(interfaceName, "Interface name must not be null or empty!");
		String[] interfaceNames = getClassMetadata(interfaceName).getInterfaceNames();
		interfaceNames = jpaRepositoryCombinationProcess(interfaceNames);
		return Arrays.stream(interfaceNames) //
				.filter(this::isCandidate);
	}

	/**
	 * Handle @JpaRepositoryCombination annotated interfaces
	 * @param interfaceNames
	 * @return
	 */
	private String[] jpaRepositoryCombinationProcess(String[] interfaceNames) {
		if(interfaceNames!=null&&interfaceNames.length>0){
			List<String> list = (List<String>)CollectionUtils.arrayToList(interfaceNames);
			HashSet<String> set = new HashSet<String>(list);
			Set<Class> combinationInterface = getCombinationInterface(list);
			for(Class ci : combinationInterface){
				Class[] interfaces = ci.getInterfaces();
				for(Class interf :  interfaces){
					set.add(interf.getName());
				}
			}
			//remove all @JpaRepositoryCombination interfaces
			for(Class ci: combinationInterface){
				set.remove(ci.getName());
			}
			interfaceNames = set.toArray(new String[set.size()]);
		}
		return interfaceNames;
	}

	/**
	 * get all combination interfaces
	 * @param list
	 * @return
	 */
	private Set<Class> getCombinationInterface(List<String> list) {
		Set<Class> allSuperClass = new HashSet();
		for(String clazzName : list ){
			try {
				allSuperClass.addAll(getAllSuperclass(Class.forName(clazzName)));
				allSuperClass = allSuperClass.stream().filter(c -> c.getAnnotation(JpaRepositoryCombination.class) != null).collect(Collectors.toSet());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return allSuperClass;
	}

	/**
	 * Iterates up to find all parent classes
	 * @param clazz
	 * @return
	 */
	public Set<Class> getAllSuperclass(Class<?> clazz){
		HashSet<Class> classes = new HashSet<>();
		if(!clazz.isInterface()){
			return classes;
		}else if(clazz.getInterfaces().length>0){
			classes.add(clazz);
			classes.addAll((List)CollectionUtils.arrayToList(clazz.getInterfaces()));
			for(Class c : clazz.getInterfaces()){
				classes.addAll(getAllSuperclass(c));
			}
			return classes;
		}else {
			classes.add(clazz);
			return classes;
		}
	}

	/**
	 * Returns whether the given interface is a fragment candidate.
	 *
	 * @param interfaceName must not be {@literal null} or empty.
	 * @return
	 */
	private boolean isCandidate(String interfaceName) {

		Assert.hasText(interfaceName, "Interface name must not be null or empty!");

		AnnotationMetadata metadata = getAnnotationMetadata(interfaceName);
		return !metadata.hasAnnotation(NoRepositoryBean.class.getName());

	}

	private AnnotationMetadata getAnnotationMetadata(String className) {

		try {
			return factory.getMetadataReader(className).getAnnotationMetadata();
		} catch (IOException e) {
			throw new BeanDefinitionStoreException(String.format("Cannot parse %s metadata.", className), e);
		}
	}

	private ClassMetadata getClassMetadata(String className) {

		try {
			return factory.getMetadataReader(className).getClassMetadata();
		} catch (IOException e) {
			throw new BeanDefinitionStoreException(String.format("Cannot parse %s metadata.", className), e);
		}
	}
}
