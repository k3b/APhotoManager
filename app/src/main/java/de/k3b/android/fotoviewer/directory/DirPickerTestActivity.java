package de.k3b.android.fotoviewer.directory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import de.k3b.android.fotoviewer.queries.FotoViewerParameter;
import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.fotoviewer.GalleryActivity;
import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.Queryable;
import de.k3b.io.Directory;

public class DirPickerTestActivity extends Activity implements DirectoryPickerFragment.OnDirectoryInteractionListener {

    /** which query is used to get the directories */
    private int dirQueryID = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dir_choose);

        Queryable query = (Queryable) getFragmentManager().findFragmentById(R.id.galleryCursor);


        if (query != null) {

            QueryParameterParcelable currentDirContentQuery = FotoViewerParameter.currentDirContentQuery;
            query.requery(this, currentDirContentQuery);
            this.dirQueryID = (currentDirContentQuery != null) ? currentDirContentQuery.getID() : 0;
        }
    }


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

    /**
     * called when user selects a new directory
     *
     * @param newSelection
     * @param queryTypeId
     */
    @Override
    public void onDirectoryPick(Directory newSelection, int queryTypeId) {
        Log.d(Global.LOG_CONTEXT, "Activity-Dir:onOk: " + newSelection);

        String selectedAbsolutePath = newSelection.getAbsolute();
        Toast.makeText(this, selectedAbsolutePath, Toast.LENGTH_LONG);

        Intent intent = new Intent(this, GalleryActivity.class);

        QueryParameterParcelable newQuery = new QueryParameterParcelable(FotoSql.queryDetail);
        FotoSql.addPathWhere(newQuery, selectedAbsolutePath, dirQueryID);
        intent.putExtra(GalleryActivity.EXTRA_QUERY, newQuery);
        String title = selectedAbsolutePath + "/* - " + getString(R.string.foto_gallery);
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
        Log.d(Global.LOG_CONTEXT, "Activity-Dir:onCancel: ");

        Toast.makeText(this, R.string.cancel, Toast.LENGTH_LONG);
    }

    /**
     * called after the selection in tree has changed
     *
     * @param selectedChild
     * @param queryTypeId
     */
    @Override
    public void onDirectorySelectionChanged(Directory selectedChild, int queryTypeId) {

    }
}
