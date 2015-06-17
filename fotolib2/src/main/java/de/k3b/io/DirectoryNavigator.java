package de.k3b.io;

/**
 * android independant navigation for andoid-s ExpandableListView(Adapter).
 *
 * Created by k3b on 12.06.2015.
 */
public class DirectoryNavigator implements IExpandableListViewNavigation<Directory, Directory> {

    private final Directory root;

    /** hierachy: root -> .... -> currentGrandFather -> group -> child -> ... */
    private Directory currentGrandFather;

    public DirectoryNavigator(Directory root) {
        this.root = root;
        this.setCurrentGrandFather(root);
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

    public Directory getRoot() {
        return root;
    }
}
