package app.vercel.jalalahmad.log_ingest_service.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "log_schemas")
public class LogSchema {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // Maps user's field names to internal model fields
    // Keys: timestampField, levelField, messageField, sourceAppValue, traceIdField
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_mappings", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> fieldMappings;

    // Custom fields beyond core mappings
    // Each entry: { "originalName": "userId", "type": "string", "searchable": true }
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private List<Map<String, Object>> customFields;

    @Column(name = "sample_log", columnDefinition = "TEXT")
    private String sampleLog;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructors
    public LogSchema() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, String> getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(Map<String, String> fieldMappings) { this.fieldMappings = fieldMappings; }

    public List<Map<String, Object>> getCustomFields() { return customFields; }
    public void setCustomFields(List<Map<String, Object>> customFields) { this.customFields = customFields; }

    public String getSampleLog() { return sampleLog; }
    public void setSampleLog(String sampleLog) { this.sampleLog = sampleLog; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
