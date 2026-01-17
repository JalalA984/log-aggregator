package app.vercel.jalalahmad.payment_simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentSimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentSimulatorService.class);

    @Value("${simulator.base-url}")
    private String baseUrl;

    @Value("${payment.success-rate}")
    private int successRate;

    @Value("${payment.min-amount}")
    private double minAmount;

    @Value("${payment.max-amount}")
    private double maxAmount;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private final String[] paymentMethods = {"VISA", "MASTERCARD", "PAYPAL", "APPLE_PAY", "GOOGLE_PAY", "STRIPE"};
    private final String[] currencies = {"USD", "EUR", "GBP", "CAD", "AUD"};
    private final String[] countries = {"US", "UK", "CA", "AU", "DE", "FR", "JP"};
    private final String[] failureReasons = {
            "insufficient_funds", "card_declined", "expired_card", "invalid_cvv",
            "suspected_fraud", "network_error", "timeout", "currency_not_supported"
    };

    private int paymentCounter = 0;
    private int successCount = 0;
    private int failureCount = 0;

    @Scheduled(fixedRateString = "${simulator.rate}")
    public void generatePaymentLog() {
        paymentCounter++;

        // Generate payment details
        String paymentId = "pay-" + String.format("%06d", paymentCounter);
        double amount = minAmount + (random.nextDouble() * (maxAmount - minAmount));
        String currency = currencies[random.nextInt(currencies.length)];
        String paymentMethod = paymentMethods[random.nextInt(paymentMethods.length)];
        String country = countries[random.nextInt(countries.length)];

        // Determine if payment succeeds based on success rate
        boolean isSuccess = random.nextInt(100) < successRate;
        String level = isSuccess ? "INFO" : "ERROR";

        // Create log message
        String message = generatePaymentMessage(paymentId, amount, currency, paymentMethod, isSuccess);

        // Create metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("paymentId", paymentId);
        metadata.put("amount", String.format("%.2f", amount));
        metadata.put("currency", currency);
        metadata.put("paymentMethod", paymentMethod);
        metadata.put("country", country);
        metadata.put("merchantId", "merchant_" + (1000 + random.nextInt(9000)));
        metadata.put("transactionTime", LocalDateTime.now().toString());

        if (!isSuccess) {
            metadata.put("failureReason", failureReasons[random.nextInt(failureReasons.length)]);
            metadata.put("retryCount", String.valueOf(random.nextInt(3)));
            failureCount++;
        } else {
            metadata.put("authorizationCode", generateAuthCode());
            metadata.put("processor", "payment-processor-" + random.nextInt(5));
            successCount++;
        }

        // Additional metrics
        metadata.put("processingTimeMs", String.format("%.0f", 50 + random.nextDouble() * 200));
        metadata.put("successRate", String.format("%.1f%%", (successCount * 100.0 / paymentCounter)));

        // Create log request
        Map<String, Object> logRequest = new HashMap<>();
        logRequest.put("sourceApp", "payment-service");
        logRequest.put("level", level);
        logRequest.put("message", message);
        logRequest.put("traceId", "trace-payment-" + paymentCounter);
        logRequest.put("metadata", metadata);

        // Send to ingest service via Gateway
        sendLog(logRequest);

        // Log stats periodically
        if (paymentCounter % 10 == 0) {
            logger.info("Payment Stats: {} total, {} successful ({}%), {} failed",
                    paymentCounter, successCount,
                    String.format("%.1f", (successCount * 100.0 / paymentCounter)),
                    failureCount);
        }
    }

    private String generatePaymentMessage(String paymentId, double amount, String currency,
                                          String paymentMethod, boolean isSuccess) {
        String formattedAmount = String.format("%.2f %s", amount, currency);

        if (isSuccess) {
            return String.format("Payment %s completed successfully: %s via %s",
                    paymentId, formattedAmount, paymentMethod);
        } else {
            String reason = failureReasons[random.nextInt(failureReasons.length)]
                    .replace("_", " ");
            return String.format("Payment %s failed: %s via %s - Reason: %s",
                    paymentId, formattedAmount, paymentMethod, reason);
        }
    }

    private String generateAuthCode() {
        return String.format("AUTH%06d", random.nextInt(1000000));
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
                        logger.debug("Generated payment log: {}", logRequest.get("message"));
                    })
                    .doOnError(error -> {
                        logger.error("Failed to send payment log: {}", error.getMessage());
                    })
                    .subscribe();
        } catch (Exception e) {
            logger.error("Error sending payment log: {}", e.getMessage());
        }
    }

    // Generate initial payment logs
    @Scheduled(initialDelay = 3000, fixedDelay = Long.MAX_VALUE)
    public void generateInitialPaymentLogs() {
        logger.info("Payment Service Simulator started. Generating initial payment logs...");
        for (int i = 0; i < 3; i++) {
            generatePaymentLog();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Generate a batch of payments occasionally
    @Scheduled(fixedRate = 60000)  // Every minute
    public void generatePaymentBatch() {
        int batchSize = 3 + random.nextInt(5);  // 3-7 payments
        logger.info("Generating batch of {} payments", batchSize);

        for (int i = 0; i < batchSize; i++) {
            generatePaymentLog();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Health check endpoint (simple REST controller)
    @org.springframework.web.bind.annotation.RestController
    public static class RestController {

        @org.springframework.beans.factory.annotation.Autowired
        private PaymentSimulatorService service;

        @org.springframework.web.bind.annotation.GetMapping("/health")
        public Map<String, Object> health() {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("service", "payment-simulator");
            health.put("timestamp", LocalDateTime.now().toString());
            health.put("totalPayments", service.paymentCounter);
            health.put("successfulPayments", service.successCount);
            health.put("failedPayments", service.failureCount);
            return health;
        }

        @org.springframework.web.bind.annotation.GetMapping("/stats")
        public Map<String, Object> stats() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("successRate", String.format("%.1f%%",
                    (service.successCount * 100.0 / Math.max(1, service.paymentCounter))));
            stats.put("avgAmount", String.format("%.2f",
                    service.minAmount + (service.maxAmount - service.minAmount) / 2));
            return stats;
        }
    }
}