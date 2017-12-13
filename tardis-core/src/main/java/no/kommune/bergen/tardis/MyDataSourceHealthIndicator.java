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

public class MyDataSourceHealthIndicator extends AbstractHealthIndicator {

    private final TardisConfiguration.DataSourceConfig dataSourceConfig;
    private final TardisConfiguration config;

    public MyDataSourceHealthIndicator(TardisConfiguration.DataSourceConfig dataSourceConfig, TardisConfiguration config) {
        this.dataSourceConfig = dataSourceConfig;
        this.config = config;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            DatabaseMetaData metaData = checkIfDataSourceIsUp();
            try {
                checkLastSync();
                builder.up()
                        .withDetail("product", metaData.getDatabaseProductName())
                        .withDetail("version", metaData.getDatabaseProductVersion());
            } catch (Exception e) {
                builder.status("WARN")
                        .withDetail("warning-message", e.getMessage())
                        .build();
            }
        } catch (Exception e) {
            builder.outOfService().withException(e);
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

    private void checkLastSync() throws Exception {
        checkStatusFile();
        checkOkFile();
    }

    private void checkStatusFile() throws Exception {
        File statusFile = getFile(dataSourceConfig.getName() + ".status");
        String status = getStatus(statusFile);

        if (!status.equals("OK"))
            throw new Exception("The synchronization for " + dataSourceConfig.getName() + " failed with status " + status);
    }

    private void checkOkFile() throws Exception {
        File okFile = getFile(dataSourceConfig.getName() + ".status");
        Date okDate = new Date(okFile.lastModified());
        Long age = Long.parseLong(dataSourceConfig.getAge());

        if ((System.currentTimeMillis() - okDate.getTime()) > age) {
            throw new Exception("The synchronization for " + dataSourceConfig.getName() + " has not succeeded for a while. "
                    + dataSourceConfig.getName() + ".status is more than " + age + " milliseconds old");
        }
    }

    private File getFile(String filename) {
        File file = new File(getStatusDirectory(), filename);
        return file;
    }

    private File getStatusDirectory() {
        File statusDir = new File(config.getStatusDirectory());
        statusDir.mkdirs();
        return statusDir;
    }

    private String getStatus(File file) throws FileNotFoundException {
        Scanner scanner = null;
        try {
            scanner = new Scanner(file);
            String status = scanner.nextLine();
            return status;
        } finally {
            if (scanner != null) scanner.close();
        }
    }
}
