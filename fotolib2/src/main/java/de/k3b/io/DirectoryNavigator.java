/*
 * Copyright (c) 2015-2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager
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
 * android independant navigation for andoid-s ExpandableListView(Adapter).
 *
 * Created by k3b on 12.06.2015.
 */
public class DirectoryNavigator implements IExpandableListViewNavigation<IDirectory,IDirectory> {

    public static final int UNDEFINED = -1;

    private final IDirectory root;
    private IDirectory currentSelection = null;
    private int mLastNavigateToGroupPosition = UNDEFINED;
    private int mLastNavigateToChildPosition = UNDEFINED;

    /** hierachy: root -> .... -> currentGrandFather -> group -> child -> ... */
    private IDirectory currentGrandFather;

    public DirectoryNavigator(IDirectory root) {
        this.root = root;
        this.setCurrentGrandFather(root);
    }

    public IDirectory getRoot() {
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
        if ((groupIndex < 0) || (groupIndex >= getGroupCount())) {
            return 0;
        }
        IDirectory group = getGroup(groupIndex);
        return Directory.getChildCount(group);
    }

    @Override
    public IDirectory getChild(int groupIndex, int childIndex) {
        IDirectory group = getGroup(groupIndex);
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
    public IDirectory getGroup(int groupIndex) {
        if ((groupIndex < 0) || (groupIndex >= getGroupCount())) {
            throw new IndexOutOfBoundsException(
                    "getGroup(" +
                    groupIndex +
                    "): index must be 0 .. " +
                    (getGroupCount() - 1));
        }
        return currentGrandFather.getChildren().get(groupIndex);
    }

    public void setCurrentGrandFather(IDirectory currentGrandFather) {
        this.currentGrandFather = currentGrandFather;
    }

    public void navigateTo(IDirectory newSelection) {
        IDirectory navigationGrandparent = getNavigationGrandparent(newSelection);
        setCurrentGrandFather(navigationGrandparent);
        setCurrentSelection(newSelection);
    }

    /** protected to allow unit testing: calclulate Grandparent for navigateTo */
    protected IDirectory getNavigationGrandparent(IDirectory newSelection) {
        if (newSelection != null) {
            IDirectory parent = newSelection.getParent();
            if (parent != null) {
                if (Directory.getChildCount(newSelection) > 0) {
                    this.mLastNavigateToGroupPosition = parent.getChildren().indexOf(newSelection);
                    this.mLastNavigateToChildPosition = UNDEFINED;
                    return parent;
                }
                IDirectory grandparent = parent.getParent();
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
    public IDirectory getSubChild(int... indexes) {
        IDirectory found = this.getRoot();
        if (indexes != null) {
            for (int index : indexes) {
                List<IDirectory> children = (found != null) ? found.getChildren() : null;
                int childCount = (children != null) ? children.size() : 0;
                if ((index < 0) || (index >= childCount)) {
                    return null;
                }
                found = children.get(index);
            }
        }

        return found;
    }

    public IDirectory getCurrentSelection() {
        return currentSelection;
    }

    public void setCurrentSelection(IDirectory currentSelection) {
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
