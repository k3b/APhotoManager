package de.k3b.io;

import java.util.List;

/**
 * android independant navigation for andoid-s ExpandableListView(Adapter).
 *
 * Created by k3b on 12.06.2015.
 */
public class DirectoryNavigator implements IExpandableListViewNavigation<Directory, Directory> {

    public static final int UNDEFINED = -1;

    private final Directory root;
    private Directory currentSelection = null;
    private int mLastNavigateToGroupPosition = UNDEFINED;
    private int mLastNavigateToChildPosition = UNDEFINED;

    /** hierachy: root -> .... -> currentGrandFather -> group -> child -> ... */
    private Directory currentGrandFather;

    public DirectoryNavigator(Directory root) {
        this.root = root;
        this.setCurrentGrandFather(root);
    }

    public Directory getRoot() {
        return root;
    }

    /**
     * ************ api close to adapter *********************
     */
    @Override
    public int getGroupCount() {
        return Directory.getChildCount(currentGrandFather);
    }

    @Override
    public int getChildrenCount(int groupIndex) {
        Directory group = getGroup(groupIndex);
        return Directory.getChildCount(group);
    }

    @Override
    public Directory getChild(int groupIndex, int childIndex) {
        Directory group = getGroup(groupIndex);
        if ((childIndex < 0) || (childIndex >= Directory.getChildCount(group))) {
            throw new IndexOutOfBoundsException(
                    "getChild(childIndex=" +
                            childIndex +
                            "): index must be 0 .. " +
                            (Directory.getChildCount(group) - 1));
        }
        return group.getChildren().get(childIndex);
    }

    @Override
    public Directory getGroup(int groupIndex) {
        if ((groupIndex < 0) || (groupIndex >= getGroupCount())) {
            throw new IndexOutOfBoundsException(
                    "getGroup(" +
                    groupIndex +
                    "): index must be 0 .. " +
                    (getGroupCount() - 1));
        }
        return currentGrandFather.getChildren().get(groupIndex);
    }

    public void setCurrentGrandFather(Directory currentGrandFather) {
        this.currentGrandFather = currentGrandFather;
    }

    public void navigateTo(Directory newSelection) {
        Directory navigationGrandparent = getNavigationGrandparent(newSelection);
        setCurrentGrandFather(navigationGrandparent);
        setCurrentSelection(newSelection);
    }

    /** package to allow unit testing: calclulate Grandparent for navigateTo */
    Directory getNavigationGrandparent(Directory newSelection) {
        if (newSelection != null) {
            Directory parent = newSelection.getParent();
            if (parent != null) {
                if (Directory.getChildCount(newSelection) > 0) {
                    this.mLastNavigateToGroupPosition = parent.getChildren().indexOf(newSelection);
                    this.mLastNavigateToChildPosition = UNDEFINED;
                    return parent;
                }
                Directory grandparent = parent.getParent();
                if (grandparent != null) {
                    this.mLastNavigateToGroupPosition = grandparent.getChildren().indexOf(parent);
                    this.mLastNavigateToChildPosition = parent.getChildren().indexOf(newSelection);
                    return grandparent;
                }
            }
        }
        mLastNavigateToGroupPosition = UNDEFINED;
        mLastNavigateToChildPosition = UNDEFINED;

        return getRoot();
    }

    /** direct access from room via index. return null if index does not exist. */
    public Directory getSubChild(int... indexes) {
        Directory found = this.getRoot();
        if (indexes != null) {
            for (int index : indexes) {
                List<Directory> children = (found != null) ? found.getChildren() : null;
                int childCount = (children != null) ? children.size() : 0;
                if ((index < 0) || (index >= childCount)) {
                    return null;
                }
                found = children.get(index);
            }
        }

        return found;
    }

    public Directory getCurrentSelection() {
        return currentSelection;
    }

    public void setCurrentSelection(Directory currentSelection) {
        this.currentSelection = currentSelection;
    }

    /** last navigateTo() treeview position */
    public int getLastNavigateToGroupPosition() {
        return mLastNavigateToGroupPosition;
    }

    /** last navigateTo() treeview position */
    public int getLastNavigateToChildPosition() {
        return mLastNavigateToChildPosition;
    }
}
