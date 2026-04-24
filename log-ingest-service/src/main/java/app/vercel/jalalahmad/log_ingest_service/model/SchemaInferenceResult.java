package app.vercel.jalalahmad.log_ingest_service.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaInferenceResult {

    private List<DetectedField> detectedFields;
    private Map<String, String> suggestedMappings;

    public SchemaInferenceResult() {}

    public SchemaInferenceResult(List<DetectedField> detectedFields, Map<String, String> suggestedMappings) {
        this.detectedFields = detectedFields;
        this.suggestedMappings = suggestedMappings;
    }

    public List<DetectedField> getDetectedFields() { return detectedFields; }
    public void setDetectedFields(List<DetectedField> detectedFields) { this.detectedFields = detectedFields; }

    public Map<String, String> getSuggestedMappings() { return suggestedMappings; }
    public void setSuggestedMappings(Map<String, String> suggestedMappings) { this.suggestedMappings = suggestedMappings; }

    public static class DetectedField {
        private String name;
        private String type;
        private String suggestedMapping; // e.g., "timestampField" or null

        public DetectedField() {}

        public DetectedField(String name, String type, String suggestedMapping) {
            this.name = name;
            this.type = type;
            this.suggestedMapping = suggestedMapping;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getSuggestedMapping() { return suggestedMapping; }
        public void setSuggestedMapping(String suggestedMapping) { this.suggestedMapping = suggestedMapping; }
    }
}
