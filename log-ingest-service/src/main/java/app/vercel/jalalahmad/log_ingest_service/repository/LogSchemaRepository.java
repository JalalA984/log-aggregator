package app.vercel.jalalahmad.log_ingest_service.repository;

import app.vercel.jalalahmad.log_ingest_service.model.LogSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LogSchemaRepository extends JpaRepository<LogSchema, UUID> {
    Optional<LogSchema> findByName(String name);
    boolean existsByName(String name);
}
