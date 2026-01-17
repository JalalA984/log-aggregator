package app.vercel.jalalahmad.log_query_service.service;

import app.vercel.jalalahmad.log_query_service.model.IndexedLog;
import app.vercel.jalalahmad.log_query_service.model.SearchRequestDTO;
import app.vercel.jalalahmad.log_query_service.repository.IndexedLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class LogSearchService {

    @Autowired
    private IndexedLogRepository indexedLogRepository;

    public Page<IndexedLog> searchLogs(SearchRequestDTO searchRequest) {
        Pageable pageable = PageRequest.of(
                searchRequest.getPage(),
                searchRequest.getSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        // If there's a text query, use full-text search
        if (searchRequest.getQuery() != null && !searchRequest.getQuery().trim().isEmpty()) {
            return indexedLogRepository.searchByText(searchRequest.getQuery(), pageable);
        }

        // If NO filters specified, return ALL logs
        if ((searchRequest.getLevel() == null || searchRequest.getLevel().isEmpty()) &&
                (searchRequest.getSourceApp() == null || searchRequest.getSourceApp().isEmpty())) {
            return indexedLogRepository.findAll(pageable);
        }

        // Otherwise, use filtered search with wider time range
        LocalDateTime from = searchRequest.getFrom() != null ?
                searchRequest.getFrom() : LocalDateTime.now().minusDays(7);  // Last 7 days instead of 24 hours
        LocalDateTime to = searchRequest.getTo() != null ?
                searchRequest.getTo() : LocalDateTime.now();

        return indexedLogRepository.findByLevelAndSourceAppAndTimestampBetween(
                searchRequest.getLevel(),
                searchRequest.getSourceApp(),
                from,
                to,
                pageable
        );
    }

    public Map<String, Object> getStatistics(LocalDateTime from, LocalDateTime to) {
        Map<String, Object> stats = new HashMap<>();

        // Get counts by level
        stats.put("infoCount", indexedLogRepository.countByLevelAndTimestampBetween("INFO", from, to));
        stats.put("errorCount", indexedLogRepository.countByLevelAndTimestampBetween("ERROR", from, to));
        stats.put("warnCount", indexedLogRepository.countByLevelAndTimestampBetween("WARN", from, to));
        stats.put("debugCount", indexedLogRepository.countByLevelAndTimestampBetween("DEBUG", from, to));

        // Get total count
        long total = (long)stats.get("infoCount") + (long)stats.get("errorCount") +
                (long)stats.get("warnCount") + (long)stats.get("debugCount");
        stats.put("totalCount", total);

        return stats;
    }

    public Map<String, Long> getSourceAppStatistics(LocalDateTime from, LocalDateTime to) {
        // This is simplified - in reality you'd query by sourceApp
        Map<String, Long> sourceStats = new HashMap<>();

        // For now, return empty or implement proper grouping query
        // You could use a native query: SELECT source_app, COUNT(*) FROM indexed_logs GROUP BY source_app

        return sourceStats;
    }
}