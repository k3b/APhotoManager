package de.k3b.android.fotoviewer.directory;

import java.util.ArrayList;
import java.util.Random;

import de.k3b.io.Directory;

/**
 * Created by k3b on 11.06.2015.
 */
public class DirectoryLoader {
    private static Random rand = new Random();
    // generate some random amount of child objects (1..10)
    private static Directory generateTestData() {
        Directory root = new Directory("", null, 0);

        generateTestData(root, "p", getRandomInt(9) + 1, 12);
        return root;
    }

    private static int getRandomInt(int maxRnd) {
        // return (maxRnd >= 7) ? 7 : maxRnd - 1;
        return rand.nextInt(maxRnd);
    }

    private static void generateTestData(Directory parent, String namePrefix, int numberOfItems, int maxDepth) {
            for (int i = 0; i < numberOfItems; i++) {
                String relPath = namePrefix + "_" + i;
                int quantity = getRandomInt(10) - 5;
                if (quantity < 0) quantity = 0;
                Directory child = new Directory(relPath, parent, quantity);
                if (maxDepth > 1) {
                    generateTestData(child, relPath, getRandomInt(9) + 1, getRandomInt(maxDepth - 1));
                }
            }
    }

    public static Directory getDirectories() {
        return generateTestData();
    }


}
