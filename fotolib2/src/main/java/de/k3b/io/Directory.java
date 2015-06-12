package de.k3b.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to represent a Directory-Structure where a Directory can have several SubDirectories.
 *
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

    /*------------------- simple properties ------------------------*/

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

    /*------------------- formatting ------------------------*/

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

    static StringBuilder toTreeString(StringBuilder result, Directory item, String delimiter) {
        if (item != null) {
            result.append(item.getRelPath());
            appendCount(result, item);
            result.append(delimiter);

            if (item.getChildren() != null) {
                for (Directory child : item.getChildren()) {
                    toTreeString(result, child, delimiter);
                }
            }
        }
        return result;
    }

    public static void appendCount(StringBuilder result, Directory item) {
        appendCount(result, "(", item.getDirCount(), item.getSubDirCount(), ")");
        appendCount(result, ":(", item.getNonDirItemCount(), item.getNonDirSubItemCount(), ")");
    }

    private static void appendCount(StringBuilder result, String prefix, int count, int subCount, String suffix) {
        if ((count > 0) || (subCount > count)) {
            result.append(prefix);
            if (count > 0) result.append(count);
            if (subCount > count) result.append("+").append(subCount - count);
            result.append(suffix);
        }
    }

    /*------------------- statistics ------------------------*/

    public Directory setNonDirItemCount(int nonDirItemCount) {
        this.nonDirItemCount = nonDirItemCount;
        return this;
    }

    public int getNonDirItemCount() {
        return this.nonDirItemCount;
    }

    public int getNonDirSubItemCount() {
        return nonDirSubItemCount;
    }

    public Directory setNonDirSubItemCount(int nonDirSubItemCount) {
        this.nonDirSubItemCount = nonDirSubItemCount;
        return this;
    }

    public int getDirCount() {
        return dirCount;
    }

    public Directory setDirCount(int dirCount) {
        this.dirCount = dirCount;
        return this;
    }

    public int getSubDirCount() {
        return subDirCount;
    }

    public Directory setSubDirCount(int subDirCount) {
        this.subDirCount = subDirCount;
        return this;
    }

    public void addChildStatistics(int subDirCount, int nonDirSubItemCount) {
        setDirCount(getDirCount() + 1);
        setSubDirCount(getSubDirCount() + subDirCount + 1);
        setNonDirSubItemCount(getNonDirSubItemCount() + nonDirSubItemCount);
    }

    public static int getChildCount(Directory item) {
        if ((item != null) && (item.children != null)) return item.children.size();
        return 0;
    }
}
