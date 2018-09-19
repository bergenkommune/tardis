package no.kommune.bergen.tardis;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


class NdjsonToJsonOutputStream extends FilterOutputStream {

    private static final Charset encoding = StandardCharsets.UTF_8;
    private static final byte[] CLOSING_BRACKET = "\n]".getBytes(encoding);
    private static final byte[] OPENING_BRACKET = "[\n".getBytes(encoding);
    private static final byte[] OBJECT_SEPARATOR = ",\n".getBytes(encoding);
    private static final byte NEWLINE = '\n';

    private boolean pendingOpening;
    private boolean pendingNewline;

    public NdjsonToJsonOutputStream(OutputStream ndjsonOutputStream) {
        super(ndjsonOutputStream);
        pendingOpening = true;
    }

    @Override
    public void write(int b) throws IOException {

        if (pendingOpening) {
            out.write(OPENING_BRACKET);
            pendingOpening = false;
        }

        if (pendingNewline) {
            out.write(OBJECT_SEPARATOR);
            pendingNewline = false;
        }

        if (b==NEWLINE) {
            pendingNewline = true;
        } else {
            out.write(b);
        }
    }

    @Override
    public void close() throws IOException {
        out.write(CLOSING_BRACKET);
        super.close();
    }
}
