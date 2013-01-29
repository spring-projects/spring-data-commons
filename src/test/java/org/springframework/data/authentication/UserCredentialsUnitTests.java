/*
 * Copyright 2011 by the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.authentication;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link UserCredentials}.
 * 
 * @author Oliver Gierke
 */
public class UserCredentialsUnitTests {

	@Test
	public void treatsEmptyStringAsNull() {

		UserCredentials credentials = new UserCredentials("", "");
		assertThat(credentials.getUsername(), is(nullValue()));
		assertThat(credentials.hasUsername(), is(false));
		assertThat(credentials.getPassword(), is(nullValue()));
		assertThat(credentials.hasPassword(), is(false));
	}

	/**
	 * @see DATACMNS-142
	 */
	@Test
	public void noCredentialsNullsUsernameAndPassword() {

		assertThat(UserCredentials.NO_CREDENTIALS.getUsername(), is(nullValue()));
		assertThat(UserCredentials.NO_CREDENTIALS.getPassword(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-142
	 */
	@Test
	public void configuresUsernameCorrectly() {

		UserCredentials credentials = new UserCredentials("username", null);

		assertThat(credentials.hasUsername(), is(true));
		assertThat(credentials.getUsername(), is("username"));
		assertThat(credentials.hasPassword(), is(false));
		assertThat(credentials.getPassword(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-142
	 */
	@Test
	public void configuresPasswordCorrectly() {

		UserCredentials credentials = new UserCredentials(null, "password");

		assertThat(credentials.hasUsername(), is(false));
		assertThat(credentials.getUsername(), is(nullValue()));
		assertThat(credentials.hasPassword(), is(true));
		assertThat(credentials.getPassword(), is("password"));
	}

	/**
	 * @see DATACMNS-275
	 */

	@Test
	public void returnsNullForNotSetObfuscatedPassword() {
		assertThat(new UserCredentials(null, null).getObfuscatedPassword(), is(nullValue()));
	}

	/**
	 * @see DATACMNS-275
	 */
	@Test
	public void obfuscatesShortPasswordsEntirely() {

		assertThat(new UserCredentials(null, "sa").getObfuscatedPassword(), is("**"));
		assertThat(new UserCredentials(null, "s").getObfuscatedPassword(), is("*"));
	}

	/**
	 * @see DATACMNS-275
	 */
	@Test
	public void returnsObfuscatedPasswordCorrectly() {
		assertThat(new UserCredentials(null, "password").getObfuscatedPassword(), is("p******d"));
	}

	/**
	 * @see DATACMNS-275
	 */
	@Test
	public void toStringDoesNotExposePlainPassword() {

		UserCredentials credentials = new UserCredentials(null, "mypassword");
		assertThat(credentials.toString(), not(containsString(credentials.getPassword())));
		assertThat(credentials.toString(), containsString(credentials.getObfuscatedPassword()));
	}
}
