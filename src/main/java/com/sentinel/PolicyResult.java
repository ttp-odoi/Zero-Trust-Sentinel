package com.sentinel;

public record PolicyResult(
        boolean allowed,
        String decisionCode,
        String reason,
        String correlationId
) {}