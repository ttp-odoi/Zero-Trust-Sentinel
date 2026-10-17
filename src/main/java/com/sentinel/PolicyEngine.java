package com.sentinel;

import java.util.Map;

public class PolicyEngine {
    // Simulated Entra ID Directory containing user security group memberships
    private static final Map<String, String> ENTRA_DIRECTORY = Map.of(
            "usr_tech_8842", "Grp_Infrastructure_Admins",
            "usr_guest_102", "Grp_Contractors"
    );

    // Zone Access Rules mapping requested zones to required Entra ID security groups
    private static final Map<String, String> ZONE_PERMISSIONS = Map.of(
            "Server_Room_Alpha", "Grp_Infrastructure_Admins",
            "Lobby_Main", "Grp_Contractors"
    );

    /**
     * Zero Trust Policy Evaluator:
     * 1. Explicit Identity Check
     * 2. Least Privilege / RBAC Match
     * 3. Zone Authorization
     */
    public PolicyResult evaluate(RingEvent event) {
        String userId = event.userClaim().userId();
        String requestedZone = event.userClaim().requestedZone();
        String correlationId = "corr_" + event.eventId().replace("evt_", "");

        // Check 1: User Identity exists in directory
        String userGroup = ENTRA_DIRECTORY.get(userId);
        if (userGroup == null) {
            return new PolicyResult(false, "DENY_UNKNOWN_IDENTITY", "User ID not found in Entra ID directory.", correlationId);
        }

        // Check 2: Zone exists in security policies
        String requiredGroup = ZONE_PERMISSIONS.get(requestedZone);
        if (requiredGroup == null) {
            return new PolicyResult(false, "DENY_INVALID_ZONE", "Requested zone does not have an active access policy.", correlationId);
        }

        // Check 3: RBAC verification (User group must match zone requirement)
        if (!userGroup.equals(requiredGroup)) {
            return new PolicyResult(false, "DENY_INSUFFICIENT_PRIVILEGE",
                    "User group '" + userGroup + "' lacks required membership '" + requiredGroup + "' for zone " + requestedZone + ".",
                    correlationId);
        }

        // All checks passed
        return new PolicyResult(true, "ALLOW_ACCESS_GRANTED", "Identity verified and RBAC checks passed.", correlationId);
    }
}