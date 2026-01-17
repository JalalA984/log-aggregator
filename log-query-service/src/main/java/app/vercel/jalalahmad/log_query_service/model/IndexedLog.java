package app.vercel.jalalahmad.log_query_service.model;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "indexed_logs")
public class IndexedLog {

    @Id
    @Column(name = "log_id", nullable = false)
    private String logId;  // Same as RawLog.id from Ingest Service

    @Column(name = "source_app", nullable = false, length = 100)
    private String sourceApp;

    @Column(nullable = false, length = 20)
    private String level; // INFO, ERROR, WARN, DEBUG

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "searchable_text", columnDefinition = "TEXT")
    private String searchableText; // Pre-processed for full-text search

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // JSONB for metadata
    @Type(JsonBinaryType.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, String> metadata = new HashMap<>();

    @Column(name = "indexed_at")
    private LocalDateTime indexedAt = LocalDateTime.now();

    // Constructors
    public IndexedLog() {}

    public IndexedLog(String logId, String sourceApp, String level, String message,
                      String traceId, LocalDateTime timestamp, Map<String, String> metadata) {
        this.logId = logId;
        this.sourceApp = sourceApp;
        this.level = level;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = timestamp;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.searchableText = preprocessForSearch(message, sourceApp, level);
    }

    // Helper method to create searchable text
    private String preprocessForSearch(String message, String sourceApp, String level) {
        // Convert to lowercase and combine fields for full-text search
        return (sourceApp + " " + level + " " + message).toLowerCase();
    }

    // Getters and Setters
    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getSourceApp() { return sourceApp; }
    public void setSourceApp(String sourceApp) { this.sourceApp = sourceApp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) {
        this.message = message;
        this.searchableText = preprocessForSearch(message, this.sourceApp, this.level);
    }

    public String getSearchableText() { return searchableText; }
    public void setSearchableText(String searchableText) { this.searchableText = searchableText; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public LocalDateTime getIndexedAt() { return indexedAt; }
    public void setIndexedAt(LocalDateTime indexedAt) { this.indexedAt = indexedAt; }
}