/*
 * Copyright (c) 2015-2020 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.k3b.io.filefacade.IFile;

/**
 * Class to represent a Directory-Structure where a Directory can have several SubDirectories.
 *
 * Created by k3b on 04.06.2015.
 */
public class Directory implements IDirectory {
    public static final String PATH_DELIMITER = "/";

    // Display options
    public static final int OPT_DIR = 1;
    public static final int OPT_SUB_DIR = 2;
    public static final int OPT_ITEM = 4;
    public static final int OPT_SUB_ITEM = 8;
    public static final int OPT_AS_HTML = 0x100;
    public static final int OPT_ALL = 0xffff;
    public static final int OPT_NONE = 0;

    private String relPath = null;
    private Boolean apmDir = null;
    private Directory parent = null;
    private IDirectory[] children = null;

    private int nonDirItemCount = 0;
    private int nonDirSubItemCount = 0;
    private int dirCount = 0;
    private int subDirCount = 0;

    private int iconID = 0;

    public Directory(String relPath, IDirectory parent, int nonDirItemCount) {
        this.setRelPath(relPath);
        this.setParent(parent);
        // this.setHasNonDirElements(hasNonDirElements);
        if (parent != null) {
            ((Directory) parent).addChild(this);
        }
        setNonDirItemCount(nonDirItemCount);
    }

