/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.querydsl;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import com.querydsl.core.annotations.QueryEntity;

/**
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 */
@QueryEntity
public class User {

	public String firstname, lastname;
	public @DateTimeFormat(iso = ISO.DATE) Date dateOfBirth;
	public Address address;
	public List<Address> addresses;
	public List<String> nickNames;
	public Long inceptionYear;

	public User(String firstname, String lastname, Address address) {

		this.firstname = firstname;
		this.lastname = lastname;
		this.address = address;
	}
}

@QueryEntity
class SpecialUser extends User {

	public String specialProperty;

	public SpecialUser(String firstname, String lastname, Address address) {
		super(firstname, lastname, address);
	}
}

@QueryEntity
class UserWrapper {
	public User user;
}
