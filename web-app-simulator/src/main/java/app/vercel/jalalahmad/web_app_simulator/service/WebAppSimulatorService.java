package app.vercel.jalalahmad.web_app_simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WebAppSimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(WebAppSimulatorService.class);

    @Value("${simulator.base-url}")
    private String baseUrl;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private final String[] users = {"alice", "bob", "charlie", "diana", "eve"};
    private final String[] actions = {"login", "view_product", "add_to_cart", "checkout", "payment", "logout", "search", "view_profile"};
    private final String[] products = {"laptop", "phone", "tablet", "headphones", "monitor", "keyboard"};

    private int logCounter = 0;

    @Scheduled(fixedRateString = "${simulator.rate}")
    public void generateLog() {
        logCounter++;

        // Randomly decide log level (mostly INFO, sometimes ERROR/WARN)
        String level = getRandomLevel();
        String user = users[random.nextInt(users.length)];
        String action = actions[random.nextInt(actions.length)];
        String product = products[random.nextInt(products.length)];

        // Create log message based on action
        String message = generateLogMessage(user, action, product, level);

        // Create metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", user);
        metadata.put("action", action);
        metadata.put("sessionId", UUID.randomUUID().toString().substring(0, 8));
        metadata.put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        metadata.put("pageLoadTime", String.format("%.2f", 0.5 + random.nextDouble() * 2));

        if (action.contains("product") || action.contains("cart")) {
            metadata.put("productId", product);
            metadata.put("price", String.format("%.2f", 99 + random.nextDouble() * 900));
        }

        // Create log request
        Map<String, Object> logRequest = new HashMap<>();
        logRequest.put("sourceApp", "web-application");
        logRequest.put("level", level);
        logRequest.put("message", message);
        logRequest.put("traceId", "trace-web-" + logCounter);
        logRequest.put("metadata", metadata);

        // Send to ingest service via Gateway
        sendLog(logRequest);
    }

    private String getRandomLevel() {
        int rand = random.nextInt(100);
        if (rand < 70) return "INFO";      // 70% INFO
        else if (rand < 85) return "WARN"; // 15% WARN
        else if (rand < 95) return "ERROR";// 10% ERROR
        else return "DEBUG";               // 5% DEBUG
    }

    private String generateLogMessage(String user, String action, String product, String level) {
        switch (action) {
            case "login":
                return level.equals("ERROR")
                        ? String.format("Failed login attempt for user '%s' - invalid credentials", user)
                        : String.format("User '%s' logged in successfully", user);

            case "view_product":
                return String.format("User '%s' viewed product: %s", user, product);

            case "add_to_cart":
                return String.format("User '%s' added %s to cart", user, product);

            case "checkout":
                return level.equals("ERROR")
                        ? String.format("Checkout failed for user '%s' - payment declined", user)
                        : String.format("User '%s' completed checkout successfully", user);

            case "payment":
                return level.equals("ERROR")
                        ? String.format("Payment processing failed for user '%s'", user)
                        : String.format("Payment of $%.2f processed for user '%s'",
                        99 + random.nextDouble() * 900, user);

            default:
                return String.format("User '%s' performed action: %s", user, action);
        }
    }

    private void sendLog(Map<String, Object> logRequest) {
        try {
            webClient.post()
                    .uri(baseUrl + "/ingest/log")
                    .header("Content-Type", "application/json")
                    .bodyValue(logRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(response -> {
                        logger.info("Generated web log: {}", logRequest.get("message"));
                    })
                    .doOnError(error -> {
                        logger.error("Failed to send log: {}", error.getMessage());
                    })
                    .subscribe();
        } catch (Exception e) {
            logger.error("Error sending log: {}", e.getMessage());
        }
    }

    // Generate batch of logs on startup
    @Scheduled(initialDelay = 2000, fixedDelay = Long.MAX_VALUE)
    public void generateInitialLogs() {
        logger.info("Web App Simulator started. Generating initial logs...");
        for (int i = 0; i < 5; i++) {
            generateLog();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}