    public static <T extends IDirectory> T[] removeChild(IDirectory<T> parent, T[] children, T... child) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < child.length; i++) {
            int index = parent.childIndexOf(child[i]);
            if (index >= 0 && !positions.contains(index)) {
                positions.add(index);
            }
        }
        return removeChild(parent, children, positions);
    }

    public static <T extends IDirectory> T[] removeChild(IDirectory<T> factory, T[] children, List<Integer> removePositions) {
        if (removePositions != null && removePositions.size() > 0) {
            T[] newArray = factory.createOsDirectoryArray(children.length - removePositions.size());
            int in = children.length;
            int out = newArray.length;
            while (--in >= 0) {
                if (!removePositions.contains(in)) {
                    newArray[--out] = children[in];
                }
            }
            return newArray;
        }
        return children;
    }

    public static <T extends IDirectory> T[] add(IDirectory<T> factory, T[] oldChildren, T... newChildren) {
        if (newChildren == null || newChildren.length == 0) return oldChildren;
        if (oldChildren == null || oldChildren.length == 0) return newChildren;

        T[] result = factory.createOsDirectoryArray(newChildren.length + oldChildren.length);
        System.arraycopy(oldChildren, 0, result, 0, oldChildren.length);

        System.arraycopy(newChildren, 0, result, oldChildren.length, newChildren.length);
        return result;
    }

    public static int getChildCount(IDirectory item) {
        if ((item != null) && (item.getChildDirs() != null)) return item.getChildDirs().length;
        return 0;
    }

    public static <T> int childIndexOf(T[] children, T child) {
        if (child != null && children != null) {
            for (int i = children.length - 1; i >= 0; i--) {
                if (child.equals(children[i])) return i;
            }
        }
        return -1;
    }

    protected static StringBuilder toTreeString(StringBuilder result, Directory item, String delimiter, int options) {
        if (item != null) {
            result.append(item.getRelPath());
            appendCount(result, item, options);
            result.append(delimiter);

            if (item.getChildDirs() != null) {
                for (IDirectory child : item.getChildDirs()) {
                    toTreeString(result, (Directory) child, delimiter, options);
                }
            }
        }
        return result;
    }

    private static IDirectory find(IDirectory parent, StringBuilder path) {
        while (path.indexOf(PATH_DELIMITER) == 0) {
            path.delete(0, PATH_DELIMITER.length());
        }

        int pathLen = path.length();
        if (pathLen == 0) return parent;

        if (parent.getChildDirs() != null) {
            for (IDirectory child : parent.getChildDirs()) {
                if (path.indexOf(child.getRelPath()) == 0) {
                    int childLen = child.getRelPath().length();
                    if (childLen == pathLen) return child; // found last path element
                    int end = path.indexOf(PATH_DELIMITER, childLen);

                    if (end == childLen) {
                        path.delete(0, childLen);
                        return find(child, path);
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void destroy() {
        if (children != null) {
            for (IDirectory child : getChildDirs()) {
                child.destroy();
            }
            children = null;
        }
        parent = null;
    }

    @Override
    public void removeChild(IDirectory... child) {
        children = Directory.removeChild(this, children, child);
    }

    /**
     * factory method to be overwrittern by derived classes, if tree should consist of derived classes.
     */
    @Override
    public Directory createOsDirectory(IFile file, IDirectory parent, IDirectory[] children) {
        final Directory result = new Directory(file.getName(), parent, 0);

        if (children != null) {
            for (IDirectory child : children) {
                addChild(child);
            }
        }
        return result;
    }

    /*------------------- simple properties ------------------------*/

    @Override
    public String getRelPath() {
        return relPath;
    }

    public void setRelPath(String relPath) {
        this.relPath = relPath;
    }

    private boolean isApmDir() {
        if (apmDir == null) {
            apmDir = new File(getAbsolute(), RuleFileNameProcessor.APM_FILE_NAME).exists();
        }
        return apmDir.equals(Boolean.TRUE);
    }

    @Override
    public IDirectory getParent() {
        return parent;
    }

    public void setParent(IDirectory parent) {
        this.parent = (Directory) parent;
    }

    @Override
    public int childIndexOf(IDirectory child) {
        return childIndexOf(children, child);
    }

    /**
     * factory method to be overwrittern by derived classes.
     */
    @Override
    public Directory[] createOsDirectoryArray(int size) {
        return new Directory[size];
    }

    /*------------------- formatting ------------------------*/

    @Override
    public String getAbsolute() {
        StringBuilder result = new StringBuilder();
        IDirectory current = this;

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

    public void addChild(IDirectory... child) {
        this.children = add(this, this.children, child);
    }

    public static void appendCount(StringBuilder result, IDirectory _item, int options) {
        if ((_item != null) && (_item instanceof Directory)) {
            Directory item = (Directory) _item;
            int dirCount = ((options & OPT_DIR) == 0) ? 0 : item.getDirCount();
            int subDirCount = ((options & OPT_SUB_DIR) == 0) ? 0 : item.getSubDirCount();
            int nonDirItemCount = ((options & OPT_ITEM) == 0) ? 0 : item.getNonDirItemCount();
            int nonDirSubItemCount = ((options & OPT_SUB_ITEM) == 0) ? 0 : item.getNonDirSubItemCount();

            boolean asHtml = (options & OPT_AS_HTML) != 0;
            appendCount(result, "(", dirCount, subDirCount, ")", asHtml);
            appendCount(result, ":(", nonDirItemCount, nonDirSubItemCount, ")", asHtml);
        }
    }

    private static void appendCount(StringBuilder result, String prefix, int count, int subCount, String suffix, boolean asHtml) {
        if ((count > 0) || (subCount > count)) {
            if (asHtml) result.append("<font color='gray'><small>");
            result.append(prefix);
            if (count > 0) result.append(count);
            if (subCount > count) result.append("+").append(subCount - count);
            result.append(suffix);
            if (asHtml) result.append("</small></font>");
        }
    }

    @Override
    public String toString() {
        return getAbsolute();
    }

    /*------------------- statistics ------------------------*/

    public IDirectory setNonDirItemCount(int nonDirItemCount) {
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

    public void addChildStatistics(int subDirCount, int nonDirSubItemCount, int iconID) {
        setDirCount(getDirCount() + 1);
        setSubDirCount(getSubDirCount() + subDirCount + 1);
        setNonDirSubItemCount(getNonDirSubItemCount() + nonDirSubItemCount);
        if (iconID > this.iconID) this.iconID = iconID;
    }

    public void setChildren(IDirectory[] children) {
        this.children = children;
    }

    @Override
    public IDirectory find(String path) {
        if (path != null) {
            return find(this, new StringBuilder(path));
        }
        return null;
    }

    @Override
    public IDirectory[] getChildDirs() {
        return children;
    }

    @Override
    public int getSelectionIconID() {
        return iconID;
    }

    @Override
    public int getDirFlags() {
        if (AlbumFile.isQueryFile(this.getRelPath())) return IDirectory.DIR_FLAG_VIRTUAL_DIR;
        return isApmDir() ? IDirectory.DIR_FLAG_APM_DIR : IDirectory.DIR_FLAG_NONE;
    }

    @Override
    public void refresh() {
        apmDir = null;
    }

    public IDirectory setIconID(int iconID) {
        this.iconID = iconID;
        return this;
    }

    /** #114: update internal data after a folder has been renamed in the gui */
    @Override
    public void rename(String oldFolderName, String newFolderName) {
        String relPath = this.getRelPath();
        relPath = relPath.replace(oldFolderName, newFolderName);
        this.relPath = relPath;
    }
}
