package no.kommune.bergen.tardis;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.PrettyPrinter;

import java.io.IOException;

public class DiffablePrettyPrinter implements PrettyPrinter {

    @Override
    public void writeRootValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
        jg.writeRaw('\n');
    }

    @Override
    public void writeStartObject(JsonGenerator jg) throws IOException, JsonGenerationException {
        jg.writeRaw('{');
    }

    @Override
    public void writeEndObject(JsonGenerator jg, int nrOfEntries) throws IOException, JsonGenerationException {
        jg.writeRaw('}');
    }

    @Override
    public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
        jg.writeRaw(", ");
    }

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
        jg.writeRaw(": ");
    }

    @Override
    public void writeStartArray(JsonGenerator jg) throws IOException, JsonGenerationException {
        jg.writeRaw("[");
    }

    @Override
    public void writeEndArray(JsonGenerator jg, int nrOfValues) throws IOException, JsonGenerationException {
        jg.writeRaw("]");
    }

    @Override
    public void writeArrayValueSeparator(JsonGenerator jg) throws IOException, JsonGenerationException {
        jg.writeRaw(", ");
    }

    @Override
    public void beforeArrayValues(JsonGenerator jg) throws IOException, JsonGenerationException {
    }

    @Override
    public void beforeObjectEntries(JsonGenerator jg) throws IOException, JsonGenerationException {
    }
}
