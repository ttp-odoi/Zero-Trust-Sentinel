package com.sentinel;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserClaim(
        @JsonProperty("user_id") String userId,
        @JsonProperty("access_card_id") String accessCardId,
        @JsonProperty("requested_zone") String requestedZone
) {}