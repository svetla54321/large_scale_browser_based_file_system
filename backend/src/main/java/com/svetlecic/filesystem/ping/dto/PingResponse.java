package com.svetlecic.filesystem.ping.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Answer to a ping")
public record PingResponse(@Schema(description = "Always \"pong\"", example = "pong") String message) {
}
