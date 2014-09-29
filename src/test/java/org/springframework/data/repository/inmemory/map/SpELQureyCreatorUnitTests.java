/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.repository.inmemory.map;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 */
@RunWith(MockitoJUnitRunner.class)
public class SpELQureyCreatorUnitTests {

	private static final Person RICKON = new Person("rickon", 4);
	private static final Person BRAN = new Person("bran", 9).skinChanger(true);
	private static final Person ARYA = new Person("arya", 13);
	private static final Person ROBB = new Person("robb", 16).named("stark");
	private static final Person JON = new Person("jon", 17).named("snow");

	private @Mock RepositoryMetadata metadataMock;

	@Test
	public void equalsReturnsTrueWhenMatching() throws Exception {
		assertThat(evaluate("findByFirstname", BRAN.firstname).against(BRAN), is(true));
	}

	@Test
	public void equalsReturnsFalseWhenNotMatching() throws Exception {
		assertThat(evaluate("findByFirstname", BRAN.firstname).against(RICKON), is(false));
	}

	@Test
	public void isTrueAssertedPropertlyWhenTrue() throws Exception {
		assertThat(evaluate("findBySkinChangerIsTrue").against(BRAN), is(true));
	}

	@Test
	public void isTrueAssertedPropertlyWhenFalse() throws Exception {
		assertThat(evaluate("findBySkinChangerIsTrue").against(RICKON), is(false));
	}

	@Test
	public void isFalseAssertedPropertlyWhenTrue() throws Exception {
		assertThat(evaluate("findBySkinChangerIsFalse").against(BRAN), is(false));
	}

	@Test
	public void isFalseAssertedPropertlyWhenFalse() throws Exception {
		assertThat(evaluate("findBySkinChangerIsFalse").against(RICKON), is(true));
	}

	@Test
	public void isNullAssertedPropertlyWhenAttributeIsNull() throws Exception {
		assertThat(evaluate("findByLastnameIsNull").against(BRAN), is(true));
	}

	@Test
	public void isNullAssertedPropertlyWhenAttributeIsNotNull() throws Exception {
		assertThat(evaluate("findByLastnameIsNull").against(ROBB), is(false));
	}

	@Test
	public void isNotNullFalseTrueWhenAttributeIsNull() throws Exception {
		assertThat(evaluate("findByLastnameIsNotNull").against(BRAN), is(false));
	}

	@Test
	public void isNotNullReturnsTrueAttributeIsNotNull() throws Exception {
		assertThat(evaluate("findByLastnameIsNotNull").against(ROBB), is(true));
	}

	@Test
	public void startsWithReturnsTrueWhenMatching() throws Exception {
		assertThat(evaluate("findByFirstnameStartingWith", "r").against(ROBB), is(true));
	}

	@Test
	public void startsWithReturnsFalseWhenNotMatching() throws Exception {
		assertThat(evaluate("findByFirstnameStartingWith", "r").against(BRAN), is(false));
	}

	@Test
	public void likeReturnsTrueWhenMatching() throws Exception {
		assertThat(evaluate("findByFirstnameLike", "ob").against(ROBB), is(true));
	}

	@Test
	public void likeReturnsFalseWhenNotMatching() throws Exception {
		assertThat(evaluate("findByFirstnameLike", "ra").against(ROBB), is(false));
	}

	@Test
	public void endsWithReturnsTrueWhenMatching() throws Exception {
		assertThat(evaluate("findByFirstnameEndingWith", "bb").against(ROBB), is(true));
	}

