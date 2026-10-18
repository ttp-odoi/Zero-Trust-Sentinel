package com.sentinel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlexaMcpHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final EventQueueProcessor queueProcessor;
    private final PolicyEngine policyEngine;
    private final String sharedSecret;

    public AlexaMcpHandler(EventQueueProcessor queueProcessor, PolicyEngine policyEngine, String sharedSecret) {
        this.queueProcessor = queueProcessor;
        this.policyEngine = policyEngine;
        this.sharedSecret = sharedSecret;
    }

    /**
     * Defines the MCP Tool Definition Schema exposed to Alexa+
     */
    public String getMcpToolDefinition() {
        return """
            {
              "name": "verify_door_access",
              "description": "Evaluates a Zero Trust physical access request from a Ring device trigger.",
              "inputSchema": {
                "type": "object",
                "properties": {
                  "event_id": { "type": "string" },
                  "device_id": { "type": "string" },
                  "user_id": { "type": "string" },
                  "access_card_id": { "type": "string" },
                  "requested_zone": { "type": "string" }
                },
                "required": ["event_id", "device_id", "user_id", "requested_zone"]
              }
            }
            """;
    }

    /**
     * Processes an incoming MCP Tool Execution call from Alexa+
     * and speaks the resulting decision aloud.
     */
    public String handleMcpToolCall(String rawJsonPayload, String providedSignature) {
        ObjectNode responseNode = mapper.createObjectNode();

        try {
            // 1. [S1] HMAC Integrity Check
            if (!HmacValidator.isValidSignature(rawJsonPayload, sharedSecret, providedSignature)) {
                String errorSpeech = "Security Alert: Access request failed payload signature verification.";
                responseNode.put("status", "REJECTED");
                responseNode.put("alexa_speech", errorSpeech);

                TextToSpeech.speak(errorSpeech);
                return mapper.writeValueAsString(responseNode);
            }

            // 2. Parse Ring Event payload
            RingEvent event = mapper.readValue(rawJsonPayload, RingEvent.class);

            // 3. [D1] Ingest into DoS Queue
            if (!queueProcessor.offerEvent(event)) {
                String rateLimitSpeech = "System busy: Too many simultaneous entry requests. Please try again.";
                responseNode.put("status", "RATE_LIMITED");
                responseNode.put("alexa_speech", rateLimitSpeech);

                TextToSpeech.speak(rateLimitSpeech);
                return mapper.writeValueAsString(responseNode);
            }

            // 4. [E1] Evaluate Policy Engine
            RingEvent dequeuedEvent = queueProcessor.takeEvent();
            PolicyResult result = policyEngine.evaluate(dequeuedEvent);

            // 5. Build MCP Response for Alexa+ Agent
            responseNode.put("status", result.allowed() ? "SUCCESS" : "DENIED");
            responseNode.put("decision_code", result.decisionCode());
            responseNode.put("correlation_id", result.correlationId());

            String speechOutput = result.allowed()
                    ? "Access granted for zone " + event.userClaim().requestedZone() + ". Door unlocked."
                    : "Access denied. " + result.reason();

            responseNode.put("alexa_speech", speechOutput);

            // 🔊 Trigger Windows Text-To-Speech Engine
            TextToSpeech.speak(speechOutput);

        } catch (Exception e) {
            String errSpeech = "An internal error occurred during verification.";
            responseNode.put("status", "ERROR");
            responseNode.put("alexa_speech", errSpeech);

            TextToSpeech.speak(errSpeech);
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseNode);
        } catch (Exception e) {
            return "{\"status\": \"CRITICAL_ERROR\"}";
        }
    }
}