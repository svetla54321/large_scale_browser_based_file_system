package com.svetlecic.filesystem.ping.api;

import com.svetlecic.filesystem.ping.dto.PingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ping")
@RestController
class PingController {

    @Operation(summary = "Ping the API")
    @ApiResponse(responseCode = "200", description = "API is reachable")
    @GetMapping("/ping")
    PingResponse ping() {
        return new PingResponse("pong");
    }
}
