package com.sentinel;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    private static final String SHARED_SECRET = "SentinelSuperSecretKey2026!";

    public static void main(String[] args) {
        System.out.println("=== Zero-Trust Sentinel Booting ===");

        ObjectMapper mapper = new ObjectMapper();
        EventQueueProcessor queueProcessor = new EventQueueProcessor(5);

        try {
            // 1. Load raw JSON payload string
            String rawJson = Files.readString(Paths.get("ring-event.json"));

            // 2. Generate legitimate HMAC signature for the payload
            String validSignature = HmacValidator.calculateHmac(rawJson, SHARED_SECRET);
            System.out.println("[SIGNATURE GENERATED] HMAC-SHA256: " + validSignature);

            // 3. Test 1: Legitimate Payload Validation
            boolean isLegitValid = HmacValidator.isValidSignature(rawJson, SHARED_SECRET, validSignature);
            if (isLegitValid) {
                System.out.println("[S1 HMAC PASSED] Valid payload signature confirmed!");

                // Parse and queue
                RingEvent event = mapper.readValue(rawJson, RingEvent.class);
                queueProcessor.offerEvent(event);
            }

            // 4. Test 2: Spoofed / Tampered Payload Validation
            System.out.println("\n--- Simulating Tampering Attack ---");
            String tamperedJson = rawJson.replace("usr_tech_8842", "usr_attacker_666");
            boolean isTamperedValid = HmacValidator.isValidSignature(tamperedJson, SHARED_SECRET, validSignature);

            if (!isTamperedValid) {
                System.err.println("[S1 HMAC REJECTED] Tampered payload detected! Event dropped before queue ingestion.");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Pipeline execution failure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}