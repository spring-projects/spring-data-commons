/*
 * Copyright 2023. the original author or authors.
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

package org.springframework.data.util;

import java.util.List;

import org.springframework.data.spel.EvaluationContextProvider;
import org.springframework.data.support.EnvironmentAccessor;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;

/**
 * @author Christoph Strobl
 * @since 2023/11
 */
public abstract class ExpressionEvaluator {

	private EnvironmentAccessor environmentAccessor;
	private EvaluationContextProvider evaluationContextProvider;

	public ExpressionEvaluator(EnvironmentAccessor environmentAccessor, EvaluationContextProvider evaluationContextProvider) {
		this.environmentAccessor = environmentAccessor;
		this.evaluationContextProvider = evaluationContextProvider;
	}

	abstract <T> T evaluate(String text, EnvironmentAwareEvaluationContext context);

	public class EnvironmentAwareEvaluationContext implements EvaluationContext, EnvironmentAccessor, EvaluationContextProvider {

		@Override
		public EvaluationContext getEvaluationContext(Object rootObject) {
			return null;
		}

		@Nullable
		@Override
		public String getProperty(String key) {
			return null;
		}

		@Override
		public String resolvePlaceholders(String text) {
			return null;
		}

		@Override
		public TypedValue getRootObject() {
			return null;
		}

		@Override
		public List<PropertyAccessor> getPropertyAccessors() {
			return null;
		}

		@Override
		public List<ConstructorResolver> getConstructorResolvers() {
			return null;
		}

		@Override
		public List<MethodResolver> getMethodResolvers() {
			return null;
		}

		@Nullable
		@Override
		public BeanResolver getBeanResolver() {
			return null;
		}

		@Override
		public TypeLocator getTypeLocator() {
			return null;
		}

		@Override
		public TypeConverter getTypeConverter() {
			return null;
		}

		@Override
		public TypeComparator getTypeComparator() {
			return null;
		}

		@Override
		public OperatorOverloader getOperatorOverloader() {
			return null;
		}

		@Override
		public void setVariable(String name, @Nullable Object value) {

		}

		@Nullable
		@Override
		public Object lookupVariable(String name) {
			return null;
		}
	}


}