	@Test
	public void endsWithReturnsFalseWhenNotMatching() throws Exception {
		assertThat(evaluate("findByFirstnameEndingWith", "an").against(ROBB), is(false));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void startsWithIgnoreCaseReturnsTrueWhenMatching() throws Exception {
		assertThat(evaluate("findByFirstnameIgnoreCase", "R").against(ROBB), is(true));
	}

	@Test
	public void greaterThanReturnsTrueForHigherValues() throws Exception {
		assertThat(evaluate("findByAgeGreaterThan", BRAN.age).against(ROBB), is(true));
	}

	@Test
	public void greaterThanReturnsFalseForLowerValues() throws Exception {
		assertThat(evaluate("findByAgeGreaterThan", BRAN.age).against(RICKON), is(false));
	}

	@Test
	public void greaterThanEaualsReturnsTrueForHigherValues() throws Exception {
		assertThat(evaluate("findByAgeGreaterThanEqual", BRAN.age).against(ROBB), is(true));
	}

	@Test
	public void greaterThanEaualsReturnsTrueForEqualValues() throws Exception {
		assertThat(evaluate("findByAgeGreaterThanEqual", BRAN.age).against(BRAN), is(true));
	}

	@Test
	public void greaterThanEqualsReturnsFalseForLowerValues() throws Exception {
		assertThat(evaluate("findByAgeGreaterThanEqual", BRAN.age).against(RICKON), is(false));
	}

	@Test
	public void lessThanReturnsTrueForHigherValues() throws Exception {
		assertThat(evaluate("findByAgeLessThan", BRAN.age).against(ROBB), is(false));
	}

	@Test
	public void lessThanReturnsFalseForLowerValues() throws Exception {
		assertThat(evaluate("findByAgeLessThan", BRAN.age).against(RICKON), is(true));
	}

	@Test
	public void lessThanEaualsReturnsTrueForHigherValues() throws Exception {
		assertThat(evaluate("findByAgeLessThanEqual", BRAN.age).against(ROBB), is(false));
	}

	@Test
	public void lessThanEaualsReturnsTrueForEqualValues() throws Exception {
		assertThat(evaluate("findByAgeLessThanEqual", BRAN.age).against(BRAN), is(true));
	}

	@Test
	public void lessThanEqualsReturnsFalseForLowerValues() throws Exception {
		assertThat(evaluate("findByAgeLessThanEqual", BRAN.age).against(RICKON), is(true));
	}

	@Test
	public void betweenEqualsReturnsTrueForValuesInBetween() throws Exception {
		assertThat(evaluate("findByAgeBetween", BRAN.age, ROBB.age).against(ARYA), is(true));
	}

	@Test
	public void betweenEqualsReturnsFalseForHigherValues() throws Exception {
		assertThat(evaluate("findByAgeBetween", BRAN.age, ROBB.age).against(JON), is(false));
	}

	@Test
	public void betweenEqualsReturnsFalseForLowerValues() throws Exception {
		assertThat(evaluate("findByAgeBetween", BRAN.age, ROBB.age).against(RICKON), is(false));
	}

	@Test
	public void connectByAndReturnsTrueWhenAllPropertiesMatching() throws Exception {
		assertThat(evaluate("findByAgeGreaterThanAndLastname", BRAN.age, JON.lastname).against(JON), is(true));
	}

	@Test
	public void connectByAndReturnsFalseWhenOnlyFewPropertiesMatch() throws Exception {
		assertThat(evaluate("findByAgeGreaterThanAndLastname", BRAN.age, JON.lastname).against(ROBB), is(false));
	}

	@Test
	public void connectByOrReturnsTrueWhenOnlyFewPropertiesMatch() throws Exception {
		assertThat(evaluate("findByAgeGreaterThanOrLastname", BRAN.age, JON.lastname).against(ROBB), is(true));
	}

	@Test
	public void connectByOrReturnsTrueWhenAllPropertiesMatch() throws Exception {
		assertThat(evaluate("findByAgeGreaterThanOrLastname", BRAN.age, JON.lastname).against(JON), is(true));
	}

	@Test
	public void regexReturnsTrueWhenMatching() throws Exception {
		assertThat(evaluate("findByLastnameMatches", "^s.*w$").against(JON), is(true));
	}

	@Test
	public void regexReturnsFalseWhenNotMatching() throws Exception {
		assertThat(evaluate("findByLastnameMatches", "^s.*w$").against(ROBB), is(false));
	}

	Evaluation evaluate(String methodName, Object... args) throws Exception {
		return new Evaluation((SpelExpression) createQueryForMethodWithArgs(methodName, args).getCritieria());
	}

	private MapQuery createQueryForMethodWithArgs(String methodName, Object... args) throws NoSuchMethodException,
			SecurityException {

		Class<?>[] argTypes = new Class<?>[args.length];
		if (!ObjectUtils.isEmpty(args)) {

			for (int i = 0; i < args.length; i++) {
				argTypes[i] = args[i].getClass();
			}

		}
		Method method = PersonRepository.class.getMethod(methodName, argTypes);

		PartTree partTree = new PartTree(method.getName(), method.getReturnType());
		SpelQueryCreator creator = new SpelQueryCreator(partTree, new ParametersParameterAccessor(new QueryMethod(method,
				metadataMock).getParameters(), args));

		MapQuery q = creator.createQuery();
		((SpelExpression) q.getCritieria()).setEvaluationContext(new StandardEvaluationContext(args));
		return q;
	}

	static interface PersonRepository {

		Person findByFirstname(String firstname);

		Person findBySkinChangerIsTrue();

		Person findBySkinChangerIsFalse();

		Person findByLastnameIsNull();

		Person findByLastnameIsNotNull();

		Person findByFirstnameStartingWith(String firstanme);

		Person findByFirstnameIgnoreCase(String firstanme);

		Person findByAgeGreaterThan(Integer age);

		Person findByAgeGreaterThanEqual(Integer age);

		Person findByAgeLessThan(Integer age);

		Person findByAgeLessThanEqual(Integer age);

		Person findByAgeBetween(Integer low, Integer high);

		Person findByFirstnameLike(String firstname);

		Person findByFirstnameEndingWith(String firstname);

		Person findByAgeGreaterThanAndLastname(Integer age, String lastname);

		Person findByAgeGreaterThanOrLastname(Integer age, String lastname);

		Person findByLastnameMatches(String lastname);

	}

	static class Evaluation {

		SpelExpression expression;
		Object candidate;

		public Evaluation(SpelExpression expresison) {
			this.expression = expresison;
		}

		public Boolean against(Object candidate) {
			this.candidate = candidate;
			return evaluate();
		}

		private boolean evaluate() {
			expression.getEvaluationContext().setVariable("it", candidate);
			return expression.getValue(Boolean.class);
		}

	}

	static class Person {

		private @Id String id;
		private String firstname, lastname;
		private int age;
		private boolean isSkinChanger = false;

		public Person() {}

		public Person(String firstname, int age) {
			super();
			this.firstname = firstname;
			this.age = age;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public boolean isSkinChanger() {
			return isSkinChanger;
		}

		public void setSkinChanger(boolean isSkinChanger) {
			this.isSkinChanger = isSkinChanger;
		}

		public Person skinChanger(boolean isSkinChanger) {
			this.isSkinChanger = isSkinChanger;
			return this;
		}

		public Person named(String lastname) {
			this.lastname = lastname;
			return this;
		}

	}

}
