package no.kommune.bergen.tardis;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Date;
import java.util.Scanner;
import java.util.Set;


@Configuration
@AutoConfigureBefore({EndpointAutoConfiguration.class})
@AutoConfigureAfter({HealthIndicatorAutoConfiguration.class})
public class DatasourceHealth {

    public static class HealthIndicatorFactory {
        @Autowired
        HealthAggregator healthAggregator;

        @Autowired
        TardisConfiguration config;

        @Bean
        protected HealthIndicator datasourceHealth() {
            CompositeHealthIndicator compositeHealthIndicator = new CompositeHealthIndicator(healthAggregator);

            for (TardisConfiguration.DataSourceConfig dataSourceConfig : config.getDataSources()) {
                compositeHealthIndicator.addHealthIndicator(dataSourceConfig.getName(), new MyDataSourceHealthIndicator(dataSourceConfig, config));
            }

            return compositeHealthIndicator;
        }
    }
}
