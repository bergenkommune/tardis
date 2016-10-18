package no.kommune.bergen.tardis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.Date;

@Component
public class Tardis {
    @Autowired
    private DatabaseSnapshotExporter exporter;

    @Autowired
    private ChangeLogCreator changeLogCreator;

    @Autowired
    private SnapshotStore snapshotStore;

    @Autowired
    public TardisConfiguration configuration;

    private Logger log = LoggerFactory.getLogger(Tardis.class);

    public String log() {
        return snapshotStore.log();
    }

    public void exportAll() {
        exporter.exportAll();
    }

    public void exportGroup(String group) {
        exporter.exportDataSource(group);
    }

    public void getDiff(String dataSourceName, String tableName, String fromRevision, String toRevision, OutputStream out) {
        Table table = configuration.getTable(dataSourceName, tableName);
        assertTableNotNull(dataSourceName, tableName, table);
        changeLogCreator.getDiff(table.getFilename(), table.getPrimaryKeys(), fromRevision, toRevision, out);
    }

    public void getDiff(String dataSourceName, String tableName, Date fromDate, Date toDate, OutputStream out) {
        Table table = configuration.getTable(dataSourceName, tableName);
        assertTableNotNull(dataSourceName, tableName, table);
        changeLogCreator.getDiff(table.getFilename(), table.getPrimaryKeys(), fromDate, toDate, out);
    }

    private void assertTableNotNull(String dataSourceName, String tableName, Table table) {
        if (null == table)
            throw new IllegalArgumentException("Table named " + tableName + " not found in data source " + dataSourceName);
    }

    public void setConfiguration(TardisConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setExporter(DatabaseSnapshotExporter exporter) {
        this.exporter = exporter;
    }

    public void setChangeLogCreator(ChangeLogCreator changeLogCreator) {
        this.changeLogCreator = changeLogCreator;
    }

}
