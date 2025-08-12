package de.k3b.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.k3b.io.filefacade.IFile;

public interface ExifInterfaceEx extends IPhotoProperties, IPhotoPropertyFileWriter, IPhotoPropertyFileReader {
    /**
     * Reads Exif tags from the specified source.
     *
     * @param in if not null: input stream where data comes from
     * @param jpgFile if not null: input IFile where data comes from
     * @param absoluteJpgPath
     * @param xmpExtern   if not null content of xmp sidecar file
     * @param dbg_context info for debug log why attributes are loaded.
     * @return
     * @throws IOException
     */
    ExifInterfaceEx loadAttributes(InputStream in, IFile jpgFile, String absoluteJpgPath, IPhotoProperties xmpExtern, String dbg_context) throws IOException;

    void saveAttributes(IFile inFile, IFile outFile, boolean deleteInFileOnFinish, Boolean hasXmp) throws IOException;

    // Stores a new JPEG image with EXIF attributes into a given output stream.
    void saveJpegAttributes(InputStream inputStream, OutputStream outputStream, byte[] thumbnail)
            throws IOException;

    /**
     * return the image orinentation as id (one of the ORIENTATION_ROTATE_XXX constants)
     */
    int getOrientationId();

    /**
     * return image orinentation in degrees (0, 90,180,270) or 0 if inknown
     */
    int getOrientationInDegrees();

    /**
     * false means this is no valid jpg format
     */
    boolean isValidJpgExifFormat();

    /** @return true if photo was modified and must be saved therefore */
    boolean fixAttributes();

    /** Prints out attributes for debugging. **/
    String getDebugString(String lineDelimiter, String... _keysToExclude);
}
