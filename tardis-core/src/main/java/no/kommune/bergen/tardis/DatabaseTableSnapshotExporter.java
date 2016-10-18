package no.kommune.bergen.tardis;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;

import java.io.File;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static org.slf4j.LoggerFactory.getLogger;

public class DatabaseTableSnapshotExporter {

    private static Logger log = getLogger(DatabaseTableSnapshotExporter.class);

    private ResultSetMetaData metaData;
    private Connection conn;
    private JsonGenerator g;
    private String filename;
    private String query;

    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private String workingDirectory;
    private SnapshotStore snapshotStore;

    public DatabaseTableSnapshotExporter() {
    }

    public void export() throws Exception {
        try (
                PreparedStatement statement = conn.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()
        ) {
            metaData = resultSet.getMetaData();
            JsonFactory f = new JsonFactory();
            g = f.createJsonGenerator(new File(workingDirectory + File.separator + filename), JsonEncoding.UTF8);
            g.setPrettyPrinter(new DiffablePrettyPrinter());
            while (resultSet.next()) {
                toJson(resultSet);
            }
            g.writeRaw("\n");
            g.close();
            snapshotStore.addSnapshot(filename);
        } catch (SQLException e) {
            throw new RuntimeException("Exception while executing query: " + query, e);
        }
    }

    private void toJson(ResultSet row) throws Exception {
        g.writeStartObject();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);

            if (row.getObject(i) == null) {
                g.writeNullField(columnName);
                continue;
            }

            int columnType = metaData.getColumnType(i);
            switch (columnType) {
                case Types.VARCHAR:
                case Types.NVARCHAR:
                case Types.CHAR:
                    g.writeStringField(columnName, row.getString(i));
                    log.debug("Exporting: " + columnName + " - " + row.getString(i));
                    break;
                case Types.NULL:
                    g.writeNullField(columnName);
                    log.debug("Exporting: " + columnName + " - " + row.getString(i));
                    break;
                case Types.DECIMAL:
                case Types.NUMERIC:
                    g.writeNumberField(columnName, row.getBigDecimal(i));
                    log.debug("Exporting: " + columnName + " - " + row.getString(i));
                    break;
                case Types.INTEGER:
                    g.writeNumberField(columnName, row.getInt(i));
                    log.debug("Exporting: " + columnName + " - " + row.getString(i));
                    break;
                case Types.DATE:
                    g.writeStringField(columnName, df.format(row.getDate(i)));
                    log.debug("Exporting: " + columnName + " - " + row.getString(i));
                    break;
                case Types.TIMESTAMP:
                    g.writeStringField(columnName, df.format(row.getTimestamp(i)));
                    log.debug("Exporting: " + columnName + " - " + row.getString(i));
                    break;
                default:
                    throw new RuntimeException("Got unexpected columnType: " + columnType + " for column: " + columnName + ". The value was " + row.getObject(i));
            }
        }
        g.writeEndObject();
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setSnapshotStore(SnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    public SnapshotStore getSnapshotStore() {
        return snapshotStore;
    }
}
