package no.kommune.bergen.tardis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.io.*;

@Configuration
@EnableScheduling
public class ScheduledTaskConfiguration implements SchedulingConfigurer {

    private TardisConfiguration config;
    private Tardis tardis;

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskConfiguration.class);

    public ScheduledTaskConfiguration(TardisConfiguration config, Tardis tardis) {
        this.config = config;
        this.tardis = tardis;
        log.info("Setting up scheduling");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        if (!config.isCron()) {
            log.info("Cron is disabled");
            return;
        }

        for (TardisConfiguration.DataSourceConfig datasource : config.getDataSources()) {
            log.info("Enabling scheduling for " + datasource.getName() + " with cron expression " + datasource.getCronExpression());
            String datasourceName = datasource.getName();
            taskRegistrar.addTriggerTask(
                    () -> {
                        try {
                            log.info("Export " + datasourceName + " starting");
                            tardis.exportGroup(datasourceName);
                            writeStatus(datasourceName + ".status", false);
                            touchFile(datasourceName + ".ok");
                            log.info("Export " + datasourceName + " finished");
                        } catch (Exception e) {
                            log.error(datasourceName + " failed.", e);
                            writeStatus(datasourceName + ".status", true);
                        }
                    },
                    new CronTrigger(datasource.getCronExpression())
            );
        }

        if (StringUtils.isNotEmpty(config.getOptimizeCron())) {
            log.info("Enabling scheduling for optimization with cron expression " + config.getOptimizeCron());

            taskRegistrar.addTriggerTask(
                    () -> {
                        try {
                            log.info("Tardis optimization starting");
                            tardis.optimizeStorage();
                            log.info("Tardis optimization finished");
                        } catch (Exception e) {
                            log.error("Tardis optimization (git gc) failed", e);
                        }
                    },
                    new CronTrigger(config.getOptimizeCron()));
        }
    }

    private void touchFile(String filename) {
        File file = new File(getStatusDirectory(), filename);
        try {
            Writer pw = new PrintWriter(file);
            pw.close();
        } catch (IOException e) {
            throw new RuntimeException("Feil ved oppretting/oppdatering av fil " + file.getAbsolutePath() + ".", e);
        }
    }

    private File getStatusDirectory() {
        File statusDir = new File(config.getStatusDirectory());
        statusDir.mkdirs();
        return statusDir;
    }

    private void writeStatus(String filename, boolean failed) {
        File file = new File(getStatusDirectory(), filename);
        try (
                FileOutputStream fos = new FileOutputStream(file);
                PrintWriter writer = new PrintWriter(fos)
        ) {
            writer.write(failed ? "ERROR" : "OK");
        } catch (Exception e) {
            log.error("Could not write status to " + file);
        }
    }
}
