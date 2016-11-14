/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.web.config;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.WebTestUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for {@link PagedResourcesAssembler}.
 * 
 * @author Oliver Gierke
 */
public class PageableResourcesAssemblerIntegrationTests {

	@Configuration
	@EnableSpringDataWebSupport
	static class Config {

		@Bean
		public SampleController controller() {
			return new SampleController();
		}
	}

	@Before
	public void setUp() {
		WebTestUtils.initWebTest();
	}

	@Test
	public void injectsPagedResourcesAssembler() {

		WebApplicationContext context = WebTestUtils.createApplicationContext(Config.class);
		SampleController controller = context.getBean(SampleController.class);

		assertThat(controller.assembler).isNotNull();

		PagedResources<Resource<Person>> resources = controller.sample(new PageRequest(1, 1));

		assertThat(resources.getLink(Link.REL_PREVIOUS)).isNotNull();
		assertThat(resources.getLink(Link.REL_NEXT)).isNotNull();
		assertThat(resources.getLink(Link.REL_SELF)).isNotNull();
	}

	/**
	 * @see DATACMNS-471
	 */
	@Test
	public void setsUpPagedResourcesAssemblerFromManualXmlConfig() {

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("manual.xml", getClass());
		assertThat(context.getBean(PagedResourcesAssembler.class)).isNotNull();
		context.close();
	}

	/**
	 * @see DATACMNS-471
	 */
	@Test
	public void setsUpPagedResourcesAssemblerFromJavaConfigXmlConfig() {

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("via-config-class.xml", getClass());
		assertThat(context.getBean(PagedResourcesAssembler.class)).isNotNull();
		context.close();
	}

	@Controller
	static class SampleController {

		@Autowired PagedResourcesAssembler<Person> assembler;

		@RequestMapping("/persons")
		PagedResources<Resource<Person>> sample(Pageable pageable) {

			Page<Person> page = new PageImpl<Person>(Arrays.asList(new Person()), pageable,
					pageable.getOffset() + pageable.getPageSize() + 1);

			return assembler.toResource(page);
		}
	}

	static class Person {

	}
}
