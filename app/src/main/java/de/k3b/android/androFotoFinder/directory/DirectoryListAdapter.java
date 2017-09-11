/*
 * Copyright (c) 2015-2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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
 
package de.k3b.android.androFotoFinder.directory;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.io.Directory;
import de.k3b.io.IDirectory;
import de.k3b.io.IExpandableListViewNavigation;

/**
 * Maps android independent IExpandableListViewNavigation to android specific ExpandableListAdapter so it can be viewed in ExpandableList
 */

public class DirectoryListAdapter extends BaseExpandableListAdapter implements IExpandableListViewNavigation<Object, Object> {


    private LayoutInflater inflater;
    private IExpandableListViewNavigation<IDirectory,IDirectory> mParent;
    private ExpandableListView accordion;
    public int lastExpandedGroupPosition;

    // for debugging
    private static int id = 1;
    // private final String debugPrefix;


    public DirectoryListAdapter(Context context, IExpandableListViewNavigation<IDirectory, IDirectory> parent, ExpandableListView accordion, String name) {
        String debugPrefix = "DirectoryListAdapter#" + (id++) + "@" + name + " ";
        Global.debugMemory(debugPrefix, "ctor");
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }

        mParent = parent;
        inflater = LayoutInflater.from(context);
        this.accordion = accordion;

	}
 
 
    @Override
    //counts the number of group/parent items so the list knows how many times calls getGroupView() method
    public int getGroupCount() {
        return mParent.getGroupCount();
    }
 
    @Override
    //counts the number of children items so the list knows how many times calls getChildView() method
    public int getChildrenCount(int i) {
        return mParent.getChildrenCount(i);
    }
 
    @Override
    //gets the title of each parent/group
    public Object getGroup(int groupIndex) {
        return mParent.getGroup(groupIndex);
    }
 
    @Override
    //gets the name of each item
    public Object getChild(int groupIndex, int childIndex) {
        return mParent.getChild(groupIndex, childIndex);
    }
 
    @Override
    public long getGroupId(int groupIndex) {
        return groupIndex;
    }
 
    @Override
    public long getChildId(int groupIndex, int childIndex) {
        return childIndex;
    }
 
    @Override
    public boolean hasStableIds() {
        return true;
    }
 
    @Override
    //in this method you must getFrom the text to see the parent/group on the list
    public View getGroupView(int groupIndex, boolean b, View _view, ViewGroup viewGroup) {

        View view = (_view == null)
                ? inflater.inflate(R.layout.list_item_directory_parent, viewGroup,false)
                : _view;
        // getFrom category name as tag so view can be found view later
        IDirectory group = mParent.getGroup(groupIndex);
        view.setTag(group);
        
        TextView textView = (TextView) view.findViewById(R.id.list_item_text_view);
        
        //"groupIndex" is the position of the parent/group in the list
        textView.setText(getDirectoryDisplayText(null, group, Directory.OPT_ALL));

        //return the entire view
        return view;
    }

    @Override
    //in this method you must getFrom the text to see the children on the list
    public View getChildView(int groupIndex, int childIndex, boolean b, View _view, ViewGroup viewGroup) {
        View view = (_view == null)
                ? inflater.inflate(R.layout.list_item_directory_child, viewGroup,false)
                : _view;


        TextView textView = (TextView) view.findViewById(R.id.list_item_text_child);
        
        //"groupIndex" is the position of the parent/group in the list and
        //"childIndex" is the position of the child
        IDirectory child = mParent.getChild(groupIndex, childIndex);
        textView.setText(getDirectoryDisplayText("- ", child, Directory.OPT_ALL));
 
        //return the entire view
        return view;
    }
 
    @Override
    public boolean isChildSelectable(int groupIndex, int childIndex) {
        return true;
    }
    
    @Override
    /**
     * automatically collapse last expanded group
     * @see http://stackoverflow.com/questions/4314777/programmatically-collapse-a-group-in-expandablelistview
     */    
    public void onGroupExpanded(int groupPosition) {
    	
    	if(groupPosition != lastExpandedGroupPosition){
            accordion.collapseGroup(lastExpandedGroupPosition);
        }
    	
        super.onGroupExpanded(groupPosition);
     
        lastExpandedGroupPosition = groupPosition;
        
    }

    /** getFrom tree display text */
    public static Spanned getDirectoryDisplayText(String prefix, IDirectory directory, int options) {
        StringBuilder result = new StringBuilder();

        String formatPrefix = "";
        String formatSuffix = "";
        int flags = directory.getDirFlags();
        switch (flags) {
            case IDirectory.DIR_FLAG_NOMEDIA:
                formatPrefix = "[";
                formatSuffix = "]";
                break;
            case IDirectory.DIR_FLAG_NOMEDIA_ROOT:
                formatPrefix = "*[";
                formatSuffix = "]";
                break;
            case IDirectory.DIR_FLAG_APM_DIR:
                formatPrefix = IDirectory.APM_DIR_PREFIX;
                break;
            case IDirectory.DIR_FLAG_NONE:
                if ((options & Directory.OPT_AS_HTML) != 0) {
                    formatPrefix = "<b>";
                    formatSuffix = "</b>";
                }
                break;
            default:
                throw new IllegalStateException();
        }

        if (prefix != null) result.append(prefix);
        result.append(formatPrefix);
        result.append(directory.getRelPath()).append(" ");
        result.append(formatSuffix);

        Directory.appendCount(result, directory, options);
        return Html.fromHtml(result.toString());
    }
}
