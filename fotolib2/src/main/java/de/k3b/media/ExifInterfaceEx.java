package de.k3b.media;

import java.io.File;
import java.io.IOException;

public interface ExifInterfaceEx extends IPhotoProperties {
    /**
     * false means this is no valid jpg format
     */
    boolean isValidJpgExifFormat();

    /**
     * return the image orientation as id (one of the ORIENTATION_ROTATE_XXX constants)
     */
    int getOrientationId();
    /** return image orinentation in degrees (0, 90,180,270) or 0 if inknown */
    int getOrientationInDegrees();

    void saveAttributes() throws IOException;
    void saveAttributes(File inFile, File outFile, boolean deleteInFileOnFinish) throws IOException;

    /** Prints out attributes for debugging. **/
    String getDebugString(String lineDelimiter, String... _keysToExclude);

}
