package no.kommune.bergen.tardis;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "tardis")
@EnableConfigurationProperties
public class TardisConfiguration {
    private String workingDirectory, statusDirectory;

    private List<Table> tables;
    private Map<String, DataSource> dataSourceMap = new HashMap<>();
    private List<DataSourceConfig> dataSources = new ArrayList<>();

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
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
        dataSource.addDataSourceProperty("implicitCachingEnabled", true);
        dataSource.addDataSourceProperty("oracle.jdbc.timezoneAsRegion", false);
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

    public static class DataSourceConfig {
        private String name, url, username, password;

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
    }
}
