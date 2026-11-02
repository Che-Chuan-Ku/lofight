package com.crowdsiege.api.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestStubConfiguration.class)
public class CucumberSpringConfiguration {

    @LocalServerPort
    private int port;

    public String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
