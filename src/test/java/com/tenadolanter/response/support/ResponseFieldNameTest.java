package com.tenadolanter.response.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenadolanter.response.TraceIds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "tenadolanter.response.fields.message=msg",
        "tenadolanter.response.trace-mdc-key=requestId"
})
class ResponseFieldNameTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void writesConfiguredFieldNames() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TraceIds.HEADER, "abc-123");
        ResponseEntity<String> response = restTemplate.exchange("/demo/user", HttpMethod.GET, new HttpEntity<String>(headers), String.class);
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.get("code").asInt()).isZero();
        assertThat(body.get("msg").asText()).isEqualTo("OK");
        assertThat(body.get("data").get("name").asText()).isEqualTo("tenado");
        assertThat(body.get("requestId").asText()).isEqualTo("abc-123");
        assertThat(body.get("timestamp").asLong()).isPositive();
        assertThat(body.has("message")).isFalse();
        assertThat(body.has("traceId")).isFalse();
    }

    @Test
    void failureUsesTheSameFieldNames() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/biz", String.class);
        JsonNode body = objectMapper.readTree(response.getBody());

        assertThat(body.get("code").asInt()).isEqualTo(40401);
        assertThat(body.get("msg").asText()).isEqualTo("User not found");
        assertThat(body.get("timestamp").asLong()).isPositive();
        assertThat(body.has("message")).isFalse();
    }
}
