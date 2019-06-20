package de.k3b.media;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Transfers {@link IPhotoProperties} - items as csv to memory via {@link #save(IPhotoProperties)} */
public class PhotoPropertiesCsvStringSaver extends PhotoPropertiesCsvSaver {
    private StringWriter result = new StringWriter();
    private PrintWriter writer = new PrintWriter(result);
    public PhotoPropertiesCsvStringSaver() {
        super(null);
        this.setPrinter(writer);
    }

    /** returns the collected csv as string */
    @Override
    public String toString() {
        return result.toString();
    }
}
