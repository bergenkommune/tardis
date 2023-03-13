package no.kommune.bergen.tardis;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@AutoConfigureBefore({EndpointAutoConfiguration.class})
public class Health {

    public static class HealthIndicatorFactory {

        @Autowired
        TardisConfiguration config;

        @Bean
        protected HealthIndicator synchronizationHealth() {
            return new SynchronizationHealthIndicator(config);
        }

        @Bean
        protected HealthIndicator datasourceHealth() {
            return new DataSourceHealthIndicator(config);
        }
    }
}
