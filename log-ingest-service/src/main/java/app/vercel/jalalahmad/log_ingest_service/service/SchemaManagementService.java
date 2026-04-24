package app.vercel.jalalahmad.log_ingest_service.service;

import app.vercel.jalalahmad.log_ingest_service.model.LogSchema;
import app.vercel.jalalahmad.log_ingest_service.model.SchemaInferenceResult;
import app.vercel.jalalahmad.log_ingest_service.model.SchemaInferenceResult.DetectedField;
import app.vercel.jalalahmad.log_ingest_service.repository.LogSchemaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SchemaManagementService {

    private static final Logger logger = LoggerFactory.getLogger(SchemaManagementService.class);

    private static final Set<String> TIMESTAMP_NAMES = Set.of(
            "timestamp", "ts", "@timestamp", "time", "datetime", "date", "created_at", "createdAt"
    );
    private static final Set<String> LEVEL_NAMES = Set.of(
            "level", "severity", "log_level", "logLevel", "loglevel", "lvl"
    );
    private static final Set<String> MESSAGE_NAMES = Set.of(
            "message", "msg", "text", "body", "log", "content"
    );
    private static final Set<String> TRACE_ID_NAMES = Set.of(
            "traceId", "trace_id", "requestId", "request_id", "correlationId",
            "correlation_id", "x-request-id", "spanId", "span_id"
    );

    @Autowired
    private LogSchemaRepository logSchemaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public LogSchema createSchema(LogSchema schema) {
        if (logSchemaRepository.existsByName(schema.getName())) {
            throw new IllegalArgumentException("Schema with name '" + schema.getName() + "' already exists");
        }
        schema.setCreatedAt(LocalDateTime.now());
        schema.setUpdatedAt(LocalDateTime.now());
        LogSchema saved = logSchemaRepository.save(schema);
        logger.info("Created schema '{}' with id {}", saved.getName(), saved.getId());
        return saved;
    }

    public List<LogSchema> getAllSchemas() {
        return logSchemaRepository.findAll();
    }

    public LogSchema getSchemaByName(String name) {
        return logSchemaRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Schema not found: " + name));
    }

    @Transactional
    public LogSchema updateSchema(UUID id, LogSchema updated) {
        LogSchema existing = logSchemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schema not found with id: " + id));

        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getFieldMappings() != null) existing.setFieldMappings(updated.getFieldMappings());
        if (updated.getCustomFields() != null) existing.setCustomFields(updated.getCustomFields());
        if (updated.getSampleLog() != null) existing.setSampleLog(updated.getSampleLog());
        existing.setUpdatedAt(LocalDateTime.now());

        return logSchemaRepository.save(existing);
    }

    @Transactional
    public void deleteSchema(UUID id) {
        if (!logSchemaRepository.existsById(id)) {
            throw new RuntimeException("Schema not found with id: " + id);
        }
        logSchemaRepository.deleteById(id);
        logger.info("Deleted schema with id {}", id);
    }

    @SuppressWarnings("unchecked")
    public SchemaInferenceResult inferSchema(String sampleJson) throws JsonProcessingException {
        Map<String, Object> parsed = objectMapper.readValue(sampleJson, Map.class);

        List<DetectedField> detectedFields = new ArrayList<>();
        Map<String, String> suggestedMappings = new HashMap<>();

        for (Map.Entry<String, Object> entry : parsed.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            String type = detectType(value);

            String suggestedMapping = guessMapping(fieldName);
            detectedFields.add(new DetectedField(fieldName, type, suggestedMapping));

            if (suggestedMapping != null && !suggestedMappings.containsValue(suggestedMapping)) {
                suggestedMappings.put(suggestedMapping, fieldName);
            }
        }

        return new SchemaInferenceResult(detectedFields, suggestedMappings);
    }

    private String detectType(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Integer || value instanceof Long) return "number";
        if (value instanceof Double || value instanceof Float) return "number";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Map) return "object";
        if (value instanceof List) return "array";
        return "string";
    }

    private String guessMapping(String fieldName) {
        String lower = fieldName.toLowerCase();
        if (TIMESTAMP_NAMES.contains(lower) || TIMESTAMP_NAMES.contains(fieldName)) return "timestampField";
        if (LEVEL_NAMES.contains(lower) || LEVEL_NAMES.contains(fieldName)) return "levelField";
        if (MESSAGE_NAMES.contains(lower) || MESSAGE_NAMES.contains(fieldName)) return "messageField";
        if (TRACE_ID_NAMES.contains(lower) || TRACE_ID_NAMES.contains(fieldName)) return "traceIdField";
        return null;
    }
}
