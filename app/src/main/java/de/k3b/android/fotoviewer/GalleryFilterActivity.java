package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;

import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.directory.DirectoryLoaderTask;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.GalleryFilter;
import de.k3b.android.fotoviewer.queries.GalleryFilterParcelable;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.io.Directory;

public class GalleryFilterActivity extends Activity {
    private static final String debugPrefix = "GalF-";

    private static final String EXTRA_FILTER = "mFilter";
    public static final int resultID = 522;
    private HashMap<Integer, Directory> directories = new HashMap<Integer, Directory>();

    public static void showActivity(Activity context, GalleryFilterParcelable filter) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, context.getClass().getSimpleName()
                    + " > GalleryFilterActivity.showActivity");
        }

        final Intent intent = new Intent().setClass(context,
                GalleryFilterActivity.class);

        intent.putExtra(EXTRA_FILTER, filter);

        context.startActivityForResult(intent, resultID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_filter);
        onCreateButtos();
    }

    private void onCreateButtos() {
        Button cmd = (Button) findViewById(R.id.cmd_path);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParams(FotoSql.queryGroupByDir);
            }
        });
        cmd = (Button) findViewById(R.id.cmd_date);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParams(FotoSql.queryGroupByDate);
            }
        });
        cmd = (Button) findViewById(R.id.cmd_lat_lon);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParams(FotoSql.queryGroupByDate);
            }
        });
    }

    private void getParams(final QueryParameterParcelable currentDirContentQuery) {
        Directory directory = directories.get(currentDirContentQuery.getID());
        if (directory == null) {
            DirectoryLoaderTask loader = new DirectoryLoaderTask(this, debugPrefix) {
                protected void onPostExecute(Directory directoryRoot) {
                    onDirectoryDataLoadComplete(directoryRoot, currentDirContentQuery.getID());
                }
            };
            loader.execute(currentDirContentQuery);
        } else {
            onDirectoryDataLoadComplete(directory, currentDirContentQuery.getID());
        }
    }

    private void onDirectoryDataLoadComplete(Directory directoryRoot, int queryId) {
        /*
        directories.put(queryId, directoryRoot);
        if (directoryRoot == null) {
            final String message = getString(R.string.err_load_dir_failed, FotoSql.getName(this, this.galleryQueryParameter.getDirQueryID()));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            mDirectoryRoot = directoryRoot;
            if ((mDirGui != null) && (this.galleryQueryParameter.mCurrentPath != null)) {
                mDirGui.defineDirectoryNavigation(directoryRoot, this.galleryQueryParameter.getDirQueryID(), this.galleryQueryParameter.mCurrentPath);
            }
            Global.debugMemory(debugPrefix, "onDirectoryDataLoadComplete");

            if ((mDirectoryRoot != null) && (this.mMustShowNavigator)) {
                openNavigator();
            }
        }
        */
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gallery_filter, menu);
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
}
