package com.tenadolanter.response.support;

import com.tenadolanter.response.BusinessException;
import com.tenadolanter.response.CommonResultCode;
import com.tenadolanter.response.Result;
import com.tenadolanter.response.annotation.RawResponse;
import com.tenadolanter.response.annotation.ResultResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Test controller. It avoids javax/jakarta imports and post-Java-8 syntax so the same tests run on Boot 2 and Boot 3.
 */
@RestController
@ResultResponse
@RequestMapping("/demo")
@SuppressWarnings("unused")
public class DemoController {

    private static final MethodParameter CREATE_USER = createUserParameter();

    @GetMapping("/user")
    public Map<String, String> user() {
        return Collections.singletonMap("name", "tenado");
    }

    @GetMapping("/text")
    public String text() {
        return "hello";
    }

    @GetMapping("/raw")
    @RawResponse
    public String raw() {
        return "raw";
    }

    @GetMapping("/raw-boom")
    @RawResponse
    public String rawBoom() {
        throw new IllegalStateException("raw down");
    }

    @GetMapping("/explicit")
    public Result<String> explicit() {
        return Result.ok("explicit");
    }

    @GetMapping("/entity")
    public ResponseEntity<Map<String, String>> entity() {
        return ResponseEntity.ok(Collections.singletonMap("name", "entity"));
    }

    @GetMapping("/created")
    public ResponseEntity<String> created() {
        return ResponseEntity.status(HttpStatus.CREATED).body("new");
    }

    @GetMapping("/biz")
    public String biz() {
        throw new BusinessException(40401, "User not found");
    }

    @GetMapping("/http-biz")
    public String httpBiz() {
        throw new BusinessException(CommonResultCode.NOT_FOUND, "User not found");
    }

    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException("db down");
    }

    @PostMapping("/users")
    public String create(@RequestBody CreateUser request) throws MethodArgumentNotValidException {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(request, "createUser");
            bindingResult.addError(new FieldError("createUser", "name", "must not be blank"));
            throw new MethodArgumentNotValidException(CREATE_USER, bindingResult);
        }
        return request.getName();
    }

    private static MethodParameter createUserParameter() {
        try {
            return new MethodParameter(DemoController.class.getDeclaredMethod("create", CreateUser.class), 0);
        }
        catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static class CreateUser {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
