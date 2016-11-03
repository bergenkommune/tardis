package no.kommune.bergen.tardis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class TardisApplication {
    public static void main(String[] args) {
        SpringApplication.run(TardisApplication.class, args);
    }
}
