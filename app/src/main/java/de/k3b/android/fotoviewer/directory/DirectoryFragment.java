package de.k3b.android.fotoviewer.directory;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import de.k3b.android.fotoviewer.R;
import de.k3b.io.Directory;
import de.k3b.io.DirectoryBuilder;
import de.k3b.io.DirectoryNavigator;
import de.k3b.io.IExpandableListViewNavigation;

import java.util.Comparator;

/**
 * A fragment with a Listing of Directories.
 * Activities that contain this fragment must implement the
 * {@link OnDirectoryInteractionListener} interface
 * to handle interaction events.
 */
public class DirectoryFragment extends Fragment {

    private DirectoryListAdapter adapter;
    private ExpandableListView listView;
    private IExpandableListViewNavigation<Directory,Directory> navigation;

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
        listView = (ExpandableListView)view.findViewById(R.id.categories);
        Directory directories = DirectoryLoader.getDirectories();
        DirectoryBuilder.createStatistics(directories.getChildren());
        navigation = new DirectoryNavigator(directories);

        adapter = new DirectoryListAdapter(mContext,
                navigation, listView);
        listView.setAdapter(adapter);

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
