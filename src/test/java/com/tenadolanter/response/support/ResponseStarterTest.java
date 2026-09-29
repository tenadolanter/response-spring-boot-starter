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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResponseStarterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void wrapsObjectAndCarriesTraceId() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TraceIds.HEADER, "abc-123");
        ResponseEntity<String> response = restTemplate.exchange("/demo/user", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        JsonNode body = read(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(TraceIds.HEADER)).isEqualTo("abc-123");
        assertThat(body.get("code").asInt()).isZero();
        assertThat(body.get("message").asText()).isEqualTo("OK");
        assertThat(body.get("data").get("name").asText()).isEqualTo("tenado");
        assertThat(body.get("traceId").asText()).isEqualTo("abc-123");
        assertThat(body.get("timestamp").asLong()).isPositive();
    }

    @Test
    void wrapsStringAsJson() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/text", String.class);
        JsonNode body = read(response);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().includes(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(body.get("data").asText()).isEqualTo("hello");
    }

    @Test
    void skipsRawResponseAndExcludedPath() {
        ResponseEntity<String> raw = restTemplate.getForEntity("/demo/raw", String.class);
        ResponseEntity<String> actuator = restTemplate.getForEntity("/actuator/info", String.class);
        assertThat(raw.getBody()).isEqualTo("raw");
        assertThat(actuator.getBody()).isEqualTo("alive");

        ResponseEntity<String> rawError = restTemplate.getForEntity("/demo/raw-boom", String.class);
        assertThat(rawError.getStatusCode().value()).isEqualTo(500);
        if (rawError.getBody() != null) {
            assertThat(rawError.getBody()).doesNotContain("\"code\":500");
            assertThat(rawError.getBody()).doesNotContain("raw down");
        }
    }

    @Test
    void doesNotWrapResultTwice() throws Exception {
        JsonNode body = read(restTemplate.getForEntity("/demo/explicit", String.class));
        assertThat(body.get("data").asText()).isEqualTo("explicit");
        assertThat(body.get("data").isObject()).isFalse();
    }

    @Test
    void wrapsResponseEntity() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/entity", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(response).get("data").get("name").asText()).isEqualTo("entity");

        ResponseEntity<String> created = restTemplate.getForEntity("/demo/created", String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(read(created).get("data").asText()).isEqualTo("new");
    }

    @Test
    void businessCodeOutsideHttpRangeKeepsHttpOk() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/biz", String.class);
        JsonNode body = read(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.get("code").asInt()).isEqualTo(40401);
        assertThat(body.get("message").asText()).isEqualTo("User not found");
        assertThat(body.get("data").isNull()).isTrue();
    }

    @Test
    void httpBusinessCodeUsesHttpStatus() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/http-biz", String.class);
        JsonNode body = read(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body.get("code").asInt()).isEqualTo(404);
        assertThat(body.get("message").asText()).isEqualTo("User not found");
    }

    @Test
    void hidesUnknownExceptionMessage() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/demo/boom", String.class);
        JsonNode body = read(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.get("code").asInt()).isEqualTo(500);
        assertThat(body.get("message").asText()).isEqualTo("Internal server error");
        assertThat(response.getBody()).doesNotContain("db down");
    }

    @Test
    void validationFailureUsesBadRequest() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity("/demo/users", new HttpEntity<>("{\"name\":\"\"}", headers), String.class);
        JsonNode body = read(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.get("code").asInt()).isEqualTo(400);
        assertThat(body.get("message").asText()).contains("must not be blank");
    }

    @Test
    void missingHandlerUsesUnifiedNotFound() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/missing", String.class);
        JsonNode body = read(response);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body.get("code").asInt()).isEqualTo(404);
        assertThat(body.get("message").asText()).isEqualTo("Not found");
        assertThat(body.get("traceId").asText()).isNotBlank();
    }

    private JsonNode read(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }
}
