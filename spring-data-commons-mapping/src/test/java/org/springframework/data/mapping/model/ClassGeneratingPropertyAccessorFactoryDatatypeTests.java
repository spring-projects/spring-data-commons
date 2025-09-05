/*
 * Copyright 2016-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.AccessType.Type;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.SampleMappingContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ClassGeneratingPropertyAccessorFactory}
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 */
@SuppressWarnings("WeakerAccess") // public required for class generation due to visibility rules
public class ClassGeneratingPropertyAccessorFactoryDatatypeTests {

	private final ClassGeneratingPropertyAccessorFactory factory = new ClassGeneratingPropertyAccessorFactory();
	private final SampleMappingContext mappingContext = new SampleMappingContext();

	static List<Object[]> parameters() throws Exception {

		List<Object[]> parameters = new ArrayList<>();
		var types = Arrays.asList(FieldAccess.class, PropertyAccess.class, PrivateFinalFieldAccess.class,
				PrivateFinalPropertyAccess.class);

		parameters.addAll(create(types, "primitiveInteger", Integer.valueOf(1)));
		parameters.addAll(create(types, "primitiveIntegerArray", new int[] { 1, 2, 3 }));
		parameters.addAll(create(types, "boxedInteger", Integer.valueOf(1)));
		parameters.addAll(create(types, "boxedIntegerArray", new Integer[] { Integer.valueOf(1) }));
		parameters.addAll(create(types, "primitiveShort", Short.valueOf("1")));
		parameters.addAll(create(types, "primitiveShortArray", new short[] { 1, 2, 3 }));
		parameters.addAll(create(types, "boxedShort", Short.valueOf("1")));
		parameters.addAll(create(types, "boxedShortArray", new Short[] { Short.valueOf("1") }));
		parameters.addAll(create(types, "primitiveByte", Byte.valueOf("1")));
		parameters.addAll(create(types, "primitiveByteArray", new byte[] { 1, 2, 3 }));
		parameters.addAll(create(types, "boxedByte", Byte.valueOf("1")));
		parameters.addAll(create(types, "boxedByteArray", new Byte[] { Byte.valueOf("1") }));
		parameters.addAll(create(types, "primitiveChar", Character.valueOf('c')));
		parameters.addAll(create(types, "primitiveCharArray", new char[] { 'a', 'b', 'c' }));
		parameters.addAll(create(types, "boxedChar", Character.valueOf('c')));
		parameters.addAll(create(types, "boxedCharArray", new Character[] { Character.valueOf('c') }));
		parameters.addAll(create(types, "primitiveBoolean", Boolean.valueOf(true)));
		parameters.addAll(create(types, "primitiveBooleanArray", new boolean[] { true, false }));
		parameters.addAll(create(types, "boxedBoolean", Boolean.valueOf(true)));
		parameters.addAll(create(types, "boxedBooleanArray", new Boolean[] { Boolean.valueOf(true) }));
		parameters.addAll(create(types, "primitiveFloat", Float.valueOf(1.0f)));
		parameters.addAll(create(types, "primitiveFloatArray", new float[] { 1.0f, 2f }));
		parameters.addAll(create(types, "boxedFloat", Float.valueOf(1.0f)));
		parameters.addAll(create(types, "boxedFloatArray", new Float[] { Float.valueOf(1.0f) }));
		parameters.addAll(create(types, "primitiveDouble", Double.valueOf(1d)));
		parameters.addAll(create(types, "primitiveDoubleArray", new double[] { 1d, 2d }));
		parameters.addAll(create(types, "boxedDouble", Double.valueOf(1d)));
		parameters.addAll(create(types, "boxedDoubleArray", new Double[] { Double.valueOf(1d) }));
		parameters.addAll(create(types, "primitiveLong", Long.valueOf(1L)));
		parameters.addAll(create(types, "primitiveLongArray", new long[] { 1L, 2L }));
		parameters.addAll(create(types, "boxedLong", Long.valueOf(1L)));
		parameters.addAll(create(types, "boxedLongArray", new Long[] { Long.valueOf(1L) }));
		parameters.addAll(create(types, "string", "hello"));
		parameters.addAll(create(types, "stringArray", new String[] { "hello", "world" }));

		return parameters;
	}

