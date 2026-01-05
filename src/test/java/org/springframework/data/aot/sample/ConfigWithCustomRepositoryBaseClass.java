/*
 * Copyright 2021-present the original author or authors.
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
package org.springframework.data.aot.sample;

import java.util.Optional;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.aot.sample.ConfigWithCustomRepositoryBaseClass.RepoBaseClass;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.config.EnableRepositoriesWithContributor;

/**
 * @author Christoph Strobl
 */
@Configuration
@EnableRepositoriesWithContributor(repositoryBaseClass = RepoBaseClass.class, considerNestedRepositories = true,
		includeFilters = { @Filter(type = FilterType.REGEX, pattern = ".*CustomerRepositoryWithCustomBaseRepo$") })
public class ConfigWithCustomRepositoryBaseClass {

	public interface CustomerRepositoryWithCustomBaseRepo extends CrudRepository<Person, String> {

	}

	public static class RepoBaseClass<T, ID> implements CrudRepository<T, ID> {

		private CrudRepository<T, ID> delegate;

		public <S extends T> S save(S entity) {
			return this.delegate.save(entity);
		}

		@Override
		public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
			return this.delegate.saveAll(entities);
		}

		public Optional<T> findById(ID id) {
			return this.delegate.findById(id);
		}

		@Override
		public boolean existsById(ID id) {
			return this.delegate.existsById(id);
		}

		@Override
		public Iterable<T> findAll() {
			return this.delegate.findAll();
		}

		@Override
		public Iterable<T> findAllById(Iterable<ID> ids) {
			return this.delegate.findAllById(ids);
		}

		@Override
		public long count() {
			return this.delegate.count();
		}

		@Override
		public void deleteById(ID id) {
			this.delegate.deleteById(id);
		}

		@Override
		public void delete(T entity) {
			this.delegate.delete(entity);
		}

		@Override
		public void deleteAllById(Iterable<? extends ID> ids) {
			this.delegate.deleteAllById(ids);
		}

		@Override
		public void deleteAll(Iterable<? extends T> entities) {
			this.delegate.deleteAll(entities);
		}

		@Override
		public void deleteAll() {
			this.delegate.deleteAll();
		}
	}

	public static class Person {

		Address address;

	}

	public static class Address {
		String street;
	}
}
