package de.k3b.media;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PhotoPropertiesCsvStringSaver extends PhotoPropertiesCsvSaver {
    private StringWriter result = new StringWriter();
    private PrintWriter writer = new PrintWriter(result);
    public PhotoPropertiesCsvStringSaver() {
        super(null);
        this.setPrinter(writer);
    }

    @Override
    public String toString() {
        return result.toString();
    }
}