	private static List<Object[]> create(List<Class<?>> types, String propertyName, Object value) throws Exception {

		List<Object[]> parameters = new ArrayList<>();

		for (var type : types) {

			var constructors = type.getDeclaredConstructors();
			constructors[0].setAccessible(true);
			parameters.add(new Object[] { constructors[0].newInstance(), propertyName, value,
					type.getSimpleName() + "/" + propertyName });
		}

		return parameters;
	}

	@ParameterizedTest(name = "{3}") // DATACMNS-809
	@MethodSource("parameters")
	void shouldSetAndGetProperty(Object bean, String propertyName, Object value, String displayName) {

		assertThat(getProperty(bean, propertyName)).satisfies(property -> {

			var persistentPropertyAccessor = getPersistentPropertyAccessor(bean);

			persistentPropertyAccessor.setProperty(property, value);
			assertThat(persistentPropertyAccessor.getProperty(property)).isEqualTo(value);
		});
	}

	@ParameterizedTest(name = "{3}") // DATACMNS-809
	@MethodSource("parameters")
	void shouldUseClassPropertyAccessorFactory(Object bean, String propertyName, Object value, String displayName)
			throws Exception {

		var persistentEntity = mappingContext.getRequiredPersistentEntity(bean.getClass());

		assertThat(ReflectionTestUtils.getField(persistentEntity, "propertyAccessorFactory"))
				.isInstanceOfSatisfying(InstantiationAwarePropertyAccessorFactory.class, it -> {
					assertThat(ReflectionTestUtils.getField(it, "delegate"))
							.isInstanceOf(ClassGeneratingPropertyAccessorFactory.class);
				});
	}

	private PersistentPropertyAccessor getPersistentPropertyAccessor(Object bean) {
		return factory.getPropertyAccessor(mappingContext.getRequiredPersistentEntity(bean.getClass()), bean);
	}

	private PersistentProperty<?> getProperty(Object bean, String name) {

		var persistentEntity = mappingContext.getRequiredPersistentEntity(bean.getClass());
		return persistentEntity.getPersistentProperty(name);
	}

	// DATACMNS-809
	@AccessType(Type.FIELD)
	public static class FieldAccess {

		int primitiveInteger;
		int[] primitiveIntegerArray;
		Integer boxedInteger;
		Integer[] boxedIntegerArray;

		short primitiveShort;
		short[] primitiveShortArray;
		Short boxedShort;
		Short[] boxedShortArray;

		byte primitiveByte;
		byte[] primitiveByteArray;
		Byte boxedByte;
		Byte[] boxedByteArray;

		char primitiveChar;
		char[] primitiveCharArray;
		Character boxedChar;
		Character[] boxedCharArray;

		boolean primitiveBoolean;
		boolean[] primitiveBooleanArray;
		Boolean boxedBoolean;
		Boolean[] boxedBooleanArray;

		float primitiveFloat;
		float[] primitiveFloatArray;
		Float boxedFloat;
		Float[] boxedFloatArray;

		double primitiveDouble;
		double[] primitiveDoubleArray;
		Double boxedDouble;
		Double[] boxedDoubleArray;

		long primitiveLong;
		long[] primitiveLongArray;
		Long boxedLong;
		Long[] boxedLongArray;

		String string;
		String[] stringArray;
	}

	// DATACMNS-809
	@AccessType(Type.PROPERTY)
	public static class PropertyAccess {

		int primitiveInteger;
		int[] primitiveIntegerArray;
		Integer boxedInteger;
		Integer[] boxedIntegerArray;

		short primitiveShort;
		short[] primitiveShortArray;
		Short boxedShort;
		Short[] boxedShortArray;

		byte primitiveByte;
		byte[] primitiveByteArray;
		Byte boxedByte;
		Byte[] boxedByteArray;

		char primitiveChar;
		char[] primitiveCharArray;
		Character boxedChar;
		Character[] boxedCharArray;

		boolean primitiveBoolean;
		boolean[] primitiveBooleanArray;
		Boolean boxedBoolean;
		Boolean[] boxedBooleanArray;

