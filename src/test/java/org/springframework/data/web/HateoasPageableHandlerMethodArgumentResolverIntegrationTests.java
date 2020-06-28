package org.springframework.data.web;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Réda Housni Alaoui
 */
public class HateoasPageableHandlerMethodArgumentResolverIntegrationTests {

	@Configuration
	@EnableWebMvc
	@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL})
	@Import(HateoasAwareSpringDataWebConfiguration.class)
	static class Config {

		@Bean
		SampleController controller() {
			return new SampleController();
		}
	}

	@BeforeEach
	void setUp() {
		WebTestUtils.initWebTest();
	}

	@Test // DATACMNS-1757
	void navigationLinksPreserveQueryParams() throws Exception {
		WebApplicationContext context = WebTestUtils.createApplicationContext(Config.class);
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

		mockMvc.perform(MockMvcRequestBuilders.get("/persons").queryParam("foo", "bar")
				.accept(MediaTypes.HAL_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.first.href").value("http://localhost/persons?foo=bar&page=0&size=20"))
				.andExpect(jsonPath("$._links.self.href").value("http://localhost/persons?foo=bar&page=0&size=20"))
				.andExpect(jsonPath("$._links.next.href").value("http://localhost/persons?foo=bar&page=1&size=20"))
				.andExpect(jsonPath("$._links.last.href").value("http://localhost/persons?foo=bar&page=1&size=20"));
	}

	@Controller
	static class SampleController {

		@GetMapping("/persons")
		@ResponseBody
		PagedModel<EntityModel<Person>> sample(Pageable pageable, PagedResourcesAssembler<Person> assembler) {

			Page<Person> page = new PageImpl<>(Collections.singletonList(new Person("John Doe")), pageable,
					pageable.getOffset() + pageable.getPageSize() + 1);

			return assembler.toModel(page);
		}
	}

	static class Person {

		public final String name;

		Person(String name) {
			this.name = name;
		}
	}
}
