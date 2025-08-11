package de.k3b.media;

import java.io.IOException;

public interface ExifInterfaceFactory {
    public ExifInterfaceEx createExifInterface() throws IOException;
}
