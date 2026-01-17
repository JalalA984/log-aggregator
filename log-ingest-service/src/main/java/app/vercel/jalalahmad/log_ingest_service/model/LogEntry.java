package app.vercel.jalalahmad.log_ingest_service.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "raw_logs")
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_app", nullable = false, length = 100)
    private String sourceApp;

    @Column(nullable = false, length = 20)
    private String level; // INFO, ERROR, WARN, DEBUG

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "indexed", nullable = false)
    private boolean indexed = false;

    // PostgreSQL JSONB column - EXACT IMPLEMENTATION FROM ARTICLE
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, String> metadata = new HashMap<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public LogEntry() {}

    public LogEntry(String sourceApp, String level, String message, String traceId, Map<String, String> metadata) {
        this.sourceApp = sourceApp;
        this.level = level;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = LocalDateTime.now();
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceApp() { return sourceApp; }
    public void setSourceApp(String sourceApp) { this.sourceApp = sourceApp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isIndexed() { return indexed; }
    public void setIndexed(boolean indexed) { this.indexed = indexed; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}