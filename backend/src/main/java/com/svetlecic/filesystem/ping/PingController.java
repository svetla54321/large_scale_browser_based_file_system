package com.svetlecic.filesystem.ping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PingController {

    @GetMapping("/ping")
    PingResponse ping() {
        return new PingResponse("pong");
    }

    record PingResponse(String message) {
    }
}
