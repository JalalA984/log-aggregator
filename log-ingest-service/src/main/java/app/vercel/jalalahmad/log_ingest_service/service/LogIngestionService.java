package app.vercel.jalalahmad.log_ingest_service.service;

import app.vercel.jalalahmad.log_ingest_service.model.LogEntry;
import app.vercel.jalalahmad.log_ingest_service.model.LogRequestDTO;
import app.vercel.jalalahmad.log_ingest_service.repository.LogEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(LogIngestionService.class);

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private LogIndexingService logIndexingService;

    @Transactional
    public LogEntry ingestLog(LogRequestDTO logRequest) {
        // Validate log level
        String level = validateAndNormalizeLevel(logRequest.getLevel());

        // Create log entry
        LogEntry logEntry = new LogEntry(
                logRequest.getSourceApp(),
                level,
                logRequest.getMessage(),
                logRequest.getTraceId(),
                logRequest.getMetadata()  // Pass Map directly
        );

        // Save to database
        LogEntry savedEntry = logEntryRepository.save(logEntry);

        logger.info("Ingested log [id={}, source={}, level={}]",
                savedEntry.getId(),
                savedEntry.getSourceApp(),
                savedEntry.getLevel());

        // Asynchronously trigger indexing
        logIndexingService.triggerIndexingAsync(savedEntry);

        return savedEntry;
    }

    private String validateAndNormalizeLevel(String level) {
        if (level == null) {
            return "INFO";
        }

        String normalized = level.toUpperCase();
        switch (normalized) {
            case "ERROR":
            case "WARN":
            case "INFO":
            case "DEBUG":
            case "TRACE":
                return normalized;
            default:
                return "INFO";
        }
    }

    public LogEntry getLogById(Long id) {
        return logEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found with id: " + id));
    }
}