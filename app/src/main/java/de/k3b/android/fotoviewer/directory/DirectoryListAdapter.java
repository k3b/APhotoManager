package de.k3b.android.fotoviewer.directory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import de.k3b.android.fotoviewer.R;

/**
 * 
 */

public class DirectoryListAdapter extends BaseExpandableListAdapter {
 
 
    private LayoutInflater inflater;
    private DirectoryDemoRoot mParent;
    private ExpandableListView accordion;
    public int lastExpandedGroupPosition;    
    
 
    public DirectoryListAdapter(Context context, DirectoryDemoRoot parent, ExpandableListView accordion) {
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
    public Object getGroup(int groupId) {
        return mParent.getGroup(groupId);
    }
 
    @Override
    //gets the name of each item
    public Object getChild(int groupId, int childId) {
        return mParent.getChild(groupId, childId);
    }
 
    @Override
    public long getGroupId(int groupId) {
        return groupId;
    }
 
    @Override
    public long getChildId(int groupId, int childId) {
        return childId;
    }
 
    @Override
    public boolean hasStableIds() {
        return true;
    }
 
    @Override
    //in this method you must set the text to see the parent/group on the list
    public View getGroupView(int groupId, boolean b, View view, ViewGroup viewGroup) {
    	
        if (view == null) {
            view = inflater.inflate(R.layout.directory_list_item_parent, viewGroup,false);
        }
        // set category name as tag so view can be found view later
        DirectoryDemoData group = mParent.getGroup(groupId);
        view.setTag(group);
        
        TextView textView = (TextView) view.findViewById(R.id.list_item_text_view);
        
        //"groupId" is the position of the parent/group in the list
        textView.setText(group.toString());
        
        TextView sub = (TextView) view.findViewById(R.id.list_item_text_subscriptions);

        if(group.selection.size()>0) {
            sub.setText(group.selection.toString());
        }
        else {
        	sub.setText("");
        }
        
        //return the entire view
        return view;
    }
    
 
    @Override
    //in this method you must set the text to see the children on the list
    public View getChildView(int groupId, int childId, boolean b, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = inflater.inflate(R.layout.directory_list_item_child, viewGroup,false);
        }
 
        
        CheckedTextView textView = (CheckedTextView) view.findViewById(R.id.list_item_text_child);
        
        //"groupId" is the position of the parent/group in the list and
        //"childId" is the position of the child
        DirectoryDemoData child = mParent.getChild(groupId, childId);
        textView.setText(child.name);
 
        // set checked if parent category selection contains child category
        DirectoryDemoData group = mParent.getGroup(groupId);
        if(group.selection.contains(textView.getText().toString())) {
    		textView.setChecked(true);
        }
        else {
        	textView.setChecked(false);
        }
        
        //return the entire view
        return view;
    }
 
    @Override
    public boolean isChildSelectable(int groupId, int childId) {
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

    /** data belonging to list element */
    static class DirectoryViewHolder {
        final public TextView description;

        DirectoryViewHolder(View parent) {
            this.description = (TextView) parent.findViewById(R.id.text);
        };

        public long imageID;
    }



}

