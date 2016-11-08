package no.kommune.bergen.tardis;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "tardis")
@EnableConfigurationProperties
public class TardisConfiguration {
    private String workingDirectory = new File(System.getProperty("user.dir"), "data").getAbsolutePath();
    private String statusDirectory = new File(System.getProperty("user.dir"), "status").getAbsolutePath();

    private boolean cron = true;

    private List<Table> tables = new ArrayList<>();
    private Map<String, DataSource> dataSourceMap = new HashMap<>();
    private List<DataSourceConfig> dataSources = new ArrayList<>();

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getStatusDirectory() {
        return statusDirectory;
    }

    public void setStatusDirectory(String statusDirectory) {
        this.statusDirectory = statusDirectory;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public Set<String> getDataSourceNames() {
        Set<String> result = new HashSet<>();
        for (Table table : tables) {
            result.add(table.getDataSourceName());
        }
        return result;
    }

    public List<Table> getTables(String dataSourceName) {
        List<Table> result = new ArrayList<>();
        for (Table table : tables) {
            if (table.getDataSourceName().equals(dataSourceName))
                result.add(table);
        }
        return result;
    }

    public DataSource getDataSource(String dataSourceName) {
        if (dataSourceMap.containsKey(dataSourceName)) return dataSourceMap.get(dataSourceName);

        for (DataSourceConfig config : dataSources) {
            if (config.getName().equals(dataSourceName)) {
                createDataSource(config);
            }
        }
        return dataSourceMap.get(dataSourceName);
    }

    public List<DataSourceConfig> getDataSources() {
        return dataSources;
    }

    public void setDataSources(List<DataSourceConfig> dataSources) {
        this.dataSources = dataSources;
    }

    private void createDataSource(DataSourceConfig config) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(config.getUrl());
        dataSource.setUsername(config.getUsername());
        dataSource.setPassword(config.getPassword());
        dataSource.setAutoCommit(false);

        for (Map.Entry<String, Object> property : config.getProperties().entrySet()) {
            dataSource.addDataSourceProperty(property.getKey(), property.getValue());
        }

        dataSourceMap.put(config.getName(), dataSource);
    }

    public Table getTable(String dataSourceName, String tableName) {
        for (Table table : tables) {
            if (table.getDataSourceName().equals(dataSourceName) && table.getName().equals(tableName))
                return table;
        }
        return null;
    }

    public void addDatasource(String dataSourceName, DataSource dataSource) {
        dataSourceMap.put(dataSourceName, dataSource);
    }

    public void addTable(Table table) {
        if (null == tables) tables = new ArrayList<>();
        tables.add(table);
    }

    public boolean isCron() {
        return cron;
    }

    public void setCron(boolean cron) {
        this.cron = cron;
    }

    public static class DataSourceConfig {
        private String name, url, username, password;

        /*
                                         +--------------------------- seconds (0 - 59) (,/-*)
                                         |   +----------------------- min (0 - 59) (,/-*)
                                         |   |   +------------------- hour (0 - 23) (,/-*)
                                         |   |   |   +--------------- day of month (1 - 31) (,/-*LWC)
                                         |   |   |   |   +----------- month (0 - 11 or JAN - DEC) (,/-*)
                                         |   |   |   |   |   +------- day of week (1 - 7) (Sunday=0) (,/-*LC#)
                                         |   |   |   |   |   |   +--- year (not required) (1970-2099) (,/-*)
                                         |   |   |   |   |   |   |
                                         *   *   *   *   *   *   *
        */
        private String cronExpression = "0  */5  *   *   *   *   *";

        private Map<String, Object> properties = new HashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }
    }
}
