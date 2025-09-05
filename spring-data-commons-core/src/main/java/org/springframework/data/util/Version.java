/*
 * Copyright 2013-2025 the original author or authors.
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
package org.springframework.data.util;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to represent a Version consisting of major, minor and bugfix part.
 *
 * @author Oliver Gierke
 */
public class Version implements Comparable<Version> {

	private static final String VERSION_PARSE_ERROR = "Invalid version string; Could not parse segment %s within %s";

	private final int major;
	private final int minor;
	private final int bugfix;
	private final int build;

	/**
	 * Creates a new {@link Version} from the given integer values. At least one value has to be given but a maximum of 4.
	 *
	 * @param parts must not be {@literal null} or empty.
	 */
	public Version(int... parts) {

		Assert.notNull(parts, "Parts must not be null");
		Assert.isTrue(parts.length > 0 && parts.length < 5,
				String.format("Invalid parts length: 0 < %s < 5", parts.length));

		this.major = parts[0];
		this.minor = parts.length > 1 ? parts[1] : 0;
		this.bugfix = parts.length > 2 ? parts[2] : 0;
		this.build = parts.length > 3 ? parts[3] : 0;

		Assert.isTrue(major >= 0, "Major version must be greater or equal zero");
		Assert.isTrue(minor >= 0, "Minor version must be greater or equal zero");
		Assert.isTrue(bugfix >= 0, "Bugfix version must be greater or equal zero");
		Assert.isTrue(build >= 0, "Build version must be greater or equal zero");
	}

	/**
	 * Parses the given string representation of a version into a {@link Version} object.
	 *
	 * @param version must not be {@literal null} or empty.
	 * @return
	 */
	public static Version parse(String version) {

		Assert.hasText(version, "Version must not be null o empty");

		String[] parts = version.trim().split("\\.");
		int[] intParts = new int[parts.length];

		for (int i = 0; i < parts.length; i++) {

			String input = i == parts.length - 1 ? parts[i].replaceAll("\\D.*", "") : parts[i];

			if (StringUtils.hasText(input)) {
				try {
					intParts[i] = Integer.parseInt(input);
				} catch (IllegalArgumentException o_O) {
					throw new IllegalArgumentException(String.format(VERSION_PARSE_ERROR, input, version), o_O);
				}
			}
		}

		return new Version(intParts);
	}

	/**
	 * Returns the Java version of the running JVM.
	 *
	 * @return will never be {@literal null}.
	 */
	public static Version javaVersion() {
		return parse(System.getProperty("java.version"));
	}

	/**
	 * Returns whether the current {@link Version} is greater (newer) than the given one.
	 *
	 * @param version
	 * @return
	 */
	public boolean isGreaterThan(Version version) {
		return compareTo(version) > 0;
	}

	/**
	 * Returns whether the current {@link Version} is greater (newer) or the same as the given one.
	 *
	 * @param version
	 * @return
	 */
	public boolean isGreaterThanOrEqualTo(Version version) {
		return compareTo(version) >= 0;
	}

	/**
	 * Returns whether the current {@link Version} is the same as the given one.
	 *
	 * @param version
	 * @return
	 */
	public boolean is(Version version) {
		return equals(version);
	}

	/**
	 * Returns whether the current {@link Version} is less (older) than the given one.
	 *
	 * @param version
	 * @return
	 */
	public boolean isLessThan(Version version) {
		return compareTo(version) < 0;
	}

	/**
	 * Returns whether the current {@link Version} is less (older) or equal to the current one.
	 *
	 * @param version
	 * @return
	 */
	public boolean isLessThanOrEqualTo(Version version) {
		return compareTo(version) <= 0;
	}

	@Override
	public int compareTo(@SuppressWarnings("null") Version that) {

		if (major != that.major) {
			return major - that.major;
		}

		if (minor != that.minor) {
			return minor - that.minor;
		}

		if (bugfix != that.bugfix) {
			return bugfix - that.bugfix;
		}

		if (build != that.build) {
			return build - that.build;
		}

		return 0;
	}

	@Override
	public boolean equals(@Nullable Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Version that)) {
			return false;
		}

		return this.major == that.major && this.minor == that.minor && this.bugfix == that.bugfix
				&& this.build == that.build;
	}

	@Override
	public int hashCode() {

		int result = 17;
		result += 31 * major;
		result += 31 * minor;
		result += 31 * bugfix;
		result += 31 * build;
		return result;
	}

	@Override
	public String toString() {

		List<Integer> digits = new ArrayList<>();
		digits.add(major);
		digits.add(minor);

		if (build != 0 || bugfix != 0) {
			digits.add(bugfix);
		}

		if (build != 0) {
			digits.add(build);
		}

		return StringUtils.collectionToDelimitedString(digits, ".");
	}
}
