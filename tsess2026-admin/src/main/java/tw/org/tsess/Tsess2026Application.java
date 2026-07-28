package tw.org.tsess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan("tw.org.tsess")
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class Tsess2026Application {
	public static void main(String[] args) {
		SpringApplication.run(Tsess2026Application.class, args);
	}
}