		float primitiveFloat;
		float[] primitiveFloatArray;
		Float boxedFloat;
		Float[] boxedFloatArray;

		double primitiveDouble;
		double[] primitiveDoubleArray;
		Double boxedDouble;
		Double[] boxedDoubleArray;

		long primitiveLong;
		long[] primitiveLongArray;
		Long boxedLong;
		Long[] boxedLongArray;

		String string;
		String[] stringArray;

		public int getPrimitiveInteger() {
			return this.primitiveInteger;
		}

		public int[] getPrimitiveIntegerArray() {
			return this.primitiveIntegerArray;
		}

		public Integer getBoxedInteger() {
			return this.boxedInteger;
		}

		public Integer[] getBoxedIntegerArray() {
			return this.boxedIntegerArray;
		}

		public short getPrimitiveShort() {
			return this.primitiveShort;
		}

		public short[] getPrimitiveShortArray() {
			return this.primitiveShortArray;
		}

		public Short getBoxedShort() {
			return this.boxedShort;
		}

		public Short[] getBoxedShortArray() {
			return this.boxedShortArray;
		}

		public byte getPrimitiveByte() {
			return this.primitiveByte;
		}

		public byte[] getPrimitiveByteArray() {
			return this.primitiveByteArray;
		}

		public Byte getBoxedByte() {
			return this.boxedByte;
		}

		public Byte[] getBoxedByteArray() {
			return this.boxedByteArray;
		}

		public char getPrimitiveChar() {
			return this.primitiveChar;
		}

		public char[] getPrimitiveCharArray() {
			return this.primitiveCharArray;
		}

		public Character getBoxedChar() {
			return this.boxedChar;
		}

		public Character[] getBoxedCharArray() {
			return this.boxedCharArray;
		}

		public boolean isPrimitiveBoolean() {
			return this.primitiveBoolean;
		}

		public boolean[] getPrimitiveBooleanArray() {
			return this.primitiveBooleanArray;
		}

		public Boolean getBoxedBoolean() {
			return this.boxedBoolean;
		}

		public Boolean[] getBoxedBooleanArray() {
			return this.boxedBooleanArray;
		}

		public float getPrimitiveFloat() {
			return this.primitiveFloat;
		}

		public float[] getPrimitiveFloatArray() {
			return this.primitiveFloatArray;
		}

		public Float getBoxedFloat() {
			return this.boxedFloat;
		}

		public Float[] getBoxedFloatArray() {
			return this.boxedFloatArray;
		}

		public double getPrimitiveDouble() {
			return this.primitiveDouble;
		}

		public double[] getPrimitiveDoubleArray() {
			return this.primitiveDoubleArray;
		}

		public Double getBoxedDouble() {
			return this.boxedDouble;
		}

		public Double[] getBoxedDoubleArray() {
			return this.boxedDoubleArray;
		}

		public long getPrimitiveLong() {
			return this.primitiveLong;
		}

		public long[] getPrimitiveLongArray() {
			return this.primitiveLongArray;
		}

		public Long getBoxedLong() {
			return this.boxedLong;
		}

		public Long[] getBoxedLongArray() {
			return this.boxedLongArray;
		}

		public String getString() {
			return this.string;
		}

		public String[] getStringArray() {
			return this.stringArray;
		}

		public void setPrimitiveInteger(int primitiveInteger) {
			this.primitiveInteger = primitiveInteger;
		}

		public void setPrimitiveIntegerArray(int[] primitiveIntegerArray) {
			this.primitiveIntegerArray = primitiveIntegerArray;
		}

		public void setBoxedInteger(Integer boxedInteger) {
			this.boxedInteger = boxedInteger;
		}

		public void setBoxedIntegerArray(Integer[] boxedIntegerArray) {
			this.boxedIntegerArray = boxedIntegerArray;
		}

		public void setPrimitiveShort(short primitiveShort) {
			this.primitiveShort = primitiveShort;
		}

		public void setPrimitiveShortArray(short[] primitiveShortArray) {
			this.primitiveShortArray = primitiveShortArray;
		}

		public void setBoxedShort(Short boxedShort) {
			this.boxedShort = boxedShort;
		}

