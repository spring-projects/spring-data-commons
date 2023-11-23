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

/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.mapping.model.justme;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.spel.ExtensionAwareEvaluationContextProvider;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Christoph Strobl
 * @since 2023/11
 */

@ExtendWith(SpringExtension.class)
@ContextConfiguration
public class PropertySourceUnitTests {

	@Value("${my-type.name}") String entityName;

	@Autowired ListableBeanFactory beanFactory;
	SpelExpressionParser parser = new SpelExpressionParser();

	@Configuration
	@PropertySource("classpath:persistent-entity.properties")
	static class Config {

	}

//	@Test
	void plainContext() {

		System.getProperties().forEach((key,value) -> System.out.printf("%s:%s\n", key, value));
		Expression expression = parse("#{systemProperties['os.arch']}");

		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		Object value = expression.getValue();
		System.out.println("va: " + value);
	}

	Expression parse(String source) {
		return parser.parseExpression(source, getParserContext());
	}

//	@Test
	void loadsContext() {

		Map<String, PropertyResolver> beansOfType = beanFactory.getBeansOfType(PropertyResolver.class, false, false);

		ExtensionAwareEvaluationContextProvider contextProvider = new ExtensionAwareEvaluationContextProvider(beanFactory);


		// Expression spelExpression = parser.parseExpression("#{ T(java.lang.Math).random() * 100.0 }",
		// getParserContext());
		// Expression expression = parser.parseExpression("#{ T(java.lang.Math).random() * 100.0 }");
		// Expression expression = parser.parseExpression("#{ systemProperties['user.region'] }", getParserContext());

		StandardEvaluationContext evaluationContext = contextProvider.getEvaluationContext(new StandardEnvironment());
		SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(SpelCompilerMode.OFF, null));
		Expression expression = parser.parseExpression("#{systemProperties['os.arch']}", getParserContext());
		Object value = expression.getValue(evaluationContext);
		System.out.println("value: " + value);
		// Expression expression = parser.parseExpression("#{ ${x.y.z} ?: 'defaultValue' }");
		// Expression expression = parser.parseExpression("#{'${my-type.name}'}");

		// System.out.println("expression: " + expression);
		// Object value = expression.getValue(evaluationContext);
		// System.out.println("value: " + value);
	}

	private static ParserContext getParserContext() {
		return new ParserContext() {
			@Override
			public boolean isTemplate() {
				return true;
			}

			@Override
			public String getExpressionPrefix() {
				return "#{";
			}

			@Override
			public String getExpressionSuffix() {
				return "}";
			}
		};
	}
}
