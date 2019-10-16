package de.k3b.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Properties extends java.util.Properties {

    private static final String FILE_ENCODING = "UTF-8";

    @Override
    public synchronized void load(InputStream inputStream) throws IOException {
        InputStreamReader in = null;
        try {
            in = new InputStreamReader(inputStream, FILE_ENCODING);
            load(in);
        } finally {
            FileUtils.close(in, inputStream);
        }
    }

    public void store(OutputStream stream, String comment) throws IOException {
        Writer o = null;
        try {
            o = new BufferedWriter(new OutputStreamWriter(stream, FILE_ENCODING));
            store(o, comment);
        } finally {
            FileUtils.close(o, stream);
        }
    }
}
