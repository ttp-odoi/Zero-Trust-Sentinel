package com.sentinel;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RingEvent(
        @JsonProperty("event_id") String eventId,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("user_claim") UserClaim userClaim
) {}