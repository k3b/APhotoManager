package de.k3b.android.fotoviewer.directory;

/**
 * Created by EVE on 12.06.2015.
 */
public interface IExpandableListViewNavigation<TGroupType, TChildType> {
    /*************** api close to adapter **********************/

    //counts the number of group/parent items so the list knows how many times calls getGroupView() method
    int getGroupCount();

    //counts the number of children items so the list knows how many times calls getChildView() method
    int getChildrenCount(int groupIndex);

    //gets child at index
    TGroupType getChild(int groupIndex, int childIndex);

    //gets group at index
    TGroupType getGroup(int groupIndex);
}
