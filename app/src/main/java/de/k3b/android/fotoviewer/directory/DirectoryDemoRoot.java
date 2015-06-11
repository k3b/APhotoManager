package de.k3b.android.fotoviewer.directory;

import java.util.ArrayList;

import de.k3b.io.Directory;

/**
 * Created by k3b on 11.06.2015.
 */
public class DirectoryDemoRoot extends Directory {
    private ArrayList<DirectoryDemoData> mParent = new ArrayList<>();

    public DirectoryDemoRoot(ArrayList<DirectoryDemoData> categories) {
        super("", null, 0);
        mParent = categories;
    }

    /*************** api close to adapter **********************/

    //counts the number of group/parent items so the list knows how many times calls getGroupView() method
    public int getGroupCount() {
        return mParent.size();
    }

    //counts the number of children items so the list knows how many times calls getChildView() method
    public int getChildrenCount(int groupId) {
        return getGroup(groupId).children.size();
    }

    //gets the title of each parent/group
    public String getGroupName(int groupId) {
        return getGroup(groupId).name;
    }

    //gets the name of each item
    public DirectoryDemoData getChild(int groupId, int childId) {
        return getGroup(groupId).children.get(childId);
    }

    public DirectoryDemoData getGroup(int groupId) {
        return mParent.get(groupId);
    }

    public long getGroupId(int groupId) {
        return groupId;
    }

    public long getChildId(int groupId, int childId) {
        return childId;
    }

}
