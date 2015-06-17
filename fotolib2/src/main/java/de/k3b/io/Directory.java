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

    // Display options
    public static final int OPT_DIR = 1;
    public static final int OPT_SUB_DIR = 2;
    public static final int OPT_ITEM = 4;
    public static final int OPT_SUB_ITEM = 8;
    public static final int OPT_ALL = 0xffff;
    public static final int OPT_NONE = 0;

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

    static StringBuilder toTreeString(StringBuilder result, Directory item, String delimiter, int options) {
        if (item != null) {
            result.append(item.getRelPath());
            appendCount(result, item, options);
            result.append(delimiter);

            if (item.getChildren() != null) {
                for (Directory child : item.getChildren()) {
                    toTreeString(result, child, delimiter, options);
                }
            }
        }
        return result;
    }

    public static void appendCount(StringBuilder result, Directory item, int options) {
        int dirCount = ((options & OPT_DIR) == 0) ? 0 : item.getDirCount();
        int subDirCount = ((options & OPT_SUB_DIR) == 0) ? 0 : item.getSubDirCount();
        int nonDirItemCount = ((options & OPT_ITEM) == 0) ? 0 : item.getNonDirItemCount();
        int nonDirSubItemCount = ((options & OPT_SUB_ITEM) == 0) ? 0 : item.getNonDirSubItemCount();

        appendCount(result, "(", dirCount, subDirCount, ")");
        appendCount(result, ":(", nonDirItemCount, nonDirSubItemCount, ")");
    }

    private static void appendCount(StringBuilder result, String prefix, int count, int subCount, String suffix) {
        if ((count > 0) || (subCount > count)) {
            result.append(prefix);
            if (count > 0) result.append(count);
            if (subCount > count) result.append("+").append(subCount - count);
            result.append(suffix);
        }
    }

    @Override
    public String toString() {
        return getAbsolute();
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

    public Directory find(String path) {
        if (path != null) {
            return find(this, new StringBuilder(path));
        }
        return null;
    }

    private static Directory find(Directory parent, StringBuilder path) {
        while (path.indexOf(PATH_DELIMITER) == 0) {
            path.delete(0, PATH_DELIMITER.length());
        }

        int pathLen = path.length();
        if (pathLen == 0) return parent;

        if (parent.children != null) {
            for(Directory child : parent.children) {
                if (path.indexOf(child.getRelPath()) == 0) {
                    int childLen = child.getRelPath().length();
                    if (childLen == pathLen) return child; // found last path element
                    int end = path.indexOf(PATH_DELIMITER, childLen);

                    if (end == childLen) {
                        path.delete(0,childLen);
                        return find(child, path);
                    }
                }
            }
        }

        return null;
    }
}
