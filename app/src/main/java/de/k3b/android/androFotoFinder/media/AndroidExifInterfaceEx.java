package de.k3b.android.androFotoFinder.media;

import de.k3b.media.ExifInterfaceEx;

public class AndroidExifInterfaceEx extends ExifInterfaceEx {
    public static void init() {
        setFactory(new Factory() {
            @Override
            public ExifInterfaceEx create() {
                return new AndroidExifInterfaceEx();
            }
        });
    }
}
