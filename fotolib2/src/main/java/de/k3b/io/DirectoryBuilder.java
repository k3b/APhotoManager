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

    public Directory getRoot() {
        if (root != null) {
            List<Directory> children = root.getChildren();
            compress(children);
            createStatistics(children);
        }
        return root;
    }

    public static void createStatistics(List<Directory> children) {
        if (children != null) {
            for (Directory child: children) {
                child.setNonDirSubItemCount(child.getNonDirItemCount()).setDirCount(0).setSubDirCount(0);
                createStatistics(child.getChildren());
                Directory parent = child.getParent();
                if (parent != null) {
                    parent.addChildStatistics(child.getSubDirCount(), child.getNonDirSubItemCount(), child.getIconID());
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
        List<Directory> children = (firstChild != null) ? firstChild.getChildren() : null;

        if ((mergeCound > 0) && (children != null)) {
            for (Directory child: children) {
                child.setParent(firstChild);
            }

        }
        compress(children);
    }

    private void compress(List<Directory> children) {
        if (children != null) {
            for (Directory child: children) {
                compress(child);
            }
        }
    }

    private Directory mergeDirWithChildIfPossible(Directory firstChild) {
        List<Directory> children = (firstChild != null) ? firstChild.getChildren() : null;

        if ((children != null) && children.size() == 1) {
            Directory child = children.get(0);
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

    private Directory addPath(String[] elements, int level, Directory root, int nonDirItemCount, int iconID) {
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

        List<Directory> children = root.getChildren();
        if (children != null) {
            for (Directory child : children) {
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
