package de.k3b.media;

import java.io.IOException;
import java.io.InputStream;

public interface ExifInterfaceFactory {
    /**
     * Reads Exif info from the specified JPEG file or inputstream.
     * @param absoluteJpgPath null or full path of the file where exif comes from
     * @param in null or inputstream where exif comes from
     * @param xmpExtern if not null content of extern xmp sidecar file
     * @param dbg_context String added to the debug-output
     */
    ExifInterfaceEx createExifInterface(String absoluteJpgPath, InputStream in, IPhotoProperties xmpExtern, String dbg_context) throws IOException;
}