package com.sentinel;

public class TextToSpeech {
    /**
     * Uses Windows System.Speech synthesizer to speak the provided text aloud.
     */
    public static void speak(String text) {
        if (text == null || text.isBlank()) return;

        // Run in a separate thread so audio playback does not block the CLI terminal
        new Thread(() -> {
            try {
                // Sanitize text for PowerShell command line execution
                String sanitized = text.replace("'", "").replace("\"", "");
                String psScript = String.format(
                        "Add-Type -AssemblyName System.Speech; " +
                                "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                                "$synth.Speak('%s');",
                        sanitized
                );

                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", psScript);
                Process process = pb.start();
                process.waitFor();
            } catch (Exception e) {
                System.err.println("[TTS WARNING] Audio playback failed: " + e.getMessage());
            }
        }).start();
    }
}