package no.kommune.bergen.tardis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


public class NdjsonToJsonOutputStreamTest {

    private final Charset utf8 = StandardCharsets.UTF_8;

    @Test
    public void testThatEmptyNdjsonBecomesEmptyArray() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        NdjsonToJsonOutputStream sut = new NdjsonToJsonOutputStream(byteArrayOutputStream);
        sut.write("\n".getBytes(utf8));
        sut.close();

        Assertions.assertEquals("[\n\n]", byteArrayOutputStream.toString("UTF-8"));
    }


    @Test
    public void testThatOneJsonObjectBecomesOneElementArray() throws IOException {

        String json = "{ \"message\": { \"id\": 23, \"lines\": [ \"hello\", \"again\" ] }}";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        NdjsonToJsonOutputStream sut = new NdjsonToJsonOutputStream(byteArrayOutputStream);
        sut.write((json+"\n").getBytes(utf8));
        sut.close();

        Assertions.assertEquals(String.format("[\n%s\n]", json), byteArrayOutputStream.toString("UTF-8"));
    }


    @Test
    public void testThatTwoJsonObjectsBecomesValidArray() throws IOException {

        String json = "{ \"message\": { \"id\": 23, \"lines\": [ \"hello\", \"again\" ] }}";

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        NdjsonToJsonOutputStream sut = new NdjsonToJsonOutputStream(byteArrayOutputStream);
        sut.write((json+"\n").getBytes(utf8));
        sut.write((json+"\n").getBytes(utf8));
        sut.close();

        Assertions.assertEquals(String.format("[\n%1$s,\n%1$s\n]", json), byteArrayOutputStream.toString("UTF-8"));
    }
}
