package no.kommune.bergen.tardis;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Scanner;

public class SynchronizationHealthIndicator extends AbstractHealthIndicator {

    private final TardisConfiguration.DataSourceConfig dataSourceConfig;
    private final TardisConfiguration config;

    public SynchronizationHealthIndicator(TardisConfiguration.DataSourceConfig dataSourceConfig, TardisConfiguration config) {
        this.dataSourceConfig = dataSourceConfig;
        this.config = config;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            checkLastSync();
            builder.up().build();
        } catch (Exception e) {
            builder.outOfService().withException(e).build();
        }
    }

    private void checkLastSync() throws Exception {
        checkStatusFile();
        if (!StringUtils.isEmpty(dataSourceConfig.getAge())) {
            checkOkFile();
        }
    }

    private void checkStatusFile() throws Exception {
        File statusFile = getFile(dataSourceConfig.getName() + ".status");
        String status = getStatus(statusFile);

        if (!status.equals("OK"))
            throw new Exception("The synchronization for " + dataSourceConfig.getName() + " failed with status " + status);
    }

    private void checkOkFile() throws Exception {
        File okFile = getFile(dataSourceConfig.getName() + ".ok");
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
