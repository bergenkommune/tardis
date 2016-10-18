package no.kommune.bergen.tardis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class FilteredLinesReader extends BufferedReader {
    private final String prefix;

    public FilteredLinesReader(Reader in, String prefix) {
        super(in);
        this.prefix = prefix;
    }

    @Override
    public String readLine() throws IOException {
        String result = super.readLine();
        while (result != null) {
            if (result.startsWith(prefix)) return result.substring(1);

            result = super.readLine();
        }
        return null;
    }
}
