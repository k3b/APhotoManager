package de.k3b.android.media;

import java.io.IOException;
import java.io.InputStream;

import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.ExifInterfaceExImpl;
import de.k3b.media.ExifInterfaceFactory;
import de.k3b.media.IPhotoProperties;

public class ExifInterfaceExAndroidImpl extends ExifInterfaceExImpl {
    /** factory to create an ExifInterface from file or stream */
    private static ExifInterfaceFactory factory = null;

    public ExifInterfaceExAndroidImpl(String absoluteJpgPath, InputStream in, IPhotoProperties xmpExtern, String dbgContext) throws IOException {
        super(absoluteJpgPath, in, xmpExtern, dbgContext);
    }

    /** factory to create an ExifInterface from file or stream */
    public static ExifInterfaceFactory factory() {
        if (factory == null) {
            factory = new ExifInterfaceFactory() {
                /** {@inheritDoc} */
                @Override
                public ExifInterfaceEx createExifInterface(String absoluteJpgPath, InputStream in, IPhotoProperties xmpExtern, String dbg_context) throws IOException {
                    return new ExifInterfaceExAndroidImpl(absoluteJpgPath, in, xmpExtern, dbg_context);
                }
            };
        }
        return factory;
    }

}
