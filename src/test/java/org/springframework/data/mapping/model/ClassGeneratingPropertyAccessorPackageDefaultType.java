/*
 * Copyright 2016-2021 the original author or authors.
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
package org.springframework.data.mapping.model;

import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;

/**
 * @author Mark Paluch
 * @author Oliver Gierke
 */
@SuppressWarnings("unused")
class ClassGeneratingPropertyAccessorPackageDefaultType {

	private String privateField;
	String packageDefaultField;
	protected String protectedField;
	public String publicField;
	private String backing;
	private final String immutable = "";
	private final String wither;

	@AccessType(Type.PROPERTY) private String privateProperty;

	@AccessType(Type.PROPERTY) private String packageDefaultProperty;

	@AccessType(Type.PROPERTY) private String protectedProperty;

	@AccessType(Type.PROPERTY) private String publicProperty;

	public ClassGeneratingPropertyAccessorPackageDefaultType() {
		this.wither = "";
	}

	private ClassGeneratingPropertyAccessorPackageDefaultType(String wither) {
		this.wither = wither;
	}

	private String getPrivateProperty() {
		return privateProperty;
	}

	private void setPrivateProperty(String privateProperty) {
		this.privateProperty = privateProperty;
	}

	String getPackageDefaultProperty() {
		return packageDefaultProperty;
	}

	void setPackageDefaultProperty(String packageDefaultProperty) {
		this.packageDefaultProperty = packageDefaultProperty;
	}

	protected String getProtectedProperty() {
		return protectedProperty;
	}

	protected void setProtectedProperty(String protectedProperty) {
		this.protectedProperty = protectedProperty;
	}

	public String getPublicProperty() {
		return publicProperty;
	}

	public void setPublicProperty(String publicProperty) {
		this.publicProperty = publicProperty;
	}

	@AccessType(Type.PROPERTY)
	public String getSyntheticProperty() {
		return backing;
	}

	public void setSyntheticProperty(String syntheticProperty) {
		backing = syntheticProperty;
	}

	public String getWither() {
		return wither;
	}

	public ClassGeneratingPropertyAccessorPackageDefaultType withWither(String wither) {
		return new ClassGeneratingPropertyAccessorPackageDefaultType(wither);
	}
}
