/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.data.mapping.model;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.data.mapping.context.SamplePersistentProperty;
import org.springframework.data.mapping.model.subpackage.TypeInOtherPackage;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ClassGeneratingPropertyAccessorFactory}
 *
 * @author Mark Paluch
 * @see DATACMNS-809
 */
@RunWith(Parameterized.class)
public class ClassGeneratingPropertyAccessorFactoryTests {

	private final ClassGeneratingPropertyAccessorFactory factory = new ClassGeneratingPropertyAccessorFactory();
	private final SampleMappingContext mappingContext = new SampleMappingContext();

	private final Object bean;
	private final String propertyName;
	private final Class<?> expectedConstructorType;

	public ClassGeneratingPropertyAccessorFactoryTests(Object bean, String propertyName, Class<?> expectedConstructorType,
			String displayName) {

		this.bean = bean;
		this.propertyName = propertyName;
		this.expectedConstructorType = expectedConstructorType;
	}

	@Parameters(name = "{3}")
	public static List<Object[]> parameters() {

		List<Object[]> parameters = new ArrayList<Object[]>();
		List<String> propertyNames = Arrays.asList("privateField", "packageDefaultField", "protectedField", "publicField",
				"privateProperty", "packageDefaultProperty", "protectedProperty", "publicProperty", "syntheticProperty");

		parameters.addAll(parameters(new InnerPrivateType(), propertyNames, Object.class));
		parameters.addAll(parameters(new InnerTypeWithPrivateAncestor(), propertyNames, InnerTypeWithPrivateAncestor.class));
		parameters.addAll(parameters(new InnerPackageDefaultType(), propertyNames, InnerPackageDefaultType.class));
		parameters.addAll(parameters(new InnerProtectedType(), propertyNames, InnerProtectedType.class));
		parameters.addAll(parameters(new InnerPublicType(), propertyNames, InnerPublicType.class));
		parameters.addAll(parameters(new ClassGeneratingPropertyAccessorPackageDefaultType(), propertyNames,
				ClassGeneratingPropertyAccessorPackageDefaultType.class));
		parameters.addAll(parameters(new ClassGeneratingPropertyAccessorPublicType(), propertyNames,
				ClassGeneratingPropertyAccessorPublicType.class));
		parameters.addAll(parameters(new SubtypeOfTypeInOtherPackage(), propertyNames, SubtypeOfTypeInOtherPackage.class));

		return parameters;
	}

	private static List<Object[]> parameters(Object bean, List<String> propertyNames, Class<?> expectedConstructorType) {

		List<Object[]> parameters = new ArrayList<Object[]>();

		for (String propertyName : propertyNames) {
			parameters.add(new Object[] { bean, propertyName, expectedConstructorType,
					bean.getClass().getSimpleName() + "/" + propertyName });
		}

		return parameters;
	}

	/**
	 * @see DATACMNS-809
	 * @throws Exception
	 */
	@Test
	public void shouldSetAndGetProperty() throws Exception {

		assertThat(getProperty(bean, propertyName)).hasValueSatisfying(property -> {

			PersistentPropertyAccessor persistentPropertyAccessor = getPersistentPropertyAccessor(bean);

			persistentPropertyAccessor.setProperty(property, Optional.of("value"));
			assertThat(persistentPropertyAccessor.getProperty(property)).isEqualTo("value");
		});
	}

	/**
	 * @see DATACMNS-809
	 * @throws Exception
	 */
	@Test
	public void accessorShouldDeclareConstructor() throws Exception {

		PersistentPropertyAccessor persistentPropertyAccessor = getPersistentPropertyAccessor(bean);

		Constructor<?>[] declaredConstructors = persistentPropertyAccessor.getClass().getDeclaredConstructors();
		assertThat(declaredConstructors.length).isEqualTo(1);
		assertThat(declaredConstructors[0].getParameterTypes().length).isEqualTo(1);
		assertThat(declaredConstructors[0].getParameterTypes()[0]).isEqualTo(expectedConstructorType);
	}

	/**
	 * @see DATACMNS-809
	 */
	@Test(expected = IllegalArgumentException.class)
	public void shouldFailOnNullBean() {
		factory.getPropertyAccessor(mappingContext.getPersistentEntity(bean.getClass()), null);
	}

