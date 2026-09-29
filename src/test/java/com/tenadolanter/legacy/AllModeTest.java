package com.tenadolanter.legacy;

import com.tenadolanter.response.support.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "tenadolanter.response.mode=all",
        "tenadolanter.response.error-controller-enabled=false"
})
class AllModeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void unannotatedControllerIsWrapped() {
        ResponseEntity<String> response = restTemplate.getForEntity("/legacy/text", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"legacy\"");
        assertThat(response.getBody()).contains("\"OK\"");
    }

    @Test
    void rawEndpointStaysHttpError() {
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/raw", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("raw");

        ResponseEntity<String> error = restTemplate.getForEntity("/demo/raw-boom", String.class);
        assertThat(error.getStatusCode().value()).isEqualTo(500);
        assertThat(error.getBody()).doesNotContain("\"code\":500");
    }

    @Test
    void unannotatedControllerExceptionIsRewritten() {
        ResponseEntity<String> response = restTemplate.getForEntity("/legacy/boom", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).contains("Internal server error");
        assertThat(response.getBody()).doesNotContain("legacy down");
    }
}
