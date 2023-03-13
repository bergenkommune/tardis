package no.kommune.bergen.tardis;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DataSourceHealthIndicator extends AbstractHealthIndicator {

    private final TardisConfiguration config;

    public DataSourceHealthIndicator(TardisConfiguration config) {
        this.config = config;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            for (TardisConfiguration.DataSourceConfig dataSourceConfig : config.getDataSources()) {
                DatabaseMetaData metaData = checkIfDataSourceIsUp(dataSourceConfig);
                builder.up()
                        .withDetail("product", metaData.getDatabaseProductName())
                        .withDetail("version", metaData.getDatabaseProductVersion())
                        .build();
            }
        } catch (Exception e) {
            builder.outOfService().withException(e).build();
        }
    }

    private DatabaseMetaData checkIfDataSourceIsUp(TardisConfiguration.DataSourceConfig dataSourceConfig) throws SQLException {
        DataSource dataSource = config.getDataSource(dataSourceConfig.getName());
        try (Connection conn = dataSource.getConnection()) {
            final DatabaseMetaData metaData = conn.getMetaData();
            return metaData;
        } catch (Exception e) {
            throw e;
        }
    }
}
