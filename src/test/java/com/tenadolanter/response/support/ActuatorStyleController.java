package com.tenadolanter.response.support;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actuator")
@SuppressWarnings("unused")
public class ActuatorStyleController {

    @GetMapping("/info")
    public String info() {
        return "alive";
    }
}
