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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Set;


@Configuration
@AutoConfigureBefore({EndpointAutoConfiguration.class})
@AutoConfigureAfter({HealthIndicatorAutoConfiguration.class})
public class DatasourceHealth {

    public static class HealthIndicatorFactory {
        @Autowired
        TardisConfiguration config;

        @Autowired
        HealthAggregator healthAggregator;

        @Bean
        protected HealthIndicator datasourceHealth() {
            CompositeHealthIndicator compositeHealthIndicator = new CompositeHealthIndicator(healthAggregator);

            Set<String> dataSourceNames = config.getDataSourceNames();
            for (String dataSourceName : dataSourceNames) {
                DataSource dataSource = config.getDataSource(dataSourceName);
                compositeHealthIndicator.addHealthIndicator(dataSourceName, new MyDataSourceHealthIndicator(dataSource));
            }

            return compositeHealthIndicator;
        }

        private class MyDataSourceHealthIndicator extends AbstractHealthIndicator {
            private final DataSource dataSource;

            public MyDataSourceHealthIndicator(DataSource dataSource) {
                this.dataSource = dataSource;
            }

            @Override
            protected void doHealthCheck(Health.Builder builder) throws Exception {
                try (Connection conn = dataSource.getConnection()) {
                    final DatabaseMetaData metaData = conn.getMetaData();
                    builder.up()
                            .withDetail("product", metaData.getDatabaseProductName())
                            .withDetail("version", metaData.getDatabaseProductVersion());
                } catch (Exception e) {
                    builder.outOfService().withException(e);
                }
            }
        }
    }
}
