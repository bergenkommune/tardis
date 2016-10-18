package no.kommune.bergen.tardis;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class UtcDateFormat extends SimpleDateFormat {
    public UtcDateFormat() {
        super("yyyy-MM-dd'T'HH:mm:ss'Z'");
        setTimeZone(TimeZone.getTimeZone("UTC"));
    }
}
