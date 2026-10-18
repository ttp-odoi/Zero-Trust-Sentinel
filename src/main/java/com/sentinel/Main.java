package com.sentinel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Scanner;
import java.util.UUID;

public class Main {
    private static final String SHARED_SECRET = "SentinelSuperSecretKey2026!";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ObjectMapper mapper = new ObjectMapper();

        // Core Pipeline Components
        EventQueueProcessor queueProcessor = new EventQueueProcessor(5);
        PolicyEngine policyEngine = new PolicyEngine();
        AlexaMcpHandler mcpHandler = new AlexaMcpHandler(queueProcessor, policyEngine, SHARED_SECRET);

        System.out.println("=========================================================");
        System.out.println("   RING DOORBELL & ZERO TRUST SENTINEL SIMULATOR         ");
        System.out.println("=========================================================");
        System.out.println("Available Entra ID Users:");
        System.out.println("  1. usr_tech_8842  (Group: Grp_Infrastructure_Admins)");
        System.out.println("  2. usr_guest_102  (Group: Grp_Contractors)");
        System.out.println("  3. Any fake ID   (e.g., usr_attacker_999)");
        System.out.println("\nAvailable Zones:");
        System.out.println("  - Server_Room_Alpha (Requires: Grp_Infrastructure_Admins)");
        System.out.println("  - Lobby_Main        (Requires: Grp_Contractors)");
        System.out.println("=========================================================\n");

        while (true) {
            System.out.print("\n🔔 [VIRTUAL RING] Press Enter to ring doorbell (or type 'exit' to quit): ");
            String input = scanner.nextLine().trim();
            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Exiting Sentinel Simulator. System offline.");
                break;
            }

            // 1. Gather Interactive User Claim Data
            System.out.print("👤 Enter User ID: ");
            String userId = scanner.nextLine().trim();

            System.out.print("🚪 Enter Requested Zone: ");
            String requestedZone = scanner.nextLine().trim();

            // 2. Build Dynamic Ring Event JSON Payload
            String eventId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
            ObjectNode userClaimNode = mapper.createObjectNode()
                    .put("user_id", userId)
                    .put("access_card_id", "badge_" + userId)
                    .put("requested_zone", requestedZone);

            ObjectNode ringEventNode = mapper.createObjectNode()
                    .put("event_id", eventId)
                    .put("device_id", "ring_doorbell_server_room_01")
                    .put("timestamp", java.time.Instant.now().toString())
                    .put("event_type", "motion_ding");
            ringEventNode.set("user_claim", userClaimNode);

            try {
                String rawJson = mapper.writeValueAsString(ringEventNode);

                // 3. Compute HMAC Signature
                String hmacSignature = HmacValidator.calculateHmac(rawJson, SHARED_SECRET);

                System.out.println("\n--- ⚡ Processing Payload via Alexa+ MCP Agent ---");

                // 4. Send Request through Alexa+ MCP Handler
                String mcpResponseJson = mcpHandler.handleMcpToolCall(rawJson, hmacSignature);

                // 5. Display Final Decision
                System.out.println("--- 📢 Alexa+ Voice & Security Output ---");
                System.out.println(mcpResponseJson);

            } catch (Exception e) {
                System.err.println("[ERROR] Failed to process event: " + e.getMessage());
            }
        }
        scanner.close();
    }
}