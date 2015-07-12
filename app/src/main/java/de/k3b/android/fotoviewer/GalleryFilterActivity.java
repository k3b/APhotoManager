package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.k3b.android.fotoviewer.directory.DirectoryLoaderTask;
import de.k3b.android.fotoviewer.directory.DirectoryPickerFragment;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.GalleryFilter;
import de.k3b.android.fotoviewer.queries.GalleryFilterParcelable;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.io.Directory;

public class GalleryFilterActivity extends Activity implements DirectoryPickerFragment.OnDirectoryInteractionListener {
    private static final String debugPrefix = "GalF-";

    private static final String EXTRA_FILTER = "Filter";
    public static final int resultID = 522;
    private static final String DLG_NAVIGATOR_TAG = "GalleryFilterActivity";

    GalleryFilterParcelable mFilter = null;
    private EditText mPath;

    private EditText mDateFrom;
    private EditText mDateTo;
    private EditText mLongitudeFrom;
    private EditText mLongitudeTo;
    private EditText mLatitudeTo;

    private EditText mLatitudeFrom;

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

        this.mPath = (EditText) findViewById(R.id.edit_path);
        this.mDateFrom = (EditText) findViewById(R.id.edit_date_from);
        this.mDateTo = (EditText) findViewById(R.id.edit_date_to);
        this.mLatitudeFrom = (EditText) findViewById(R.id.edit_latitude_from);
        this.mLatitudeTo = (EditText) findViewById(R.id.edit_latitude_to);
        this.mLongitudeFrom = (EditText) findViewById(R.id.edit_longitude_from);
        this.mLongitudeTo = (EditText) findViewById(R.id.edit_longitude_to);

        Intent intent = getIntent();
        mFilter = intent.getParcelableExtra(EXTRA_FILTER);

        toGui(mFilter);
    }

    private void onCreateButtos() {
        Button cmd = (Button) findViewById(R.id.cmd_path);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirectoryPicker(FotoSql.queryGroupByDir);
            }
        });
        cmd = (Button) findViewById(R.id.cmd_date);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirectoryPicker(FotoSql.queryGroupByDate);
            }
        });
        cmd = (Button) findViewById(R.id.cmd_lat_lon);
        cmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirectoryPicker(FotoSql.queryGroupByPlace);
            }
        });
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

    private void toGui(GalleryFilter gf) {
        mPath           .setText(gf.getPath());
        mDateFrom       .setText(d(gf.getDateMin()));
        mDateTo         .setText(d(gf.getDateMax()));
        mLongitudeFrom  .setText(l(gf.getLogituedMin()));
        mLongitudeTo    .setText(l(gf.getLogituedMax()));
        mLatitudeFrom   .setText(l(gf.getLatitudeMin()));
        mLatitudeTo     .setText(l(gf.getLatitudeMax()));
    }

    private String l(double latLon) {
        if (latLon == 0) return "";
        return Double.toString(latLon);
    }

    final private static java.text.DateFormat isoDateformatter = new SimpleDateFormat(
            "yyyy-MM-dd", Locale.GERMANY);


    private String d(long dateMin) {
        if (dateMin == 0) return "";
        return isoDateformatter.format(new Date(dateMin));
    }

    /**************** DirectoryPicker *****************/
    private static class DirInfo {
        int queryId = 0;
        Directory directoryRoot = null;
        String currentPath = null;

    }
    private HashMap<Integer, DirInfo> dirInfos = new HashMap<Integer, DirInfo>();
    DirInfo getOrCreateDirInfo(int queryId) {
        DirInfo result = dirInfos.get(queryId);
        if (result == null) {
            result = new DirInfo();
            result.queryId = queryId;
            dirInfos.put(queryId, result);
        }
        return result;
    }

    private void showDirectoryPicker(final QueryParameterParcelable currentDirContentQuery) {
        Directory directoryRoot = getOrCreateDirInfo(currentDirContentQuery.getID()).directoryRoot;
        if (directoryRoot == null) {
            DirectoryLoaderTask loader = new DirectoryLoaderTask(this, debugPrefix) {
                protected void onPostExecute(Directory directoryRoot) {
                    onDirectoryDataLoadComplete(directoryRoot, currentDirContentQuery.getID());
                }
            };
            loader.execute(currentDirContentQuery);
        } else {
            onDirectoryDataLoadComplete(directoryRoot, currentDirContentQuery.getID());
        }
    }

    private void onDirectoryDataLoadComplete(Directory directoryRoot, int queryId) {
        if (directoryRoot != null) {
            DirInfo dirInfo = getOrCreateDirInfo(queryId);
            dirInfo.directoryRoot = directoryRoot;
            final FragmentManager manager = getFragmentManager();
            DirectoryPickerFragment dirDialog = new DirectoryPickerFragment();
            dirDialog.defineDirectoryNavigation(dirInfo.directoryRoot, dirInfo.queryId, dirInfo.currentPath);

            dirDialog.show(manager, DLG_NAVIGATOR_TAG);
        }
    }

    /**
     * called when user picks a new directory
     */
    @Override
    public void onDirectoryPick(String selectedAbsolutePath, int queryTypeId) {
        DirInfo dirInfo = getOrCreateDirInfo(queryTypeId);
        dirInfo.currentPath=selectedAbsolutePath;

        mFilter.set(selectedAbsolutePath, queryTypeId);
        toGui(mFilter);
    }

    /** interface DirectoryPickerFragment.OnDirectoryInteractionListener not used */
    @Override
    public void onDirectoryCancel(int queryTypeId) {}

    /** interface DirectoryPickerFragment.OnDirectoryInteractionListener not used */
    @Override
    public void onDirectorySelectionChanged(String selectedChild, int queryTypeId) {}



}
