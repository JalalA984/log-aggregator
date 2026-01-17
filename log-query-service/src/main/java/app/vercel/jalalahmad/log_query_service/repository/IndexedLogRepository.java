package app.vercel.jalalahmad.log_query_service.repository;

import app.vercel.jalalahmad.log_query_service.model.IndexedLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IndexedLogRepository extends JpaRepository<IndexedLog, String> {

    // Basic finders
    List<IndexedLog> findBySourceApp(String sourceApp);
    List<IndexedLog> findByLevel(String level);
    List<IndexedLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Combined filters
// Add this method for flexible filtering:
    @Query("SELECT l FROM IndexedLog l WHERE " +
            "(:level IS NULL OR :level = '' OR l.level = :level) AND " +
            "(:sourceApp IS NULL OR :sourceApp = '' OR l.sourceApp = :sourceApp) AND " +
            "l.timestamp BETWEEN :from AND :to")
    Page<IndexedLog> findByLevelAndSourceAppAndTimestampBetween(
            @Param("level") String level,
            @Param("sourceApp") String sourceApp,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    // Full-text search using PostgreSQL ILIKE (case-insensitive LIKE)
    @Query("SELECT l FROM IndexedLog l WHERE " +
            "LOWER(l.searchableText) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<IndexedLog> searchByText(@Param("query") String query, Pageable pageable);



    // Count statistics
    long countBySourceAppAndTimestampBetween(String sourceApp, LocalDateTime start, LocalDateTime end);
    long countByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end);

    // Find by traceId (for distributed tracing correlation)
    List<IndexedLog> findByTraceId(String traceId);
}