package app.vercel.jalalahmad.log_query_service.service;

import app.vercel.jalalahmad.log_query_service.model.IndexRequestDTO;
import app.vercel.jalalahmad.log_query_service.model.IndexedLog;
import app.vercel.jalalahmad.log_query_service.repository.IndexedLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(LogIndexingService.class);

    @Autowired
    private IndexedLogRepository indexedLogRepository;

    @Transactional
    public IndexedLog indexLog(IndexRequestDTO indexRequest) {
        try {
            logger.info("Indexing log with ID: {}", indexRequest.getId());

            // Check if already indexed
            if (indexedLogRepository.existsById(indexRequest.getId())) {
                logger.debug("Log {} already indexed, skipping", indexRequest.getId());
                return indexedLogRepository.findById(indexRequest.getId()).orElse(null);
            }

            // Create indexed log (with custom searchable values if present)
            IndexedLog indexedLog = new IndexedLog(
                    indexRequest.getId(),
                    indexRequest.getSourceApp(),
                    indexRequest.getLevel(),
                    indexRequest.getMessage(),
                    indexRequest.getTraceId(),
                    indexRequest.getTimestamp(),
                    indexRequest.getMetadata(),
                    indexRequest.getSearchableCustomValues()
            );

            // Save to database
            IndexedLog savedLog = indexedLogRepository.save(indexedLog);

            logger.info("Successfully indexed log ID: {}", savedLog.getLogId());
            return savedLog;

        } catch (Exception e) {
            logger.error("Failed to index log {}: {}", indexRequest.getId(), e.getMessage());
            throw new RuntimeException("Failed to index log: " + e.getMessage(), e);
        }
    }

    public IndexedLog getLogById(String logId) {
        return indexedLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Indexed log not found with ID: " + logId));
    }
}