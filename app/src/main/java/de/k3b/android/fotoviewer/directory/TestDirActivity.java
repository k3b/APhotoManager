package de.k3b.android.fotoviewer.directory;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import de.k3b.android.fotoviewer.Global;
import de.k3b.android.fotoviewer.R;
import de.k3b.io.Directory;

public class TestDirActivity extends Activity implements DirectoryFragment.OnDirectoryInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dir);
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
     */
    @Override
    public void onDirectorySelected(Directory newSelection) {
        Log.d(Global.LOG_CONTEXT, "Activity-Dir:onOk: " + newSelection);

        Toast.makeText(this, newSelection.getAbsolute(), Toast.LENGTH_LONG);
    }

    /**
     * called when user cancels selection of a new directory
     */
    @Override
    public void onDirectorySelectCancel() {
        Log.d(Global.LOG_CONTEXT, "Activity-Dir:onCancel: ");

        Toast.makeText(this, R.string.cancel, Toast.LENGTH_LONG);
    }
}