		public void setBoxedShortArray(Short[] boxedShortArray) {
			this.boxedShortArray = boxedShortArray;
		}

		public void setPrimitiveByte(byte primitiveByte) {
			this.primitiveByte = primitiveByte;
		}

		public void setPrimitiveByteArray(byte[] primitiveByteArray) {
			this.primitiveByteArray = primitiveByteArray;
		}

		public void setBoxedByte(Byte boxedByte) {
			this.boxedByte = boxedByte;
		}

		public void setBoxedByteArray(Byte[] boxedByteArray) {
			this.boxedByteArray = boxedByteArray;
		}

		public void setPrimitiveChar(char primitiveChar) {
			this.primitiveChar = primitiveChar;
		}

		public void setPrimitiveCharArray(char[] primitiveCharArray) {
			this.primitiveCharArray = primitiveCharArray;
		}

		public void setBoxedChar(Character boxedChar) {
			this.boxedChar = boxedChar;
		}

		public void setBoxedCharArray(Character[] boxedCharArray) {
			this.boxedCharArray = boxedCharArray;
		}

		public void setPrimitiveBoolean(boolean primitiveBoolean) {
			this.primitiveBoolean = primitiveBoolean;
		}

		public void setPrimitiveBooleanArray(boolean[] primitiveBooleanArray) {
			this.primitiveBooleanArray = primitiveBooleanArray;
		}

		public void setBoxedBoolean(Boolean boxedBoolean) {
			this.boxedBoolean = boxedBoolean;
		}

		public void setBoxedBooleanArray(Boolean[] boxedBooleanArray) {
			this.boxedBooleanArray = boxedBooleanArray;
		}

		public void setPrimitiveFloat(float primitiveFloat) {
			this.primitiveFloat = primitiveFloat;
		}

		public void setPrimitiveFloatArray(float[] primitiveFloatArray) {
			this.primitiveFloatArray = primitiveFloatArray;
		}

		public void setBoxedFloat(Float boxedFloat) {
			this.boxedFloat = boxedFloat;
		}

		public void setBoxedFloatArray(Float[] boxedFloatArray) {
			this.boxedFloatArray = boxedFloatArray;
		}

		public void setPrimitiveDouble(double primitiveDouble) {
			this.primitiveDouble = primitiveDouble;
		}

		public void setPrimitiveDoubleArray(double[] primitiveDoubleArray) {
			this.primitiveDoubleArray = primitiveDoubleArray;
		}

		public void setBoxedDouble(Double boxedDouble) {
			this.boxedDouble = boxedDouble;
		}

		public void setBoxedDoubleArray(Double[] boxedDoubleArray) {
			this.boxedDoubleArray = boxedDoubleArray;
		}

		public void setPrimitiveLong(long primitiveLong) {
			this.primitiveLong = primitiveLong;
		}

		public void setPrimitiveLongArray(long[] primitiveLongArray) {
			this.primitiveLongArray = primitiveLongArray;
		}

		public void setBoxedLong(Long boxedLong) {
			this.boxedLong = boxedLong;
		}

		public void setBoxedLongArray(Long[] boxedLongArray) {
			this.boxedLongArray = boxedLongArray;
		}

		public void setString(String string) {
			this.string = string;
		}

		public void setStringArray(String[] stringArray) {
			this.stringArray = stringArray;
		}

	}

	// DATACMNS-916
	@AccessType(Type.FIELD)
	private static final class PrivateFinalFieldAccess {

		int primitiveInteger;
		int[] primitiveIntegerArray;
		Integer boxedInteger;
		Integer[] boxedIntegerArray;

		short primitiveShort;
		short[] primitiveShortArray;
		Short boxedShort;
		Short[] boxedShortArray;

		byte primitiveByte;
		byte[] primitiveByteArray;
		Byte boxedByte;
		Byte[] boxedByteArray;

		char primitiveChar;
		char[] primitiveCharArray;
		Character boxedChar;
		Character[] boxedCharArray;

		boolean primitiveBoolean;
		boolean[] primitiveBooleanArray;
		Boolean boxedBoolean;
		Boolean[] boxedBooleanArray;

		float primitiveFloat;
		float[] primitiveFloatArray;
		Float boxedFloat;
		Float[] boxedFloatArray;

