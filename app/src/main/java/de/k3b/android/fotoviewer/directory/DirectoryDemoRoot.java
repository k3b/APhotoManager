package de.k3b.android.fotoviewer.directory;

import java.util.ArrayList;
import java.util.Random;

import de.k3b.io.Directory;

/**
 * Created by k3b on 11.06.2015.
 */
public class DirectoryDemoRoot extends Directory implements IExpandableListViewNavigation<DirectoryDemoData,DirectoryDemoData> {
    private ArrayList<DirectoryDemoData> mParent = new ArrayList<>();

    public static IExpandableListViewNavigation<DirectoryDemoData,DirectoryDemoData> getCategories() {
        ArrayList<DirectoryDemoData> categories = new ArrayList<DirectoryDemoData>();
        for(int i = 0; i < 10 ; i++) {
            DirectoryDemoData cat = new DirectoryDemoData("Category "+i);
            generateChildren(cat);
            categories.add(cat);
        }
        DirectoryDemoRoot root = new DirectoryDemoRoot(categories);

        return root;
    }

    // generate some random amount of child objects (1..10)
    static void generateChildren(DirectoryDemoData parent) {
        Random rand = new Random();
        for(int i=0; i < rand.nextInt(9)+1; i++) {
            DirectoryDemoData cat = new DirectoryDemoData("Child "+i);
            parent.children.add(cat);
        }
    }

    public DirectoryDemoRoot(ArrayList<DirectoryDemoData> categories) {
        super("", null, 0);
        mParent = categories;
    }

    /*************** api close to adapter **********************/

    //counts the number of group/parent items so the list knows how many times calls getGroupView() method
    @Override
    public int getGroupCount() {
        return mParent.size();
    }

    //counts the number of children items so the list knows how many times calls getChildView() method
    @Override
    public int getChildrenCount(int groupIndex) {
        return getGroup(groupIndex).children.size();
    }

    //gets child at index
    @Override
    public DirectoryDemoData getChild(int groupIndex, int childIndex) {
        return getGroup(groupIndex).children.get(childIndex);
    }

    //gets group at index
    @Override
    public DirectoryDemoData getGroup(int groupIndex) {
        return mParent.get(groupIndex);
    }
}
