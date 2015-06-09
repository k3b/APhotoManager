package de.k3b.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by k3b on 04.06.2015.
 */
public class Directory {
    public static final String PATH_DELIMITER = "/";

    private String relPath = null;
    private Directory parent = null;
    private List<Directory> children = null;

    private int nonDirItemCount = 0;
    private int nonDirSubItemCount = 0;
    private int dirCount = 0;
    private int subDirCount = 0;

    public Directory(String relPath, Directory parent, int nonDirItemCount) {
        this.setRelPath(relPath);
        this.setParent(parent);
        // this.setHasNonDirElements(hasNonDirElements);
        if (parent != null) {
            parent.addChild(this);
        }
        setNonDirItemCount(nonDirItemCount);
    }

    void addChild(Directory child) {
        if (this.children == null)
            this.children = new ArrayList<>();
        this.children.add(child);

    }

    public String getAbsolute() {
        StringBuilder result = new StringBuilder();
        Directory current = this;

        while (current != null) {
            String pathSegment = current.getRelPath();
            if ((pathSegment != null) && (pathSegment.length() > 0)) {
                result.insert(0, pathSegment);
                result.insert(0, PATH_DELIMITER);
            }
            current = current.getParent();
        }
        return result.toString();
    }

    public String getRelPath() {
        return relPath;
    }

    public void setRelPath(String relPath) {
        this.relPath = relPath;
    }

    public Directory getParent() {
        return parent;
    }

    public void setParent(Directory parent) {
        this.parent = parent;
    }

    public List<Directory> getChildren() {
        return children;
    }

    public void setChildren(List<Directory> children) {
        this.children = children;
    }

    static StringBuilder toTreeString(StringBuilder result, Directory item, String delimiter) {
        if (item != null) {
            result.append(item.getRelPath()).append(delimiter);

            if (item.getChildren() != null) {
                for (Directory child : item.getChildren()) {
                    toTreeString(result, child, delimiter);
                }
            }
        }
        return result;
    }

    public String toString() {
        return getAbsolute();
    }

    public int getNonDirItemCount() {
        return nonDirItemCount;
    }

    public void setNonDirItemCount(int nonDirItemCount) {
        this.nonDirItemCount = nonDirItemCount;
    }

    public int getNonDirSubItemCount() {
        return nonDirSubItemCount;
    }

    public void setNonDirSubItemCount(int nonDirSubItemCount) {
        this.nonDirSubItemCount = nonDirSubItemCount;
    }

    public int getDirCount() {
        return dirCount;
    }

    public void setDirCount(int dirCount) {
        this.dirCount = dirCount;
    }

    public int getSubDirCount() {
        return subDirCount;
    }

    public void setSubDirCount(int subDirCount) {
        this.subDirCount = subDirCount;
    }
}