		double primitiveDouble;
		double[] primitiveDoubleArray;
		Double boxedDouble;
		Double[] boxedDoubleArray;

		long primitiveLong;
		long[] primitiveLongArray;
		Long boxedLong;
		Long[] boxedLongArray;

		String string;
		String[] stringArray;

	}

	// DATACMNS-916
	@AccessType(Type.PROPERTY)
	private static final class PrivateFinalPropertyAccess {

		int primitiveInteger;
		int[] primitiveIntegerArray;
		Integer boxedInteger;
		Integer[] boxedIntegerArray;

		short primitiveShort;
		short[] primitiveShortArray;
		Short boxedShort;
		Short[] boxedShortArray;

		byte primitiveByte;
		byte[] primitiveByteArray;
		Byte boxedByte;
		Byte[] boxedByteArray;

		char primitiveChar;
		char[] primitiveCharArray;
		Character boxedChar;
		Character[] boxedCharArray;

		boolean primitiveBoolean;
		boolean[] primitiveBooleanArray;
		Boolean boxedBoolean;
		Boolean[] boxedBooleanArray;

		float primitiveFloat;
		float[] primitiveFloatArray;
		Float boxedFloat;
		Float[] boxedFloatArray;

		double primitiveDouble;
		double[] primitiveDoubleArray;
		Double boxedDouble;
		Double[] boxedDoubleArray;

		long primitiveLong;
		long[] primitiveLongArray;
		Long boxedLong;
		Long[] boxedLongArray;

		String string;
		String[] stringArray;

		public int getPrimitiveInteger() {
			return primitiveInteger;
		}

		public void setPrimitiveInteger(int primitiveInteger) {
			this.primitiveInteger = primitiveInteger;
		}

		public int[] getPrimitiveIntegerArray() {
			return primitiveIntegerArray;
		}

		public void setPrimitiveIntegerArray(int[] primitiveIntegerArray) {
			this.primitiveIntegerArray = primitiveIntegerArray;
		}

		public Integer getBoxedInteger() {
			return boxedInteger;
		}

		public void setBoxedInteger(Integer boxedInteger) {
			this.boxedInteger = boxedInteger;
		}

		public Integer[] getBoxedIntegerArray() {
			return boxedIntegerArray;
		}

		public void setBoxedIntegerArray(Integer[] boxedIntegerArray) {
			this.boxedIntegerArray = boxedIntegerArray;
		}

		public short getPrimitiveShort() {
			return primitiveShort;
		}

		public void setPrimitiveShort(short primitiveShort) {
			this.primitiveShort = primitiveShort;
		}

		public short[] getPrimitiveShortArray() {
			return primitiveShortArray;
		}

		public void setPrimitiveShortArray(short[] primitiveShortArray) {
			this.primitiveShortArray = primitiveShortArray;
		}

		public Short getBoxedShort() {
			return boxedShort;
		}

		public void setBoxedShort(Short boxedShort) {
			this.boxedShort = boxedShort;
		}

		public Short[] getBoxedShortArray() {
			return boxedShortArray;
		}

		public void setBoxedShortArray(Short[] boxedShortArray) {
			this.boxedShortArray = boxedShortArray;
		}

		public byte getPrimitiveByte() {
			return primitiveByte;
		}

		public void setPrimitiveByte(byte primitiveByte) {
			this.primitiveByte = primitiveByte;
		}

		public byte[] getPrimitiveByteArray() {
			return primitiveByteArray;
		}

		public void setPrimitiveByteArray(byte[] primitiveByteArray) {
			this.primitiveByteArray = primitiveByteArray;
		}

		public Byte getBoxedByte() {
			return boxedByte;
		}

		public void setBoxedByte(Byte boxedByte) {
			this.boxedByte = boxedByte;
		}

		public Byte[] getBoxedByteArray() {
			return boxedByteArray;
		}

		public void setBoxedByteArray(Byte[] boxedByteArray) {
			this.boxedByteArray = boxedByteArray;
		}

		public char getPrimitiveChar() {
			return primitiveChar;
		}

		public void setPrimitiveChar(char primitiveChar) {
			this.primitiveChar = primitiveChar;
		}

