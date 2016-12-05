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

import java.util.List;

/**
 * Class to collect Directories and results in a normalized
 * Directory-Structure, where paths can be combined.
 *
 * Created by k3b on 04.06.2015.
 */
public class DirectoryBuilder {
    private Directory root;

    public DirectoryBuilder() {
        root = null;
    }

    public IDirectory getRoot() {
        if (root != null) {
            List<IDirectory> children = root.getChildren();
            compress(children);
            createStatistics(children);
        }
        return root;
    }

    public static void createStatistics(List<IDirectory> children) {
        if (children != null) {
            for (IDirectory _child: children) {
                Directory child = (Directory) _child;
                child.setNonDirSubItemCount(child.getNonDirItemCount()).setDirCount(0).setSubDirCount(0);
                createStatistics(child.getChildren());
                IDirectory parent = child.getParent();
                if (parent != null) {
                    ((Directory)parent).addChildStatistics(child.getSubDirCount(), child.getNonDirSubItemCount(), child.getSelectionIconID());
                }
            }
        }
    }

    private void compress(Directory firstChild) {
        int mergeCound = 0;
        Directory item = firstChild;
        while ((item != null) && (item.getNonDirItemCount() <= 0)) {
            item = mergeDirWithChildIfPossible(firstChild);
            mergeCound++;
        }
        List<IDirectory> children = (firstChild != null) ? firstChild.getChildren() : null;

        if ((mergeCound > 0) && (children != null)) {
            for (IDirectory _child: children) {
                Directory child = (Directory) _child;
                child.setParent(firstChild);
            }

        }
        compress(children);
    }

    private void compress(List<IDirectory> children) {
        if (children != null) {
            for (IDirectory _child: children) {
                Directory child = (Directory) _child;
                compress(child);
            }
        }
    }

    private Directory mergeDirWithChildIfPossible(Directory firstChild) {
        List<IDirectory> children = (firstChild != null) ? firstChild.getChildren() : null;

        if ((children != null) && children.size() == 1) {
            Directory child = (Directory) children.get(0);
            firstChild.setRelPath(firstChild.getRelPath() + Directory.PATH_DELIMITER + child.getRelPath());
            firstChild.setNonDirItemCount(firstChild.getNonDirItemCount() + child.getNonDirItemCount());

            children = child.getChildren();
            firstChild.setChildren(children);

            child.setParent(null);
            child.setChildren(null);
            return firstChild;
        }
        return null;
    }

    public DirectoryBuilder add(String absolutePath, int nonDirItemCount, int iconID) {
        if (root == null) {
            root = new Directory("", null, 0);
        }
        addPath(absolutePath.split(Directory.PATH_DELIMITER), 0, root, nonDirItemCount, iconID);
        return this;
    }

    private IDirectory addPath(String[] elements, int level, Directory root, int nonDirItemCount, int iconID) {
        Directory result = addPath(elements, level, root, iconID);

        if (result != null) {
            result.setNonDirItemCount(result.getNonDirItemCount() + nonDirItemCount);
        }
        return result;
    }

    private Directory addPath(String[] elements, int level, Directory root, int iconID) {

        if ((elements == null) || (level >= elements.length))
            return root; // end of recursion

        String serach = elements[level];

        if ((serach == null) || (serach.length() == 0))
            return addPath(elements, level + 1, root, iconID);

        List<IDirectory> children = root.getChildren();
        if (children != null) {
            for (IDirectory _child: children) {
                Directory child = (Directory) _child;
                if (serach.compareToIgnoreCase(child.getRelPath()) == 0) {
                    return addPath(elements, level+1, child, iconID);
                }
            }
        }

        Directory result = new Directory(serach, root, 0);
        result.setIconID(iconID);
        return addPath(elements, level+1, result, iconID);
    }
}
