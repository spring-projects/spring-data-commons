/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.data.aot;

import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import org.mockito.Mockito;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;

/**
 * Test utility to build and compile AOT code contributions.
 *
 * @author Christoph Strobl
 */
public class AotTestCodeContributionBuilder {

	TestGenerationContext generationContext;
	MockBeanRegistrationCode beanRegistrationCode;
	BeanRegistrationAotContribution contribution;

	static AotTestCodeContributionBuilder withContextFor(Class<?> type) {
		return withContext(new TestGenerationContext(type));
	}

	static AotTestCodeContributionBuilder withContext(TestGenerationContext ctx) {

		AotTestCodeContributionBuilder codeGenerationBuilder = new AotTestCodeContributionBuilder();
		codeGenerationBuilder.generationContext = ctx;
		codeGenerationBuilder.beanRegistrationCode = new MockBeanRegistrationCode(ctx);
		return codeGenerationBuilder;
	}

	BeanRegistrationCodeFragments getFragments(BeanRegistrationAotContribution contribution) {

		this.contribution = contribution;

		return contribution.customizeBeanRegistrationCodeFragments(generationContext,
				Mockito.mock(BeanRegistrationCodeFragments.class));
	}

	AotTestCodeContributionBuilder writeContentFor(BeanRegistrationAotContribution contribution) {

		CodeBlock codeBlock = getFragments(contribution).generateInstanceSupplierCode(generationContext,
				beanRegistrationCode, null, false);

		Class<?> beanType = Object.class;
		try {
			beanType = contribution instanceof RegisteredBeanAotContribution
					? ((RegisteredBeanAotContribution) contribution).getSource().getBeanClass()
					: Object.class;
		} catch (Exception e) {}

		ParameterizedTypeName parameterizedReturnTypeName = ParameterizedTypeName.get(InstanceSupplier.class, beanType);
		beanRegistrationCode.getTypeBuilder().set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			type.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC).returns(parameterizedReturnTypeName)
					.addStatement("return $L", codeBlock).build());
		});

		return this;
	}

	public void compile() {
		compile(it -> {});
	}

	public void compile(Consumer<Compiled> compiled) {
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(generationContext).compile(compiled);
	}
}