		public char[] getPrimitiveCharArray() {
			return primitiveCharArray;
		}

		public void setPrimitiveCharArray(char[] primitiveCharArray) {
			this.primitiveCharArray = primitiveCharArray;
		}

		public Character getBoxedChar() {
			return boxedChar;
		}

		public void setBoxedChar(Character boxedChar) {
			this.boxedChar = boxedChar;
		}

		public Character[] getBoxedCharArray() {
			return boxedCharArray;
		}

		public void setBoxedCharArray(Character[] boxedCharArray) {
			this.boxedCharArray = boxedCharArray;
		}

		public boolean isPrimitiveBoolean() {
			return primitiveBoolean;
		}

		public void setPrimitiveBoolean(boolean primitiveBoolean) {
			this.primitiveBoolean = primitiveBoolean;
		}

		public boolean[] getPrimitiveBooleanArray() {
			return primitiveBooleanArray;
		}

		public void setPrimitiveBooleanArray(boolean[] primitiveBooleanArray) {
			this.primitiveBooleanArray = primitiveBooleanArray;
		}

		public Boolean getBoxedBoolean() {
			return boxedBoolean;
		}

		public void setBoxedBoolean(Boolean boxedBoolean) {
			this.boxedBoolean = boxedBoolean;
		}

		public Boolean[] getBoxedBooleanArray() {
			return boxedBooleanArray;
		}

		public void setBoxedBooleanArray(Boolean[] boxedBooleanArray) {
			this.boxedBooleanArray = boxedBooleanArray;
		}

		public float getPrimitiveFloat() {
			return primitiveFloat;
		}

		public void setPrimitiveFloat(float primitiveFloat) {
			this.primitiveFloat = primitiveFloat;
		}

		public float[] getPrimitiveFloatArray() {
			return primitiveFloatArray;
		}

		public void setPrimitiveFloatArray(float[] primitiveFloatArray) {
			this.primitiveFloatArray = primitiveFloatArray;
		}

		public Float getBoxedFloat() {
			return boxedFloat;
		}

		public void setBoxedFloat(Float boxedFloat) {
			this.boxedFloat = boxedFloat;
		}

		public Float[] getBoxedFloatArray() {
			return boxedFloatArray;
		}

		public void setBoxedFloatArray(Float[] boxedFloatArray) {
			this.boxedFloatArray = boxedFloatArray;
		}

		public double getPrimitiveDouble() {
			return primitiveDouble;
		}

		public void setPrimitiveDouble(double primitiveDouble) {
			this.primitiveDouble = primitiveDouble;
		}

		public double[] getPrimitiveDoubleArray() {
			return primitiveDoubleArray;
		}

		public void setPrimitiveDoubleArray(double[] primitiveDoubleArray) {
			this.primitiveDoubleArray = primitiveDoubleArray;
		}

		public Double getBoxedDouble() {
			return boxedDouble;
		}

		public void setBoxedDouble(Double boxedDouble) {
			this.boxedDouble = boxedDouble;
		}

		public Double[] getBoxedDoubleArray() {
			return boxedDoubleArray;
		}

		public void setBoxedDoubleArray(Double[] boxedDoubleArray) {
			this.boxedDoubleArray = boxedDoubleArray;
		}

		public long getPrimitiveLong() {
			return primitiveLong;
		}

		public void setPrimitiveLong(long primitiveLong) {
			this.primitiveLong = primitiveLong;
		}

		public long[] getPrimitiveLongArray() {
			return primitiveLongArray;
		}

		public void setPrimitiveLongArray(long[] primitiveLongArray) {
			this.primitiveLongArray = primitiveLongArray;
		}

		public Long getBoxedLong() {
			return boxedLong;
		}

		public void setBoxedLong(Long boxedLong) {
			this.boxedLong = boxedLong;
		}

		public Long[] getBoxedLongArray() {
			return boxedLongArray;
		}

		public void setBoxedLongArray(Long[] boxedLongArray) {
			this.boxedLongArray = boxedLongArray;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public String[] getStringArray() {
			return stringArray;
		}

		public void setStringArray(String[] stringArray) {
			this.stringArray = stringArray;
		}
	}
}
