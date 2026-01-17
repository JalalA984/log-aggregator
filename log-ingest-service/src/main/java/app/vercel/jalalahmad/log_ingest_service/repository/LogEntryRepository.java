package app.vercel.jalalahmad.log_ingest_service.repository;

import app.vercel.jalalahmad.log_ingest_service.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    List<LogEntry> findBySourceAppAndTimestampBetween(
            String sourceApp,
            LocalDateTime start,
            LocalDateTime end
    );

    List<LogEntry> findByLevelAndTimestampBetween(
            String level,
            LocalDateTime start,
            LocalDateTime end
    );

    List<LogEntry> findByIndexedFalse();

    long countBySourceAppAndTimestampBetween(
            String sourceApp,
            LocalDateTime start,
            LocalDateTime end
    );
}