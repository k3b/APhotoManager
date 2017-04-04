package de.k3b;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.k3b.media.ExifInterface;

public class ShowExif {
    private static final String usage = "usage java -jar ShowExif.jar [file.jpg [file.jpg] ..]";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(usage);
            System.exit(-1);
        }
        for (String fileName : args) {
            show(fileName);
        }
        System.exit(0);
    }

    private static void show(String fileName) {
        System.out.println("------");
        System.out.println(fileName);

        try {
            ExifInterface exif = new ExifInterface(fileName, null);
            System.out.println(exif.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
