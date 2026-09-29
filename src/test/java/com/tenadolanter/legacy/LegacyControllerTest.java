package com.tenadolanter.legacy;

import com.tenadolanter.response.support.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "tenadolanter.response.error-controller-enabled=false")
class LegacyControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void existingControllerStaysUnwrapped() {
        ResponseEntity<String> response = restTemplate.getForEntity("/legacy/text", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("legacy");
    }

    @Test
    void existingControllerExceptionIsNotRewritten() {
        ResponseEntity<String> response = restTemplate.getForEntity("/legacy/boom", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).doesNotContain("\"code\":500");
        assertThat(response.getBody()).doesNotContain("legacy down");
    }

    @Test
    void annotatedMethodUsesUnifiedResponse() {
        ResponseEntity<String> response = restTemplate.getForEntity("/legacy/adopted", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"name\":\"adopted\"");
        assertThat(response.getBody()).contains("\"OK\"");
    }

    @Test
    void annotatedMethodExceptionUsesUnifiedResponse() {
        ResponseEntity<String> response = restTemplate.getForEntity("/legacy/adopted-boom", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).contains("Internal server error");
        assertThat(response.getBody()).doesNotContain("adopted down");
    }
}
