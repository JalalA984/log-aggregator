package app.vercel.jalalahmad.log_ingest_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // This enables @Async annotation support
}