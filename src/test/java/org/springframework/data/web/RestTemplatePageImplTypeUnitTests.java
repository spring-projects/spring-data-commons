package org.springframework.data.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

/**
 * Unit tests for {@link PageImpl}.
 * Unit tests for {@link org.springframework.web.client.RestTemplate}.
 *
 * @author woniper
 */
public class RestTemplatePageImplTypeUnitTests {

    @Before
    public void setUp() throws Exception {
    }

    @Configuration
    static class Config {
        @Bean
        public SampleController testPageImplController() {
            return new SampleController();
        }
    }

    @Test // DATACMNS-1061
    public void test() throws Exception {
        WebApplicationContext context = WebTestUtils.createApplicationContext(Config.class);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<PageImpl<SampleData>> responseEntity =
                restTemplate.exchange("/test", HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<PageImpl<SampleData>>() {});

        responseEntity.getBody();
    }

    @Controller
    static class SampleController {
        @GetMapping("/test")
        public ResponseEntity<?> test() {
            return ResponseEntity.ok(new PageImpl<>(Arrays.asList(new SampleData("woniper"), new SampleData("admin"))));
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    static class SampleData {
        private String username;
    }
}
