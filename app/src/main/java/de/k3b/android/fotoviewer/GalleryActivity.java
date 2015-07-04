package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.k3b.android.fotoviewer.directory.DirectoryGui;
import de.k3b.android.fotoviewer.directory.DirectoryLoaderTask;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.fotoviewer.directory.DirectoryPickerFragment;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.FotoViewerParameter;
import de.k3b.android.fotoviewer.queries.Queryable;
import de.k3b.io.Directory;

public class GalleryActivity extends Activity implements
        OnGalleryInteractionListener, DirectoryPickerFragment.OnDirectoryInteractionListener {
    private static final String DBG_PREFIX = "Gal-";

    /** intent parameters supported by GalleryActivity: EXTRA_... */
    public static final String EXTRA_QUERY = "gallery";
    public static final String EXTRA_TITLE = Intent.EXTRA_TITLE;

    private static final String STATE_CurrentPath = "CurrentPath";
    private static final String STATE_DirQueryID = "DirQueryID";

    private static final String DLG_NAVIGATOR_TAG = "navigator";

    private QueryParameterParcelable mGalleryContentQuery = null;
    private Queryable mGalleryGui;

    private boolean mHasEmbeddedDirPicker = false;
    private DirectoryGui mDirGui;
    /** one of the FotoSql.QUERY_TYPE_xxx values */
    private int mDirQueryID = FotoSql.QUERY_TYPE_GROUP_DEFAULT;

    private String mCurrentPath = "/";

    private String mTitleResultCount = "";

    private Directory mDirectoryRoot = null;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        saveSettings();


        // save InstanceState
        savedInstanceState.putInt(STATE_DirQueryID, mDirQueryID);
        savedInstanceState.putString(STATE_CurrentPath, mCurrentPath);

        super.onSaveInstanceState(savedInstanceState);
    }

    private void saveSettings() {
        // save settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = sharedPref.edit();
        edit.putInt(STATE_DirQueryID, mDirQueryID);
        edit.putString(STATE_CurrentPath, mCurrentPath);
        edit.commit();
    }

    // load from settings/instanceState
    private void loadSettingsAndInstanceState(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentPath = sharedPref.getString(STATE_CurrentPath, mCurrentPath);
        mDirQueryID = sharedPref.getInt(STATE_DirQueryID, mDirQueryID);

        // instance state overrides settings
        if (savedInstanceState != null) {
            mCurrentPath = savedInstanceState.getString(STATE_CurrentPath, mCurrentPath);
            mDirQueryID = savedInstanceState.getInt(STATE_DirQueryID,mDirQueryID);
        }

        // extra parameter
        this.mGalleryContentQuery = getIntent().getParcelableExtra(EXTRA_QUERY);
        if (mGalleryContentQuery == null) mGalleryContentQuery = FotoSql.getQuery(FotoSql.QUERY_TYPE_DEFAULT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery); // .gallery_activity);

        loadSettingsAndInstanceState(savedInstanceState);

        FragmentManager fragmentManager = getFragmentManager();
        mGalleryGui = (Queryable) fragmentManager.findFragmentById(R.id.galleryCursor);

        // on tablet seperate dir navigator fragment
        mDirGui = (DirectoryGui) fragmentManager.findFragmentById(R.id.directoryFragment);

        if (FotoViewerParameter.galleryHasEmbeddedDirPicker) {
            if (mDirGui == null) {
                // on small screen/cellphone DirectoryGui is part of gallery
                mDirGui = (DirectoryGui) fragmentManager.findFragmentById(R.id.galleryCursor);
            } else {
                mHasEmbeddedDirPicker = true;
            }
        } else {
            if (mDirGui != null) {
                fragmentManager.beginTransaction().remove((Fragment) mDirGui).commit();
                mDirGui = null;
            }
        }

        // load directoryRoot in background
        final QueryParameterParcelable currentDirContentQuery = FotoSql.getQuery(this.mDirQueryID);
        this.mDirQueryID = (currentDirContentQuery != null) ? currentDirContentQuery.getID() : FotoSql.QUERY_TYPE_UNDEFINED;

        if (currentDirContentQuery != null) {
            DirectoryLoaderTask loader = new DirectoryLoaderTask(this, DBG_PREFIX) {
                protected void onPostExecute(Directory directoryRoot) {
                    onDirectoryDataLoadComplete(directoryRoot);
                }
            };
            loader.execute(currentDirContentQuery);
        }

        setTitle();
        reloadGui();
    }

    @Override
    protected void onPause () {
        saveSettings();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_gallery, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.cmd_navigator:
                openNavigator();
                return true;
            case R.id.cmd_filter:
                openFilter();
                return true;
            case R.id.cmd_sort:
                openSort();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void openNavigator() {
        final FragmentManager manager = getFragmentManager();
        DirectoryPickerFragment dir = new DirectoryPickerFragment(); // (DirectoryPickerFragment) manager.findFragmentByTag(DLG_NAVIGATOR_TAG);
        dir.defineDirectoryNavigation(mDirectoryRoot, mDirQueryID, mCurrentPath);

        dir.show(manager, DLG_NAVIGATOR_TAG);

    }

    private void openFilter() {
    }

    private void openSort() {
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);;
        startActivity(intent);
    }

    /** called by Fragment: a fragment Item was clicked */
    @Override
    public void onGalleryImageClick(Bitmap image, Uri imageUri, String description, QueryParameterParcelable parentQuery) {
        Intent intent;
        if ((parentQuery != null) && (parentQuery.getID() == FotoSql.QUERY_TYPE_GROUP_ALBUM) ) {
            //Create intent
            intent = new Intent(this, this.getClass());

            QueryParameterParcelable newQuery = new QueryParameterParcelable(FotoSql.queryDetail);
            newQuery.getWhereFrom(parentQuery, false).getOrderByFrom(parentQuery, false);

            intent.putExtra(EXTRA_QUERY, newQuery);
        } else {

            //Create intent
            intent = new Intent(this, ImageViewActivity.class);

            if (image != null)
                intent.putExtra(ImageViewActivity.EXTRA_IMAGE, image); // does not work for images > 1mb. there we need to use uri-s instead
            if (imageUri != null) intent.setData(imageUri);

        }
        intent.putExtra(Intent.EXTRA_TITLE, description);
        //Start details activity
        startActivity(intent);
    }

    /** GalleryFragment tells the Owning Activity that querying data has finisched */
    @Override
    public void setResultCount(int count) {
        this.mTitleResultCount = (count > 0) ? ("(" + count + ")") : "";
        setTitle();
    }

    /**
     * called when user selects a new directory
     *
     * @param selectedAbsolutePath
     * @param queryTypeId
     */
    @Override
    public void onDirectoryPick(String selectedAbsolutePath, int queryTypeId) {
        if (!this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
    }

    /**
     * called when user cancels selection of a new directory
     * @param queryTypeId
     */
    @Override
    public void onDirectoryCancel(int queryTypeId) {
        // do nothing
    }

    /** called after the selection in tree has changed */
    @Override
    public void onDirectorySelectionChanged(String selectedAbsolutePath, int queryTypeId) {
        if (this.mHasEmbeddedDirPicker) {
            navigateTo(selectedAbsolutePath, queryTypeId);
        }
    }

    private void navigateTo(String selectedAbsolutePath, int queryTypeId) {
        if (mCurrentPath.compareTo(selectedAbsolutePath) != 0) {
            Log.d(Global.LOG_CONTEXT, "GalleryActivity.navigateTo " + selectedAbsolutePath + " from " + mCurrentPath);
            mCurrentPath = selectedAbsolutePath;
            mDirQueryID = queryTypeId;
            setTitle();

            reloadGui();
        }
    }

    private void reloadGui() {
        if ((mGalleryGui != null) && (mGalleryContentQuery != null)) {
            this.mGalleryGui.requery(this, calculateEffectiveGalleryContentQuery());
        }
        if ((mDirGui != null) && (mCurrentPath != null)) {
            mDirGui.navigateTo(mCurrentPath);
        }
    }

    private void onDirectoryDataLoadComplete(Directory directoryRoot) {
        mDirectoryRoot = directoryRoot;
        if ((mDirGui != null) && (mCurrentPath != null)) {
            mDirGui.defineDirectoryNavigation(directoryRoot, mDirQueryID, mCurrentPath);
        }
    }


    /** combine root-query plus current selected directory */
    private QueryParameterParcelable calculateEffectiveGalleryContentQuery() {
        if (mGalleryContentQuery == null) return null;
        QueryParameterParcelable result = new QueryParameterParcelable(mGalleryContentQuery);

        if ((mDirQueryID != 0) && (mCurrentPath != null)) {
            FotoSql.addPathWhere(result, mCurrentPath, mDirQueryID);
        }
        return result;
    }

    private void setTitle() {
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if ((title == null) && (mDirQueryID != 0) && (mCurrentPath != null)) {
            title = getString(mDirQueryID) + " - " + mCurrentPath;
        }

        if (title != null) {
            this.setTitle(title + mTitleResultCount);
        }
    }
}
