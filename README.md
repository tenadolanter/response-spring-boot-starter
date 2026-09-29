# response-spring-boot-starter

[中文说明](README_zh.md)

Spring Boot component for a unified response body and exception handling. After adding it, a controller can return a domain object directly. Success and failure both use the same JSON shape.

Requires JDK 8 or later. One jar works on both Spring Boot 2 (`javax.servlet`) and Spring Boot 3 (`jakarta.servlet`). Consumers do not pick a separate artifact.

The default build compiles against JDK 8 and Spring Boot 2.7. `mvn test -Pboot3` runs the same tests on Spring Boot 3.

```xml
<dependency>
    <groupId>com.tenadolanter</groupId>
    <artifactId>response-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

Successful response:

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "timestamp": 1710000000000,
  "traceId": "abc123"
}
```

```java
@ResultResponse
@GetMapping("/users/{id}")
public User detail(@PathVariable Long id) {
    return userService.detail(id);
}

@GetMapping("/orders")
public Result<List<Order>> orders() {
    return Result.ok(orderService.list());
}

@PostMapping("/orders")
public void create() {
    throw new BusinessException(40001, "Out of stock");
}
```

`mode` has two values:

- `annotation` (default): only types or methods marked with `@ResultResponse` wrap return values and translate exceptions. Existing endpoints stay unchanged. The annotation can be placed on a type or on a single new method.
- `all`: every controller uses the unified response. Mark server-facing endpoints with `@RawResponse`.

Mark client-facing endpoints with `@ResultResponse`. Both success and failure use the `code` and `message` structure. Mark server-facing endpoints with `@RawResponse`: success returns the data directly, and failure returns an HTTP status without that structure. A method can still carry `@RawResponse` when its type already has `@ResultResponse`. Downloads, static resources, `byte[]`, and `Resource` are not wrapped. `/actuator/**` and Swagger paths are skipped by default. Errors that never reach a controller, such as 404, are left unchanged unless `error-controller-enabled` is turned on.

A business code from 400 to 599 is also used as the HTTP status. A code such as `40001` still returns HTTP 200, and the result is `body.code`. Unknown exceptions return a generic message. The stack trace stays in the server log.

```yaml
tenadolanter:
  response:
    enabled: true
    mode: annotation
    exception-handler-enabled: true
    error-controller-enabled: false
    trace-enabled: true
    trace-header: X-Trace-Id
    trace-mdc-key: traceId
    success-code: 0
    success-message: OK
    default-error-message: Internal server error
    expose-exception-message: false
    always-ok: false
    exclude-paths:
      - /actuator/**
    fields:
      code: code
      message: message
      data: data
```

`fields` renames the `code`, `message`, and `data` JSON fields to match an existing project. If the message field is called `msg`, set `message` to `msg`. A blank value omits that field. `timestamp` is always written. The trace field name in the body follows `trace-mdc-key`. The request header follows `trace-header`.

With `always-ok: true`, error responses also use HTTP 200. An application's own `@RestControllerAdvice` handles the exceptions it declares first.
