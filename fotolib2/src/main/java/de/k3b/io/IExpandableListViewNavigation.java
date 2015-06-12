package de.k3b.io;

/**
 * android independant abstraction to andoid-s ExpandableListView(Adapter)
 * Created by k3b on 12.06.2015.
 */
public interface IExpandableListViewNavigation<TGroupType, TChildType> {
    /*************** api close to adapter **********************/

    /** counts the number of group/parent items so the list knows how many times calls getGroupView() method */
    int getGroupCount();

    /** counts the number of children items so the list knows how many times calls getChildView() method */
    int getChildrenCount(int groupIndex);

     /** gets group at index */
    TGroupType getGroup(int groupIndex);

    /** gets child at index */
    TChildType getChild(int groupIndex, int childIndex);
}
