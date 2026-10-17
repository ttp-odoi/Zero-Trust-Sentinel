package com.sentinel;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    private static final String SHARED_SECRET = "SentinelSuperSecretKey2026!";

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   ZERO-TRUST SENTINEL - ALEXA+ MCP INTEGRATION  ");
        System.out.println("=================================================\n");

        EventQueueProcessor queueProcessor = new EventQueueProcessor(5);
        PolicyEngine policyEngine = new PolicyEngine();
        AlexaMcpHandler mcpHandler = new AlexaMcpHandler(queueProcessor, policyEngine, SHARED_SECRET);

        // Print MCP Tool Definition Schema
        System.out.println("--- MCP Tool Schema (Exposed to Alexa+) ---");
        System.out.println(mcpHandler.getMcpToolDefinition());

        try {
            // Read sample payload
            String rawJson = Files.readString(Paths.get("ring-event.json"));
            String validSignature = HmacValidator.calculateHmac(rawJson, SHARED_SECRET);

            System.out.println("--- Simulating Alexa+ MCP Tool Invocation ---");
            String mcpResponse = mcpHandler.handleMcpToolCall(rawJson, validSignature);

            System.out.println("--- Response Delivered back to Alexa+ Agent ---");
            System.out.println(mcpResponse);

        } catch (Exception e) {
            System.err.println("[ERROR] MCP Test failed: " + e.getMessage());
        }
    }
}