	/**
	 * @see DATACMNS-809
	 */
	@Test
	public void getPropertyShouldFailOnUnhandledProperty() {

		assertThat(getProperty(new Dummy(), "dummy")).hasValueSatisfying(property -> {

			assertThatExceptionOfType(UnsupportedOperationException.class)//
					.isThrownBy(() -> getPersistentPropertyAccessor(bean).getProperty(property));
		});
	}

	/**
	 * @see DATACMNS-809
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void setPropertyShouldFailOnUnhandledProperty() {

		assertThat(getProperty(new Dummy(), "dummy")).hasValueSatisfying(property -> {
			getPersistentPropertyAccessor(bean).setProperty(property, Optional.empty());
		});

	}

	/**
	 * @see DATACMNS-809
	 */
	@Test
	public void shouldUseClassPropertyAccessorFactory() throws Exception {

		BasicPersistentEntity<Object, SamplePersistentProperty> persistentEntity = mappingContext
				.getPersistentEntity(bean.getClass());

		assertThat(ReflectionTestUtils.getField(persistentEntity, "propertyAccessorFactory"))
				.isInstanceOf(ClassGeneratingPropertyAccessorFactory.class);
	}

	private PersistentPropertyAccessor getPersistentPropertyAccessor(Object bean) {
		return factory.getPropertyAccessor(mappingContext.getPersistentEntity(bean.getClass()), bean);
	}

	private Optional<? extends PersistentProperty<?>> getProperty(Object bean, String name) {

		BasicPersistentEntity<Object, SamplePersistentProperty> persistentEntity = mappingContext
				.getPersistentEntity(bean.getClass());
		return persistentEntity.getPersistentProperty(name);
	}

	/**
	 * @see DATACMNS-809
	 */
	@SuppressWarnings("unused")
	private static class InnerPrivateType {

		private String privateField;
		String packageDefaultField;
		protected String protectedField;
		public String publicField;
		private String backing;

		@AccessType(Type.PROPERTY) private String privateProperty;

		@AccessType(Type.PROPERTY) private String packageDefaultProperty;

		@AccessType(Type.PROPERTY) private String protectedProperty;

		@AccessType(Type.PROPERTY) private String publicProperty;

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
	}

	/**
	 * @see DATACMNS-809
	 */
	public static class InnerTypeWithPrivateAncestor extends InnerPrivateType {

	}

	/**
	 * @see DATACMNS-809
	 */
	@SuppressWarnings("unused")
	static class InnerPackageDefaultType {

		private String privateField;
		String packageDefaultField;
		protected String protectedField;
		public String publicField;
		private String backing;

		@AccessType(Type.PROPERTY) private String privateProperty;

		@AccessType(Type.PROPERTY) private String packageDefaultProperty;

		@AccessType(Type.PROPERTY) private String protectedProperty;

		@AccessType(Type.PROPERTY) private String publicProperty;

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
	}

	/**
	 * @see DATACMNS-809
	 */
	@SuppressWarnings("unused")
	protected static class InnerProtectedType {

		private String privateField;
		String packageDefaultField;
		protected String protectedField;
		public String publicField;
		private String backing;

		@AccessType(Type.PROPERTY) private String privateProperty;

		@AccessType(Type.PROPERTY) private String packageDefaultProperty;

		@AccessType(Type.PROPERTY) private String protectedProperty;

		@AccessType(Type.PROPERTY) private String publicProperty;

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
	}

	/**
	 * @see DATACMNS-809
	 */
	@SuppressWarnings("unused")
	public static class InnerPublicType {

		private String privateField;
		String packageDefaultField;
		protected String protectedField;
		public String publicField;
		private String backing;

		@AccessType(Type.PROPERTY) private String privateProperty;

		@AccessType(Type.PROPERTY) private String packageDefaultProperty;

		@AccessType(Type.PROPERTY) private String protectedProperty;

		@AccessType(Type.PROPERTY) private String publicProperty;

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
	}

	public static class SubtypeOfTypeInOtherPackage extends TypeInOtherPackage {}

	/**
	 * @see DATACMNS-809
	 */
	@SuppressWarnings("unused")
	private static class Dummy {

		private String dummy;
		public String publicField;
	}
}
