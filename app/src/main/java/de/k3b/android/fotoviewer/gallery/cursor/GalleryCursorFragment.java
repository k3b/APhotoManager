package de.k3b.android.fotoviewer.gallery.cursor;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.List;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.directory.DirectoryGui;
import de.k3b.android.fotoviewer.directory.DirectoryPickerFragment;
import de.k3b.android.fotoviewer.queries.FotoViewerParameter;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.OnGalleryInteractionListener;
import de.k3b.android.fotoviewer.queries.Queryable;
import de.k3b.io.Directory;

/**
 * A {@link Fragment} to show ImageGallery content based on ContentProvider-Cursor.
 * Activities that contain this fragment must implement the
 * {@link OnGalleryInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GalleryCursorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GalleryCursorFragment extends Fragment  implements Queryable, DirectoryGui {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";


    private HorizontalScrollView parentPathBarScroller;
    private LinearLayout parentPathBar;

    private HorizontalScrollView childPathBarScroller;
    private LinearLayout childPathBar;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // for debugging
    private static int id = 1;
    private final String debugPrefix;

    private GridView galleryView;
    private GalleryCursorAdapter galleryAdapter = null;

    private OnGalleryInteractionListener mGalleryListener;
    private QueryParameterParcelable mGalleryContentQuery;

    private DirectoryPickerFragment.OnDirectoryInteractionListener mDirectoryListener;

    /**************** construction ******************/
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GalleryCursorFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GalleryCursorFragment newInstance(String param1, String param2) {
        GalleryCursorFragment fragment = new GalleryCursorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public GalleryCursorFragment() {
        debugPrefix = "GalleryCursorFragment#" + (id++)  + " ";
        // Required empty public constructor
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "()");
        }

    }

    /**************** live-cycle ******************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View result = inflater.inflate(R.layout.fragment_gallery, container, false);
        galleryView = (GridView) result.findViewById(R.id.gridView);

        galleryAdapter = new GalleryCursorAdapter(this.getActivity(), mGalleryContentQuery, debugPrefix);
        galleryView.setAdapter(galleryAdapter);

        galleryView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onGalleryImageClick((GalleryCursorAdapter.GridCellViewHolder) v.getTag());
            }
        });

        this.parentPathBar = (LinearLayout) result.findViewById(R.id.parent_owner);
        this.parentPathBarScroller = (HorizontalScrollView) result.findViewById(R.id.parent_scroller);

        this.childPathBar = (LinearLayout) result.findViewById(R.id.child_owner);
        this.childPathBarScroller = (HorizontalScrollView) result.findViewById(R.id.child_scroller);

        reloadDirGuiIfAvailable();

        return result;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mGalleryListener = (OnGalleryInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnGalleryInteractionListener");
        }

        try {
            mDirectoryListener = (DirectoryPickerFragment.OnDirectoryInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DirectoryPickerFragment.OnDirectoryInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mGalleryListener = null;
        mDirectoryListener = null;
    }

    /**
     * interface Queryable: Initiates a database requery in the background
     *
     * @param context
     * @param parameters
     */
    @Override
    public void requery(Activity context, QueryParameterParcelable parameters) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + "requery " + ((parameters != null) ? parameters.toSqlString() : null));
        }

        this.mGalleryContentQuery = parameters;

        galleryAdapter.requery(this.getActivity(), mGalleryContentQuery);
    }

    @Override
    public String toString() {
        return debugPrefix + this.galleryAdapter;
    }

    /*********************** local helper *******************************************/
    /** an Image in the FotoGallery was clicked */
    private void onGalleryImageClick(final GalleryCursorAdapter.GridCellViewHolder holder) {
        if ((mGalleryListener != null) && (mGalleryContentQuery != null)) {
            QueryParameterParcelable imageQuery = new QueryParameterParcelable(mGalleryContentQuery);

            if (holder.filter != null) {
                FotoSql.addWhereFilter(imageQuery, holder.filter);
            }
            mGalleryListener.onGalleryImageClick(null, getUri(holder.imageID), holder.description.getText().toString(), imageQuery);
        }
    }

    /** converts imageID to content-uri */
    private Uri getUri(long imageID) {
        return Uri.parse(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString() + "/" + imageID);
    }

    /****************** path navigation *************************/

    private Directory mDirectoryRoot = null;
    private int mDirQueryID = 0;
    private String mCurrentPath = null;

    /** Defines Directory Navigation */
    @Override
    public void defineDirectoryNavigation(Directory root, int dirTypId, String initialAbsolutePath) {
        mDirectoryRoot = root;
        mDirQueryID = dirTypId;
        navigateTo(initialAbsolutePath);
    }

    /** Set curent selection to absolutePath */
    @Override
    public void navigateTo(String absolutePath) {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, debugPrefix + " navigateTo : " + absolutePath);
        }

        mCurrentPath = absolutePath;
        reloadDirGuiIfAvailable();
        // requeryGallery(); done by owning activity
    }

    private void reloadDirGuiIfAvailable() {
        if ((mDirectoryRoot != null) && (mCurrentPath != null) && (parentPathBar != null)) {

            parentPathBar.removeAllViews();
            childPathBar.removeAllViews();

            Directory selectedChild = mDirectoryRoot.find(mCurrentPath);
            if (selectedChild == null) selectedChild = mDirectoryRoot;

            Button first = null;
            Directory current = selectedChild;
            while (current.getParent() != null) {
                Button button = createPathButton(current);
                // add parent left to chlild
                // gui order root/../child.parent/child
                parentPathBar.addView(button, 0);
                if (first == null) first = button;
                current = current.getParent();
            }

            // scroll to right where deepest child is
            if (first != null) parentPathBarScroller.requestChildFocus(parentPathBar, first);

            List<Directory> children = selectedChild.getChildren();
            if (children != null) {
                for (Directory child : children) {
                    Button button = createPathButton(child);
                    childPathBar.addView(button);
                }
            }
        }
    }

    private Button createPathButton(Directory currentDir) {
        Button result = new Button(getActivity());
        result.setTag(currentDir);
        result.setText(getDirectoryDisplayText(null, currentDir, (FotoViewerParameter.includeSubItems) ? Directory.OPT_SUB_ITEM : Directory.OPT_ITEM));

        result.setOnClickListener(onPathButtonClickListener);
        return result;
    }

    /** path/directory was clicked */
    private View.OnClickListener onPathButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onPathButtonClick((Directory) v.getTag());
        }
    };

    /** path/directory was clicked */
    private void onPathButtonClick(Directory newSelection) {
        if ((mDirectoryListener != null) && (newSelection != null)) {
            mCurrentPath = newSelection.getAbsolute();
            mDirectoryListener.onDirectoryPick(mCurrentPath, this.mDirQueryID);
        }
    }

    /** getFrom tree display text */
    private static String getDirectoryDisplayText(String prefix, Directory directory, int options) {
        StringBuilder result = new StringBuilder();
        if (prefix != null) result.append(prefix);
        result.append(directory.getRelPath()).append(" ");
        Directory.appendCount(result, directory, options);
        return result.toString();
    }
}
