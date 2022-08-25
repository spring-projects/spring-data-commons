/*
 * Copyright 2022 the original author or authors.
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

import javax.lang.model.element.Modifier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.mockito.Mockito;
import org.springframework.aot.test.generator.compile.Compiled;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCodeFragments;
import org.springframework.data.domain.ManagedTypes;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterizedTypeName;
import org.springframework.test.aot.generate.TestGenerationContext;

/**
 * @author Christoph Strobl
 */
public class AotTestCodeContributionBuilder {

	TestGenerationContext generationContext;
	MockBeanRegistrationCode beanRegistrationCode;

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
		return contribution.customizeBeanRegistrationCodeFragments(generationContext,
				Mockito.mock(BeanRegistrationCodeFragments.class));
	}

	AotTestCodeContributionBuilder writeContentFor(BeanRegistrationAotContribution contribution) {

		CodeBlock codeBlock = getFragments(contribution).generateInstanceSupplierCode(generationContext,
				beanRegistrationCode, null, false);

		ParameterizedTypeName parameterizedReturnTypeName = ParameterizedTypeName.get(Supplier.class, ManagedTypes.class);
		beanRegistrationCode.getTypeBuilder().set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			type.addMethod(MethodSpec.methodBuilder("get").addModifiers(Modifier.PUBLIC)
					.returns(parameterizedReturnTypeName).addStatement("return $L", codeBlock).build());
		});

		return this;
	}

	public void compile() {
		compile(it -> {
		});
	}

	public void compile(Consumer<Compiled> compiled) {
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().withFiles(generationContext.getGeneratedFiles()).compile(compiled);
	}
}
