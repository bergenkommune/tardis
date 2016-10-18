package no.kommune.bergen.tardis;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

@Component
class DatabaseSnapshotExporter {
    @Autowired
    private SnapshotStore snapshotStore;

    @Autowired
    private TardisConfiguration configuration;

    private static Logger log = getLogger(DatabaseSnapshotExporter.class);

    boolean TEST;

    public void exportAll() {
        log.debug("Exporting all data sources");
        for (String dataSource : configuration.getDataSourceNames()) {
            log.debug("Exporting data source " + dataSource);
            exportDataSource(dataSource);
        }
    }

    public void exportDataSource(String dataSource) {
        try {
            for (Table table : configuration.getTables(dataSource)) {
                export(table);
                log.debug("Exporting table: {" + table.getName() + ", " + table.getQuery() + "}");
            }
        } catch (Exception e) {
            log.error("Caught exception while exporting dataSource " + dataSource, e);
        } finally {
            snapshotStore.commit(dataSource);
        }
    }

    public void export(Table table) {
        DatabaseTableSnapshotExporter exporter = createExporter();
        Connection conn = null;
        try {
            conn = getConnection(table.getDataSourceName());
            exporter.setConn(conn);
            exporter.setQuery(table.getQuery());
            exporter.setFilename(table.getFilename());
            exporter.export();
            conn.commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            rollback(conn);
        } finally {
            close(conn);
        }
    }

    private void rollback(Connection conn) {
        if (null == conn) return;
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.warn("Failed to roll back", e);
        }
    }

    private Connection getConnection(String dataSource) throws SQLException {
        return configuration.getDataSource(dataSource).getConnection();
    }

    private void close(Connection conn) {
        if (TEST) return;
        try {
            if (null != conn) conn.close();
        } catch (Exception e) {
            log.warn("Failed to close connection", e);
        }
    }

    public DatabaseTableSnapshotExporter createExporter() {
        DatabaseTableSnapshotExporter exporter = new DatabaseTableSnapshotExporter();
        exporter.setSnapshotStore(snapshotStore);
        exporter.setWorkingDirectory(configuration.getWorkingDirectory());
        return exporter;
    }

    public void setConfiguration(TardisConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setSnapshotStore(SnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }
}
