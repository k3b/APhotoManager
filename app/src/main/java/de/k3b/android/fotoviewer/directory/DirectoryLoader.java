package de.k3b.android.fotoviewer.directory;

import java.util.ArrayList;
import java.util.Random;

import de.k3b.io.Directory;

/**
 * Created by k3b on 11.06.2015.
 */
public class DirectoryLoader {
    // generate some random amount of child objects (1..10)
    private static Directory generateTestData() {
        Random rand = new Random();
        Directory root = new Directory("", null, 0);

        for(int i=0; i < rand.nextInt(9)+1; i++) {
            Directory pi = new Directory("p_" + i, root, 0);
            for (int j = 0; j < rand.nextInt(9) + 1; j++) {
                Directory pj = new Directory("p_" + i + "_" + j, pi, 0);
                for (int k = 0; k < rand.nextInt(9) + 1; k++) {
                    Directory pk = new Directory("p_" + i + "_" + j + "_" + k, pj, 0);
                }
            }
        }
        return root;
    }

    public static Directory getDirectories() {
        return generateTestData();
    }


}
