/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.data;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.core.RepositoryMetadata;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * Tests for package and slice cycles. All packages that have the same start including the part after {@literal data}
 * are considered one slice. For example {@literal org.springframework.data.repository} and
 * {@literal org.springframework.data.repository.support} are part of the same slice {@literal repository}.
 *
 * @author Jens Schauder
 */
public class DependencyTests {

	JavaClasses importedClasses = new ClassFileImporter() //
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS) //
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS) // we just analyze the code of this module.
			.importPackages("org.springframework.data") //
			.that(onlySpringData()) //

			// new cycle
			.that(ignore(RepositoryMetadata.class));

	@Test
	void cycleFreeSlices() {

		ArchRule rule = SlicesRuleDefinition.slices() //
				.matching("org.springframework.data.(*)..") //
				.should() //
				.beFreeOfCycles();

		rule.check(importedClasses);
	}

	@Test
	void cycleFreePackages() {

		ArchRule rule = SlicesRuleDefinition.slices() //
				.matching("org.springframework.data.(**)") //
				.should() //
				.beFreeOfCycles();

		rule.check(importedClasses);
	}

	@Test
	void testGetFirstPackagePart() {

		assertThat(getFirstPackagePart("a.b.c")).isEqualTo("a");
		assertThat(getFirstPackagePart("a")).isEqualTo("a");
	}

	@Test
	void testSubModule() {

		assertThat(subModule("a.b", "a.b.c.d")).isEqualTo("c");
		assertThat(subModule("a.b", "a.b.c")).isEqualTo("c");
		assertThat(subModule("a.b", "a.b")).isEqualTo("");
	}

	private DescribedPredicate<JavaClass> onlySpringData() {

		return new DescribedPredicate<>("Spring Data Classes") {
			@Override
			public boolean test(JavaClass input) {
				return input.getPackageName().startsWith("org.springframework.data");
			}
		};
	}

	private DescribedPredicate<JavaClass> ignore(Class<?> type) {

		return new DescribedPredicate<>("ignored class " + type.getName()) {
			@Override
			public boolean test(JavaClass input) {
				return !input.getFullName().startsWith(type.getName());
			}
		};
	}

	private DescribedPredicate<JavaClass> ignorePackage(String type) {

		return new DescribedPredicate<>("ignored class " + type) {
			@Override
			public boolean test(JavaClass input) {
				return !input.getPackageName().equals(type);
			}
		};
	}

	private String getFirstPackagePart(String subpackage) {

		int index = subpackage.indexOf(".");
		if (index < 0) {
			return subpackage;
		}
		return subpackage.substring(0, index);
	}

	private String subModule(String basePackage, String packageName) {

		if (packageName.startsWith(basePackage) && packageName.length() > basePackage.length()) {

			final int index = basePackage.length() + 1;
			String subpackage = packageName.substring(index);
			return getFirstPackagePart(subpackage);
		}
		return "";
	}

}
