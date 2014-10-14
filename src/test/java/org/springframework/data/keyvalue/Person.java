/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.keyvalue;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.util.ObjectUtils;

import com.mysema.query.annotations.QueryEntity;

/**
 * @author Christoph Strobl
 */
@QueryEntity
public class Person implements Serializable {

	private @Id String id;
	private String firstname;
	private int age;

	public Person(String firstname, int age) {
		super();
		this.firstname = firstname;
		this.age = age;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	@Override
	public String toString() {
		return "Person [id=" + id + ", firstname=" + firstname + ", age=" + age + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + age;
		result = prime * result + ObjectUtils.nullSafeHashCode(this.firstname);
		result = prime * result + ObjectUtils.nullSafeHashCode(this.id);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Person)) {
			return false;
		}
		Person other = (Person) obj;
		if (!ObjectUtils.nullSafeEquals(this.id, other.id)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(this.firstname, other.firstname)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(this.age, other.age)) {
			return false;
		}
		return true;
	}

}
