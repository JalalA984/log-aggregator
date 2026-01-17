package app.vercel.jalalahmad.web_app_simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebAppSimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebAppSimulatorApplication.class, args);
	}

}
