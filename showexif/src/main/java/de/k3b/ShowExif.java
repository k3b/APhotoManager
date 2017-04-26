package de.k3b;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.k3b.media.ExifInterface;
import de.k3b.media.ExifInterfaceEx;
import de.k3b.media.IMetaApi;
import de.k3b.media.ImageMetaReader;
import de.k3b.media.MediaUtil;
import de.k3b.media.MediaXmpSegment;

public class ShowExif {
    private static final String usage = "usage java -jar ShowExif.jar [-d(ebug)] [file.jpg [file.jpg] ..]";
    private static final String dbg_context = "ShowExif";

    public static void main(String[] args) {
        boolean debug = false;
        if (args.length == 0) {
            System.out.println(usage);
            System.exit(-1);
        }
        for (String fileName : args) {
            if (fileName.toLowerCase().startsWith("-d")) {
                debug = true;
            } else {
                show(fileName, debug);
            }
        }
        System.exit(0);
    }

    private static void show(String fileName, boolean debug) {
        System.out.println("------");
        System.out.println(fileName);

        try {
            MediaXmpSegment xmp = MediaXmpSegment.loadXmpSidecarContentOrNull(fileName, dbg_context);

            ExifInterfaceEx exif = new ExifInterfaceEx(fileName, null, xmp, dbg_context);
            ImageMetaReader jpg = new ImageMetaReader().load(fileName, null, xmp, dbg_context);
            show(jpg, debug);
            show(exif, debug);
            show(xmp, debug);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void show(IMetaApi item, boolean debug) {
        if (item != null) {
            System.out.println(MediaUtil.toString(item, false, MediaUtil.FieldID.path));
            if (debug) System.out.println(item.toString() + "\n\n");
        }
    }
}
