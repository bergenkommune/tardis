package no.kommune.bergen.tardis;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.Scanner;

public class DataSourceHealthIndicator extends AbstractHealthIndicator {

    private final TardisConfiguration.DataSourceConfig dataSourceConfig;
    private final TardisConfiguration config;

    public DataSourceHealthIndicator(TardisConfiguration.DataSourceConfig dataSourceConfig, TardisConfiguration config) {
        this.dataSourceConfig = dataSourceConfig;
        this.config = config;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            DatabaseMetaData metaData = checkIfDataSourceIsUp();
            builder.up()
                    .withDetail("product", metaData.getDatabaseProductName())
                    .withDetail("version", metaData.getDatabaseProductVersion())
                    .build();
        } catch (Exception e) {
            builder.outOfService().withException(e).build();
        }
    }

    private DatabaseMetaData checkIfDataSourceIsUp() throws SQLException {
        DataSource dataSource = config.getDataSource(dataSourceConfig.getName());
        try (Connection conn = dataSource.getConnection()) {
            final DatabaseMetaData metaData = conn.getMetaData();
            return metaData;
        } catch (Exception e) {
            throw e;
        }
    }
}
