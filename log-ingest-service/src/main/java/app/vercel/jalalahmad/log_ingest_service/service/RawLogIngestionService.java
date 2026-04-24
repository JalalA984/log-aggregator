package app.vercel.jalalahmad.log_ingest_service.service;

import app.vercel.jalalahmad.log_ingest_service.model.LogEntry;
import app.vercel.jalalahmad.log_ingest_service.model.LogSchema;
import app.vercel.jalalahmad.log_ingest_service.repository.LogEntryRepository;
import app.vercel.jalalahmad.log_ingest_service.repository.LogSchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RawLogIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(RawLogIngestionService.class);

    @Autowired
    private LogSchemaRepository logSchemaRepository;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private LogIndexingService logIndexingService;

    /**
     * Ingest a raw JSON log using a named schema.
     * Returns the saved LogEntry, or null if validation fails (silent ignore).
     */
    @Transactional
    public LogEntry ingestRawLog(String schemaName, Map<String, Object> rawLog) {
        Optional<LogSchema> schemaOpt = logSchemaRepository.findByName(schemaName);
        if (schemaOpt.isEmpty()) {
            logger.debug("Schema '{}' not found, ignoring log", schemaName);
            return null;
        }

        LogSchema schema = schemaOpt.get();
        Map<String, String> mappings = schema.getFieldMappings();

        // Extract core fields using mappings
        String message = extractStringField(rawLog, mappings.get("messageField"));
        String level = extractStringField(rawLog, mappings.get("levelField"));
        String timestampStr = extractStringField(rawLog, mappings.get("timestampField"));
        String traceId = extractStringField(rawLog, mappings.get("traceIdField"));
        String sourceApp = mappings.get("sourceAppValue");

        // message is required — if missing, silently ignore
        if (message == null || message.isBlank()) {
            logger.debug("Missing message field for schema '{}', ignoring log", schemaName);
            return null;
        }

        // Normalize level
        level = validateAndNormalizeLevel(level);

        // Parse timestamp or default to now
        LocalDateTime timestamp = parseTimestamp(timestampStr);

        // Build metadata from custom fields
        Map<String, String> metadata = new HashMap<>();
        List<Map<String, Object>> customFields = schema.getCustomFields();
        if (customFields != null) {
            for (Map<String, Object> cf : customFields) {
                String fieldName = (String) cf.get("originalName");
                Object value = rawLog.get(fieldName);
                if (value != null) {
                    metadata.put(fieldName, value.toString());
                }
            }
        }

        // Create and save LogEntry
        LogEntry logEntry = new LogEntry(sourceApp, level, message, traceId, metadata);
        logEntry.setTimestamp(timestamp);
        LogEntry saved = logEntryRepository.save(logEntry);

        logger.info("Ingested raw log [id={}, schema={}, source={}]",
                saved.getId(), schemaName, sourceApp);

        // Collect searchable custom field values
        List<String> searchableValues = new ArrayList<>();
        if (customFields != null) {
            for (Map<String, Object> cf : customFields) {
                Boolean searchable = (Boolean) cf.get("searchable");
                if (Boolean.TRUE.equals(searchable)) {
                    String fieldName = (String) cf.get("originalName");
                    String val = metadata.get(fieldName);
                    if (val != null) {
                        searchableValues.add(val);
                    }
                }
            }
        }

        // Trigger async indexing with searchable values
        logIndexingService.triggerIndexingAsync(saved, searchableValues);

        return saved;
    }

    private String extractStringField(Map<String, Object> rawLog, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) return null;
        Object val = rawLog.get(fieldName);
        return val != null ? val.toString() : null;
    }

    private LocalDateTime parseTimestamp(String ts) {
        if (ts == null || ts.isBlank()) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(ts);
        } catch (Exception e1) {
            try {
                // Try ISO-8601 with offset (e.g., 2024-01-15T10:30:00Z)
                return LocalDateTime.parse(ts, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e2) {
                try {
                    // Try epoch millis
                    long epoch = Long.parseLong(ts);
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault());
                } catch (Exception e3) {
                    return LocalDateTime.now();
                }
            }
        }
    }

    private String validateAndNormalizeLevel(String level) {
        if (level == null) return "INFO";
        String normalized = level.toUpperCase();
        return switch (normalized) {
            case "ERROR", "WARN", "WARNING", "INFO", "DEBUG", "TRACE" ->
                "WARNING".equals(normalized) ? "WARN" : normalized;
            default -> "INFO";
        };
    }
}
