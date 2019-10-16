package de.k3b.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Properties extends java.util.Properties {

    private static final String FILE_ENCODING = "UTF-8";

    public synchronized void load(File file) throws IOException {
        InputStream inputStream = null;

        try {
            inputStream = new FileInputStream(file);
            load(inputStream);
        } finally {
            FileUtils.close(inputStream, file);
        }
    }

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
