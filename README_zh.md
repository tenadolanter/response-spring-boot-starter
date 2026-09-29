# response-spring-boot-starter

[English](README.md)

Spring Boot 统一响应与异常处理组件。引入后，Controller 可以直接返回业务对象，成功和失败都会输出同一种 JSON。

要求 JDK 8 及以上。同一个 jar 同时适用于 Spring Boot 2（`javax.servlet`）和 Spring Boot 3（`jakarta.servlet`），无需区分版本引入。

构建默认按 JDK 8、Spring Boot 2.7 编译；`mvn test -Pboot3` 可用 Spring Boot 3 再跑一遍同一套测试。

```xml
<dependency>
    <groupId>com.tenadolanter</groupId>
    <artifactId>response-spring-boot-starter</artifactId>
    <version>0.0.1</version>
</dependency>
```

成功响应：

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

`mode` 有两种：

- `annotation`（默认）：只有标了 `@ResultResponse` 的类或方法才包装返回值并转换异常，已有接口不用改。注解可以放在类上，也可以只放在新方法上。
- `all`：全部 Controller 都走统一响应。给服务端调用的接口标 `@RawResponse`。

客户端接口标 `@ResultResponse`：成功和失败都使用 `code`、`message` 结构。服务端接口标 `@RawResponse`：成功时直接返回数据，失败时返回 HTTP 状态码，不套这层结构。类上已经标了 `@ResultResponse` 时，单个方法仍可再标 `@RawResponse`。下载、静态资源、`byte[]`、`Resource` 也不会包装。默认跳过 `/actuator/**` 和 Swagger 路径。未进入 Controller 的错误（例如 404）默认不改写，需要时再打开 `error-controller-enabled`。

业务码如果本身是 400 到 599，会同时作为 HTTP 状态码；像 `40001` 这种业务码仍然返回 HTTP 200，结果看 `body.code`。未知异常只返回通用提示，堆栈写在服务端日志里。

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

`fields` 用来对齐已有项目的 `code`、`message`、`data` 字段名。例如提示字段叫 `msg` 时，把 `message` 改成 `msg`。某一项留空则不输出该字段。`timestamp` 固定输出。响应体里的 trace 字段名使用 `trace-mdc-key`，请求头使用 `trace-header`。

`always-ok: true` 时，异常响应的 HTTP 状态码也固定为 200。应用自己的 `@RestControllerAdvice` 会先处理它声明的异常。
