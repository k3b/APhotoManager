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

/**
 * android independent abstraction to andoid-s ExpandableListView(Adapter)
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
