/*
 * Copyright (c) 2011 by the original author(s).
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

/**
 * Class used to provide credentials for username/password authentication
 *
 * @author Thomas Risberg
 */
public class UserCredentials {

    private String username;

    private String password;

    public UserCredentials() {
    }

    public UserCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Get the username to use for authentication
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the username to use for authentication
     *
     * @param username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get the password to use for authentication
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password to use for authentication
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
