package app.vercel.jalalahmad.log_query_service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IndexRequestDTO {
    private String id;  // RawLog.id from Ingest Service
    private String sourceApp;
    private String level;
    private String message;
    private String traceId;
    private LocalDateTime timestamp;
    private Map<String, String> metadata = new HashMap<>();
    private List<String> searchableCustomValues;

    // Constructors
    public IndexRequestDTO() {}

    public IndexRequestDTO(String id, String sourceApp, String level, String message,
                           String traceId, LocalDateTime timestamp, Map<String, String> metadata) {
        this.id = id;
        this.sourceApp = sourceApp;
        this.level = level;
        this.message = message;
        this.traceId = traceId;
        this.timestamp = timestamp;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    public List<String> getSearchableCustomValues() { return searchableCustomValues; }
    public void setSearchableCustomValues(List<String> searchableCustomValues) { this.searchableCustomValues = searchableCustomValues; }
}