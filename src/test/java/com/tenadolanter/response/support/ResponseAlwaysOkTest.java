package com.tenadolanter.response.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "tenadolanter.response.always-ok=true",
        "tenadolanter.response.success-code=200",
        "tenadolanter.response.success-message=Success"
})
class ResponseAlwaysOkTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void successCodeFollowsConfiguration() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/user", String.class);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.get("code").asInt()).isEqualTo(200);
        assertThat(body.get("message").asText()).isEqualTo("Success");
    }

    @Test
    void errorsStayHttpOk() throws Exception {
        ResponseEntity<String> boom = restTemplate.getForEntity("/demo/boom", String.class);
        JsonNode boomBody = objectMapper.readTree(boom.getBody());
        assertThat(boom.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(boomBody.get("code").asInt()).isEqualTo(500);

        ResponseEntity<String> missing = restTemplate.getForEntity("/demo/http-biz", String.class);
        JsonNode missingBody = objectMapper.readTree(missing.getBody());
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(missingBody.get("code").asInt()).isEqualTo(404);
    }
}
