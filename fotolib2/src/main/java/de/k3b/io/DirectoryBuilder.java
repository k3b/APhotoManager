package de.k3b.io;

import java.util.List;

/**
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
            /*
            if (children != null) {
                for (Directory child: children) {
                    if (!child.getRelPath().startsWith(Directory.PATH_DELIMITER)) {
                        child.setRelPath(Directory.PATH_DELIMITER + child.getRelPath());
                    }
                }
            }
            */
        }
        return root;
    }

    private void compress(Directory root) {
        Directory item = root;
        while ((item != null) && (item.getNonDirItemCount() <= 0)) {
            item = mergeDirWithChildIfPossible(root);
        }
        List<Directory> children = (root != null) ? root.getChildren() : null;
        compress(children);
    }

    private void compress(List<Directory> children) {
        if (children != null) {
            for (Directory child: children) {
                compress(child);
            }
        }
    }

    private Directory mergeDirWithChildIfPossible(Directory parent) {
        List<Directory> children = (parent != null) ? parent.getChildren() : null;

        if ((children != null) && children.size() == 1) {
            Directory child = children.get(0);
            child.setParent(parent);
            parent.setRelPath(parent.getRelPath() + Directory.PATH_DELIMITER + child.getRelPath());
            parent.setNonDirItemCount(parent.getNonDirItemCount() + child.getNonDirItemCount());

            children = child.getChildren();
            parent.setChildren(children);

            child.setChildren(null);
            return child;
        }
        return null;
    }

    public DirectoryBuilder add(String absolutePath, int nonDirItemCount) {
        if (root == null) {
            root = new Directory("", null, 0);
        }
        addPath(absolutePath.split(Directory.PATH_DELIMITER), 0, root, nonDirItemCount);
        return this;
    }

    private Directory addPath(String[] elements, int level, Directory root, int nonDirItemCount) {
        Directory result = addPath(elements, level, root);

        if (result != null) {
            result.setNonDirItemCount(result.getNonDirItemCount() + nonDirItemCount);
        }
        return result;
    }

    private Directory addPath(String[] elements, int level, Directory root) {

        if ((elements == null) || (level >= elements.length))
            return root; // end of recursion

        String serach = elements[level];

        if ((serach == null) || (serach.length() == 0))
            return addPath(elements, level + 1, root);

        List<Directory> children = root.getChildren();
        if (children != null) {
            for (Directory child : children) {
                if (serach.compareToIgnoreCase(child.getRelPath()) == 0) {
                    return addPath(elements, level+1, child);
                }
            }
        }

        Directory result = new Directory(serach, root, 0);
        return addPath(elements, level+1, result);
    }
}
