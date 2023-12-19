package no.kommune.bergen.tardis;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class ChangeLogCreator {

    @Autowired
    private SnapshotStore snapshotStore;

    public void setSnapshotStore(SnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    public void getDiff(String filename, Collection<String> primaryKeyColumns, Date from, Date to, OutputStream out) {
        byte[] diff = snapshotStore.getDiff(filename, from, to);
        createJsonDiffStream(primaryKeyColumns, diff, out);
    }

    void getDiff(String filename, Collection<String> primaryKeyColumns, String fromRevision, String toRevision, OutputStream out) {
        byte[] diff = snapshotStore.getDiff(filename, fromRevision, toRevision);
        createJsonDiffStream(primaryKeyColumns, diff, out);
    }

    public void getSnapshot(String filename, OutputStream out){
        InputStream inputStream = snapshotStore.getLatestSnapshot(filename);
        createJsonSnapshotStream(inputStream, out);
    }

    private void createJsonSnapshotStream(InputStream inputStream, OutputStream out) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonGenerator g = createJsonGenerator(out, mapper);
            g.writeStartArray();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    generateSnapshotRecord(g, line);
                }
            }

            g.writeEndArray();
            g.writeRaw("\n");
            g.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void generateSnapshotRecord(JsonGenerator g, String line) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode recordNode = mapper.readTree(line);
        g.writeObject(recordNode);
    }

    private void createJsonDiffStream(Collection<String> primaryKeyColumns, byte[] diff, OutputStream out) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonGenerator g = createJsonGenerator(out, mapper);

            try(FilteredLinesReader deleted = new FilteredLinesReader(new InputStreamReader(new ByteArrayInputStream(diff), "UTF-8"), "-{");
                FilteredLinesReader added = new FilteredLinesReader(new InputStreamReader(new ByteArrayInputStream(diff), "UTF-8"), "+{")) {

                String deletedLine = deleted.readLine();
                String addedLine = added.readLine();

                while (addedLine != null || deletedLine != null) {
                    Map<String, Object> deletedRecord = getRecord(deletedLine);
                    Map<String, Object> addedRecord = getRecord(addedLine);

                    int compareResult = compareRecords(deletedRecord, addedRecord, primaryKeyColumns);

                    if (compareResult == 0) {
                        generateChangedRecord(g, deletedLine, addedLine);
                        addedLine = added.readLine();
                        deletedLine = deleted.readLine();
                    } else if (compareResult > 0) {
                        generateAddedRecord(g, addedLine);
                        addedLine = added.readLine();
                    } else {
                        generateDeletedRecord(g, deletedLine);
                        deletedLine = deleted.readLine();
                    }
                }
            }
            g.writeRaw("\n");
            g.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public JsonGenerator createJsonGenerator(OutputStream out, ObjectMapper mapper) throws IOException {
        JsonFactory f = new JsonFactory();
        JsonGenerator g = f.createGenerator(out, JsonEncoding.UTF8);
        g.setCodec(mapper); // Set the ObjectCodec
        g.setPrettyPrinter(new DiffablePrettyPrinter());
        return g;
    }

    public void generateChangedRecord(JsonGenerator g, String deletedLine, String addedLine) throws Exception {
        g.writeStartObject();
        g.writeStringField("changeType", "modify");
        writeRawField(g, "oldRecord", deletedLine);
        writeRawField(g, "newRecord", addedLine);
        g.writeEndObject();
    }

    public void generateDeletedRecord(JsonGenerator g, String deletedLine) throws Exception {
        g.writeStartObject();
        g.writeStringField("changeType", "delete");
        writeRawField(g, "oldRecord", deletedLine);
        g.writeEndObject();
    }

    public void generateAddedRecord(JsonGenerator g, String addedLine) throws Exception {
        g.writeStartObject();
        g.writeStringField("changeType", "add");
        writeRawField(g, "newRecord", addedLine);
        g.writeEndObject();
    }

    private void writeRawField(JsonGenerator g, String fieldname, String value) throws IOException {
        g.writeFieldName(fieldname);
        g.writeRawValue(value);
    }

    private int compareRecords(Map<String, Object> a, Map<String, Object> b, Collection<String> primaryKeyColumns) {
        for (String primaryKey : primaryKeyColumns) {
            Comparable keyA = (Comparable) a.get(primaryKey);
            Comparable keyB = (Comparable) b.get(primaryKey);

            if (keyA == null && keyB == null) continue;
            if (keyA != null && keyB == null) return -1;
            if (keyA == null && keyB != null) return 1;

            int result = keyA.compareTo(keyB);
            if (result == 0) continue;
            return result;
        }
        return 0;
    }

    private Map<String, Object> getRecord(String line) throws IOException {
        if (line == null) return new HashMap<>();

        return new ObjectMapper().readValue(line, new TypeReference<HashMap<String, Object>>() {
        });
    }
}

