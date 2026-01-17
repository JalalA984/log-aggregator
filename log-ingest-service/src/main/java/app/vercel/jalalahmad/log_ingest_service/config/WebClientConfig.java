package app.vercel.jalalahmad.log_ingest_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced  // Keep this for service-to-service calls (if needed later)
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean  // ADD THIS: Non-load-balanced WebClient for Gateway calls
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}