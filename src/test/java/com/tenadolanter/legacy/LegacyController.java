package com.tenadolanter.legacy;

import com.tenadolanter.response.annotation.ResultResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Simulates endpoints that already existed before adoption. Only methods marked with {@link ResultResponse} use the unified response.
 */
@RestController
@RequestMapping("/legacy")
@SuppressWarnings("unused")
public class LegacyController {

    @GetMapping("/text")
    public String text() {
        return "legacy";
    }

    @GetMapping("/boom")
    public String boom() {
        throw new IllegalStateException("legacy down");
    }

    @GetMapping("/adopted")
    @ResultResponse
    public Map<String, String> adopted() {
        return Collections.singletonMap("name", "adopted");
    }

    @GetMapping("/adopted-boom")
    @ResultResponse
    public String adoptedBoom() {
        throw new IllegalStateException("adopted down");
    }
}
