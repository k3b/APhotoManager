package de.k3b.android.fotoviewer.directory;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import de.k3b.android.fotoviewer.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A fragment with a Listing of Directories.
 * Activities that contain this fragment must implement the
 * {@link OnDirectoryInteractionListener} interface
 * to handle interaction events.
 */
public class DirectoryFragment extends Fragment {

    private DirectoryListAdapter adapter;
    private ExpandableListView categoriesList;
    private ArrayList<Directory> categories;

    protected Activity mContext;

    private OnDirectoryInteractionListener mListener;

    public DirectoryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_directory, container, false);

        mContext = this.getActivity();
        categoriesList = (ExpandableListView)view.findViewById(R.id.categories);
        categories = Directory.getCategories();
        adapter = new DirectoryListAdapter(mContext,
                categories, categoriesList);
        categoriesList.setAdapter(adapter);

        categoriesList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {


                CheckedTextView checkbox = (CheckedTextView)v.findViewById(R.id.list_item_text_child);
                checkbox.toggle();


                // find parent view by tag
                View parentView = categoriesList.findViewWithTag(categories.get(groupPosition).name);
                if(parentView != null) {
                    TextView sub = (TextView)parentView.findViewById(R.id.list_item_text_subscriptions);

                    if(sub != null) {
                        Directory directory = categories.get(groupPosition);
                        if(checkbox.isChecked()) {
                            // add child category to parent's selection list
                            directory.selection.add(checkbox.getText().toString());

                            // sort list in alphabetical order
                            Collections.sort(directory.selection, new CustomComparator());
                        }
                        else {
                            // remove child category from parent's selection list
                            directory.selection.remove(checkbox.getText().toString());
                        }

                        // display selection list
                        sub.setText(directory.selection.toString());
                    }
                }
                return true;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnDirectoryInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDirectoryInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnDirectoryInteractionListener {
        // TODO: Update argument type and name
        // public void onFragmentInteraction(Uri uri);
    }

    public class CustomComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    }

}
