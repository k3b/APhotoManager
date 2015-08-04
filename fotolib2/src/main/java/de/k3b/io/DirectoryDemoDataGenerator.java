/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of AndroFotoFinder.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
 
package de.k3b.io;

import java.util.Random;

/**
 * Created by k3b on 11.06.2015.
 */
public class DirectoryDemoDataGenerator {
    private static Random rand = new Random();
    // generate some random amount of child objects (1..10)
    public static IDirectory generateTestData() {
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

}
