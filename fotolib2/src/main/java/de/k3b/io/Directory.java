package de.k3b.io;

import java.util.List;

/**
 * Created by k3b on 04.06.2015.
 */
public class Directory {
    private final String relPath;
    private Directory parent;
    private List<Directory> children;

    public Directory(String relPath, Directory parent) {
        this.relPath = relPath;
        this.parent = parent;
    }

    public String getFull() {
        StringBuilder result = new StringBuilder();
        Directory current = this;

        while (current != null) {
            result.insert(0,current.relPath);
            current = current.parent;
        }
        return result.toString();
    }
}
