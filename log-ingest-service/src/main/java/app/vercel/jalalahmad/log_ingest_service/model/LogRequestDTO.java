package app.vercel.jalalahmad.log_ingest_service.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogRequestDTO {
    private String sourceApp;
    private String level;
    private String message;
    private String traceId;
    private Map<String, String> metadata = new HashMap<>();

    // Constructors
    public LogRequestDTO() {}

    public LogRequestDTO(String sourceApp, String level, String message, String traceId, Map<String, String> metadata) {
        this.sourceApp = sourceApp;
        this.level = level;
        this.message = message;
        this.traceId = traceId;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    // Getters and Setters
    public String getSourceApp() { return sourceApp; }
    public void setSourceApp(String sourceApp) { this.sourceApp = sourceApp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
}