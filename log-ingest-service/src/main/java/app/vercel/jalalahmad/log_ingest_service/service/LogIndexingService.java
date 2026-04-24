package app.vercel.jalalahmad.log_ingest_service.service;

import app.vercel.jalalahmad.log_ingest_service.model.LogEntry;
import app.vercel.jalalahmad.log_ingest_service.repository.LogEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(LogIndexingService.class);

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    @Qualifier("webClientBuilder")  // Use NON-load-balanced version
    private WebClient.Builder webClientBuilder;

    @Async
    @Transactional
    public void triggerIndexingAsync(LogEntry logEntry) {
        triggerIndexingAsync(logEntry, null);
    }

    @Async
    @Transactional
    public void triggerIndexingAsync(LogEntry logEntry, List<String> searchableCustomValues) {
        Long logId = logEntry.getId();

        try {
            logger.info("🚀 Starting async indexing for log id: {}", logId);

            // Prepare the indexing request
            Map<String, Object> indexRequest = new HashMap<>();
            indexRequest.put("id", logId.toString());
            indexRequest.put("sourceApp", logEntry.getSourceApp());
            indexRequest.put("level", logEntry.getLevel());
            indexRequest.put("message", logEntry.getMessage());
            indexRequest.put("timestamp", logEntry.getTimestamp().toString());
            indexRequest.put("traceId", logEntry.getTraceId());
            indexRequest.put("metadata", logEntry.getMetadata());

            if (searchableCustomValues != null && !searchableCustomValues.isEmpty()) {
                indexRequest.put("searchableCustomValues", searchableCustomValues);
            }

            // Use the WebClient builder (non-load-balanced)
            webClientBuilder.build()
                    .post()
                    .uri("http://localhost:8080/query/index")  // Direct Gateway URL
                    .header("Content-Type", "application/json")
                    .bodyValue(indexRequest)
                    .retrieve()
                    .onStatus(status -> status.isError(), response -> {
                        logger.error("❌ Failed to index log {}. Status: {}", logId, response.statusCode());
                        markAsIndexingFailed(logId);
                        return Mono.error(new RuntimeException("Indexing failed with status: " + response.statusCode()));
                    })
                    .bodyToMono(String.class)
                    .doOnSuccess(response -> {
                        logger.info("✅ Successfully indexed log id: {}", logId);
                        markAsIndexed(logId);
                    })
                    .doOnError(error -> {
                        logger.error("❌ Error indexing log {}: {}", logId, error.getMessage());
                        markAsIndexingFailed(logId);
                    })
                    .subscribe();

        } catch (Exception e) {
            logger.error("❌ Failed to trigger indexing for log {}: {}", logId, e.getMessage());
            markAsIndexingFailed(logId);
        }
    }

    @Transactional
    private void markAsIndexed(Long logId) {
        try {
            logEntryRepository.findById(logId).ifPresent(logEntry -> {
                logEntry.setIndexed(true);
                logEntryRepository.save(logEntry);
                logger.info("📝 Updated log {} as indexed=true in database", logId);
            });
        } catch (Exception e) {
            logger.error("Failed to update indexed status for log {}: {}", logId, e.getMessage());
        }
    }

    @Transactional
    private void markAsIndexingFailed(Long logId) {
        try {
            logger.warn("⚠️ Marking log {} as indexing failed", logId);
        } catch (Exception e) {
            logger.error("Failed to mark log {} as indexing failed: {}", logId, e.getMessage());
        }
    }
}