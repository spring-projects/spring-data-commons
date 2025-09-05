package org.springframework.data.web.config;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.web.SlicedResourcesAssembler;
import org.springframework.data.web.WebTestUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.SlicedModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

public class SliceableResourcesAssemblerIntegrationTests {
	@BeforeEach
	void setUp() {
		WebTestUtils.initWebTest();
	}

	@Test
	void injectsSlicedResourcesAssembler() {
		var context = WebTestUtils.createApplicationContext(Config.class);
		var controller = context.getBean(SampleController.class);

		assertThat(controller.assembler).isNotNull();

		var resources = controller.sample(PageRequest.of(1, 1));

		assertThat(resources.getLink(IanaLinkRelations.PREV)).isNotNull();
		assertThat(resources.getLink(IanaLinkRelations.NEXT)).isNotNull();
		assertThat(resources.getLink(IanaLinkRelations.SELF)).isNotNull();
	}

	@Test
	void setsUpSlicedResourcesAssemblerFromManualXmlConfig() {
		var context = new ClassPathXmlApplicationContext("manual.xml", getClass());
		assertThat(context.getBean(SlicedResourcesAssembler.class)).isNotNull();
		context.close();
	}

	@Test
	void setsUpPagedResourcesAssemblerFromJavaConfigXmlConfig() {
		var context = new ClassPathXmlApplicationContext("via-config-class.xml", getClass());
		assertThat(context.getBean(SlicedResourcesAssembler.class)).isNotNull();
		context.close();
	}

	@Configuration
	@EnableSpringDataWebSupport
	static class Config {

		@Bean
		SampleController controller() {
			return new SampleController();
		}
	}

	@Controller
	static class SampleController {
		@Autowired
		SlicedResourcesAssembler<Person> assembler;

		@RequestMapping("/persons")
		SlicedModel<EntityModel<Person>> sample(Pageable pageable) {

			Slice<Person> page = new SliceImpl<>(Collections.singletonList(new Person()), pageable, true);

			return assembler.toModel(page);
		}
	}

	static class Person {
	}
}
