package de.k3b.android.fotoviewer.directory;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.io.Directory;
import de.k3b.io.IExpandableListViewNavigation;

/**
 * 
 */

public class DirectoryListAdapter extends BaseExpandableListAdapter implements IExpandableListViewNavigation<Object, Object> {
 
 
    private LayoutInflater inflater;
    private IExpandableListViewNavigation<Directory,Directory> mParent;
    private ExpandableListView accordion;
    public int lastExpandedGroupPosition;

    // for debugging
    private static int id = 1;
    private final String debugPrefix;


    public DirectoryListAdapter(Context context, IExpandableListViewNavigation<Directory, Directory> parent, ExpandableListView accordion, String name) {
        debugPrefix = "GalleryCursorAdapter#" + (id++) + "@" + name + " ";
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
    public View getGroupView(int groupIndex, boolean b, View view, ViewGroup viewGroup) {
    	
        if (view == null) {
            view = inflater.inflate(R.layout.directory_list_item_parent, viewGroup,false);
        }
        // getFrom category name as tag so view can be found view later
        Directory group = mParent.getGroup(groupIndex);
        view.setTag(group);
        
        TextView textView = (TextView) view.findViewById(R.id.list_item_text_view);
        
        //"groupIndex" is the position of the parent/group in the list
        textView.setText(getDirectoryDisplayText(null, group, Directory.OPT_ALL));
        
        //return the entire view
        return view;
    }

    /** getFrom tree display text */
    static String getDirectoryDisplayText(String prefix, Directory directory, int options) {
        StringBuilder result = new StringBuilder();
        if (prefix != null) result.append(prefix);
        result.append(directory.getRelPath()).append(" ");
        Directory.appendCount(result, directory, options);
        return result.toString();
    }

    @Override
    //in this method you must getFrom the text to see the children on the list
    public View getChildView(int groupIndex, int childIndex, boolean b, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.directory_list_item_child, viewGroup,false);
        }
 
        
        TextView textView = (TextView) view.findViewById(R.id.list_item_text_child);
        
        //"groupIndex" is the position of the parent/group in the list and
        //"childIndex" is the position of the child
        Directory child = mParent.getChild(groupIndex, childIndex);
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
}
