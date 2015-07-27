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
 
package de.k3b.android.androFotoFinder.directory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

// import com.squareup.leakcanary.RefWatcher;

import de.k3b.android.androFotoFinder.FotoGalleryActivity;
import de.k3b.android.androFotoFinder.queries.QueryParameterParcelable;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.io.Directory;

public class DirPickerTestActivity extends Activity implements DirectoryPickerFragment.OnDirectoryInteractionListener {

    private static final String DBG_PREFIX = "TestGui-";

    /** which query is used to get the directories: one of the FotoSql.QUERY_TYPE_xxx values */

    private int dirQueryID = 0;
    private DirectoryGui dirGui;

    /************ life cycle *********************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dir_choose);

        dirGui = (DirectoryGui) getFragmentManager().findFragmentById(R.id.directoryFragment);

        if (dirGui != null) {

            final QueryParameterParcelable currentDirContentQuery = FotoSql.getQuery(FotoSql.QUERY_TYPE_GROUP_DEFAULT);
            this.dirQueryID = (currentDirContentQuery != null) ? currentDirContentQuery.getID() : 0;

            DirectoryLoaderTask loader = new DirectoryLoaderTask(this, DBG_PREFIX) {
                protected void onPostExecute(Directory directoryRoot) {
                    onDirectoryDataLoadComplete(directoryRoot);
                }
            };
            loader.execute(currentDirContentQuery);
        }
    }

    private void onDirectoryDataLoadComplete(Directory directoryRoot) {
        dirGui.defineDirectoryNavigation(directoryRoot, dirQueryID, "/");
    }

    @Override public void onDestroy() {
        super.onDestroy();
        // RefWatcher refWatcher = AndroFotoFinderApp.getRefWatcher(this);
        // refWatcher.watch(this);
    }

    /************ menu *********************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test_dir, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /************** DirectoryPickerFragment.OnDirectoryInteractionListener callback *******/
    /**
     * called when user selects a new directory
     *
     * @param selectedAbsolutePath
     * @param queryTypeId
     */
    @Override
    public void onDirectoryPick(String selectedAbsolutePath, int queryTypeId) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, DBG_PREFIX + "onDirectoryPick: " + selectedAbsolutePath);
        }

        Toast.makeText(this, selectedAbsolutePath, Toast.LENGTH_LONG);

        Intent intent = new Intent(this, FotoGalleryActivity.class);

        QueryParameterParcelable newQuery = new QueryParameterParcelable(FotoSql.queryDetail);
        FotoSql.addPathWhere(newQuery, selectedAbsolutePath, dirQueryID);
        intent.putExtra(FotoGalleryActivity.EXTRA_QUERY, newQuery);
        String title = selectedAbsolutePath + "/* - " + getString(R.string.gallery_foto);
        intent.putExtra(Intent.EXTRA_TITLE, title);
        //Start details activity
        startActivity(intent);
    }

    /**
     * called when user cancels selection of a new directory
     * @param queryTypeId
     */
    @Override
    public void onDirectoryCancel(int queryTypeId) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, DBG_PREFIX + "onDirectoryCancel: ");
        }


        Toast.makeText(this, R.string.cancel, Toast.LENGTH_LONG);
    }

    /**
     * called after the selection in tree has changed
     *
     * @param selectedChild
     * @param queryTypeId
     */
    @Override
    public void onDirectorySelectionChanged(String selectedChild, int queryTypeId) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, DBG_PREFIX + "onDirectorySelectionChanged: " + selectedChild);
        }
    }
}
