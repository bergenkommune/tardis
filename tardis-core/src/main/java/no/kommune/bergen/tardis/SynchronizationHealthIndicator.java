package no.kommune.bergen.tardis;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Date;
import java.util.Scanner;

public class SynchronizationHealthIndicator extends AbstractHealthIndicator {

    private final TardisConfiguration config;

    public SynchronizationHealthIndicator(TardisConfiguration config) {
        this.config = config;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            for (TardisConfiguration.DataSourceConfig dataSourceConfig : config.getDataSources()) {
                checkLastSync(dataSourceConfig);
                builder.up().build();
            }
        } catch (Exception e) {
            builder.outOfService().withException(e).build();
        }
    }

    private void checkLastSync(TardisConfiguration.DataSourceConfig dataSourceConfig) throws Exception {
        checkStatusFile(dataSourceConfig);
        if (!StringUtils.isEmpty(dataSourceConfig.getAge())) {
            checkOkFile(dataSourceConfig);
        }
    }

    private void checkStatusFile(TardisConfiguration.DataSourceConfig dataSourceConfig) throws Exception {
        File statusFile = getFile(dataSourceConfig.getName() + ".status");
        String status = getStatus(statusFile);

        if (!status.equals("OK"))
            throw new Exception("The synchronization for " + dataSourceConfig.getName() + " failed with status " + status);
    }

    private void checkOkFile(TardisConfiguration.DataSourceConfig dataSourceConfig) throws Exception {
        File okFile = getFile(dataSourceConfig.getName() + ".ok");
        Date okDate = new Date(okFile.lastModified());
        Long age = Long.parseLong(dataSourceConfig.getAge());

        if ((System.currentTimeMillis() - okDate.getTime()) > age && !isWeekend(LocalDate.now())) {
            throw new Exception("The synchronization for " + dataSourceConfig.getName() + " has not succeeded for a while. "
                    + dataSourceConfig.getName() + ".status is more than " + age + " milliseconds old");
        }
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
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
