package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import de.k3b.android.fotoviewer.directory.DirectoryGui;
import de.k3b.android.fotoviewer.directory.DirectoryLoaderTask;
import de.k3b.android.fotoviewer.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.fotoviewer.directory.DirectoryPickerFragment;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.FotoViewerParameter;
import de.k3b.android.fotoviewer.queries.Queryable;
import de.k3b.android.util.GarbageCollector;
import de.k3b.io.Directory;

public class FotoGalleryActivity extends Activity implements
        OnGalleryInteractionListener, DirectoryPickerFragment.OnDirectoryInteractionListener {
    private static final String debugPrefix = "GalA-";

    /** intent parameters supported by FotoGalleryActivity: EXTRA_... */
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

    /** true if activity should show navigator dialog after loading mDirectoryRoot is complete */
    private boolean mMustShowNavigator = false;

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        saveSettings();

        // save InstanceState
        savedInstanceState.putInt(STATE_DirQueryID, getDirQueryID());
        savedInstanceState.putString(STATE_CurrentPath, mCurrentPath);

        super.onSaveInstanceState(savedInstanceState);
    }

    private void saveSettings() {
        // save settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = sharedPref.edit();

        edit.putInt(STATE_DirQueryID, getDirQueryID());
        edit.putString(STATE_CurrentPath, mCurrentPath);
        edit.commit();
    }

    // load from settings/instanceState
    private void loadSettingsAndInstanceState(Bundle savedInstanceState) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentPath = sharedPref.getString(STATE_CurrentPath, mCurrentPath);
        mDirQueryID = sharedPref.getInt(STATE_DirQueryID, getDirQueryID());

        // instance state overrides settings
        if (savedInstanceState != null) {
            mCurrentPath = savedInstanceState.getString(STATE_CurrentPath, mCurrentPath);
            mDirQueryID = savedInstanceState.getInt(STATE_DirQueryID, getDirQueryID());
        }

        // extra parameter
        this.mGalleryContentQuery = getIntent().getParcelableExtra(EXTRA_QUERY);
        if (mGalleryContentQuery == null) mGalleryContentQuery = FotoSql.getQuery(FotoSql.QUERY_TYPE_DEFAULT);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(debugPrefix, "onCreate");
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

        setTitle();
        reloadGui();
    }

    @Override
    protected void onPause () {
        Global.debugMemory(debugPrefix, "onPause");
        saveSettings();
        super.onPause();
    }

    @Override
    protected void onResume () {
        Global.debugMemory(debugPrefix, "onResume");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Global.debugMemory(debugPrefix, "onDestroy start");
        super.onDestroy();

        // to avoid memory leaks
        GarbageCollector.freeMemory(findViewById(R.id.root_view));

        mGalleryContentQuery = null;
        mGalleryGui = null;
        mDirGui = null;

        if (mDirectoryRoot != null)
        {
            mDirectoryRoot.destroy();
            mDirectoryRoot = null;
        }
        System.gc();
        Global.debugMemory(debugPrefix, "onDestroy end");
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
        if (mDirectoryRoot == null) {
            // not loaded yet. load directoryRoot in background
            final QueryParameterParcelable currentDirContentQuery = FotoSql.getQuery(this.getDirQueryID());
            this.mDirQueryID = (currentDirContentQuery != null) ? currentDirContentQuery.getID() : FotoSql.QUERY_TYPE_UNDEFINED;

            if (currentDirContentQuery != null) {
                this.mMustShowNavigator = true;
                DirectoryLoaderTask loader = new DirectoryLoaderTask(this, debugPrefix) {
                    protected void onPostExecute(Directory directoryRoot) {
                        onDirectoryDataLoadComplete(directoryRoot);
                    }
                };
                loader.execute(currentDirContentQuery);
            } else {
                Log.e(Global.LOG_CONTEXT, debugPrefix + " this.mDirQueryID undefined " + this.mDirQueryID);
            }
        } else {
            this.mMustShowNavigator = false;
            final FragmentManager manager = getFragmentManager();
            DirectoryPickerFragment dirDialog = new DirectoryPickerFragment(); // (DirectoryPickerFragment) manager.findFragmentByTag(DLG_NAVIGATOR_TAG);
            dirDialog.defineDirectoryNavigation(mDirectoryRoot, getDirQueryID(), mCurrentPath);

            dirDialog.show(manager, DLG_NAVIGATOR_TAG);
        }
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
    public void onGalleryImageClick(long imageId, Uri imageUri, int position) {
        Global.debugMemory(debugPrefix, "onGalleryImageClick");
        Intent intent;
        //Create intent
        intent = new Intent(this, ImageDetailActivityViewPager.class);

        intent.putExtra(ImageDetailActivityViewPager.EXTRA_QUERY, calculateEffectiveGalleryContentQuery() );
        intent.putExtra(ImageDetailActivityViewPager.EXTRA_POSITION, position);
        intent.setData(imageUri);

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
            Log.d(Global.LOG_CONTEXT, "FotoGalleryActivity.navigateTo " + selectedAbsolutePath + " from " + mCurrentPath);
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
        if (directoryRoot == null) {
            final String message = getString(R.string.err_load_dir_failed, getString(this.getDirQueryID()));
            Toast.makeText(this, message,Toast.LENGTH_LONG).show();
        } else {
            mDirectoryRoot = directoryRoot;
            if ((mDirGui != null) && (mCurrentPath != null)) {
                mDirGui.defineDirectoryNavigation(directoryRoot, getDirQueryID(), mCurrentPath);
            }
            Global.debugMemory(debugPrefix, "onDirectoryDataLoadComplete");

            if ((mDirectoryRoot != null) && (this.mMustShowNavigator)) {
                openNavigator();
            }
        }
    }


    /** combine root-query plus current selected directory */
    private QueryParameterParcelable calculateEffectiveGalleryContentQuery() {
        if (mGalleryContentQuery == null) return null;
        QueryParameterParcelable result = new QueryParameterParcelable(mGalleryContentQuery);

        if (mCurrentPath != null) {
            FotoSql.addPathWhere(result, mCurrentPath, getDirQueryID());
        }
        return result;
    }

    private void setTitle() {
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if ((title == null) && (mCurrentPath != null)) {
            title = getString(getDirQueryID()) + " - " + mCurrentPath;
        }

        if (title != null) {
            this.setTitle(title + mTitleResultCount);
        }
    }

    /** one of the FotoSql.QUERY_TYPE_xxx values. if undefined use default */
    private int getDirQueryID() {
        if (mDirQueryID == FotoSql.QUERY_TYPE_UNDEFINED)
            return FotoSql.QUERY_TYPE_GROUP_DEFAULT;

        return mDirQueryID;
    }
}
