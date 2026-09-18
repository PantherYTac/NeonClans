package com.neonclans.manager;

import com.neonclans.NeonClans;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class DiscordWebhookManager {
    private final NeonClans plugin;

    public DiscordWebhookManager(NeonClans plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("features.discord.enabled", false);
    }

    public void sendEmbed(String title, String description, int colorDecimal) {
        if (!isEnabled()) return;
        String webhookUrl = plugin.getConfig().getString("features.discord.webhook-url", "");
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.contains("your-webhook-id")) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "NeonClans-Plugin");
                connection.setDoOutput(true);

                String jsonPayload = "{"
                        + "\"embeds\": [{"
                        + "\"title\": \"" + escapeJson(title) + "\","
                        + "\"description\": \"" + escapeJson(description) + "\","
                        + "\"color\": " + colorDecimal
                        + "}]"
                        + "}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) {
                    plugin.getLogger().warning("Discord Webhook responded with HTTP code: " + code);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send Discord Webhook embed", e);
            }
        });
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
