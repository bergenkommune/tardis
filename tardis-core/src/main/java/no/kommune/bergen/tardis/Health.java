package no.kommune.bergen.tardis;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@AutoConfigureBefore({EndpointAutoConfiguration.class})
@AutoConfigureAfter({HealthIndicatorAutoConfiguration.class})
public class Health {

    public static class HealthIndicatorFactory {
        @Autowired
        HealthAggregator healthAggregator;

        @Autowired
        TardisConfiguration config;

        @Bean
        protected HealthIndicator synchronizationHealth() {
            CompositeHealthIndicator compositeHealthIndicator = new CompositeHealthIndicator(healthAggregator);
            for (TardisConfiguration.DataSourceConfig dataSourceConfig : config.getDataSources()) {
                compositeHealthIndicator.addHealthIndicator(dataSourceConfig.getName(),
                        new SynchronizationHealthIndicator(dataSourceConfig, config));
            }
            return compositeHealthIndicator;
        }

        @Bean
        protected HealthIndicator datasourceHealth() {
            CompositeHealthIndicator compositeHealthIndicator = new CompositeHealthIndicator(healthAggregator);
            for (TardisConfiguration.DataSourceConfig dataSourceConfig : config.getDataSources()) {
                compositeHealthIndicator.addHealthIndicator(dataSourceConfig.getName(),
                        new DataSourceHealthIndicator(dataSourceConfig, config));
            }
            return compositeHealthIndicator;
        }
    }
}
