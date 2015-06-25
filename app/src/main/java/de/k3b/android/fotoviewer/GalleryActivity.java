package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import de.k3b.android.fotoviewer.queries.QueryParameterParcelable;
import de.k3b.android.fotoviewer.directory.DirectoryFragment;
import de.k3b.android.fotoviewer.queries.FotoSql;
import de.k3b.android.fotoviewer.queries.FotoViewerParameter;
import de.k3b.android.fotoviewer.queries.Queryable;
import de.k3b.io.Directory;

public class GalleryActivity extends Activity implements
        OnGalleryInteractionListener, DirectoryFragment.OnDirectoryInteractionListener {

    public static final String EXTRA_QUERY = "query";
    private static final String DLG_NAVIGATOR = "navigator";
    private QueryParameterParcelable parameters = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery); // .gallery_activity);

        this.parameters = getIntent().getParcelableExtra(EXTRA_QUERY);
        if (parameters == null) parameters = FotoViewerParameter.currentDirContentQuery;

        setTitle(parameters.getID(), getIntent().getStringExtra(Intent.EXTRA_TITLE));

        Queryable query = (Queryable) getFragmentManager().findFragmentById(R.id.galleryCursor);

        if (query != null) {
            query.requery(this,parameters);
        }
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
        DirectoryFragment dir = new DirectoryFragment(); // (DirectoryFragment) manager.findFragmentByTag(DLG_NAVIGATOR);

        dir.show(manager, DLG_NAVIGATOR);

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
    public void onGalleryClick(Bitmap image, Uri imageUri, String description, QueryParameterParcelable parentQuery) {
        Intent intent;
        if ((parentQuery != null) && (parentQuery.getID() == R.string.directory_gallery) ) {
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
        String countText = "(" + count + ")";
        String title = this.getTitle().toString();
        if (!title.contains(countText)) {
            setTitle(title + " - " + countText);
        }
    }

    /**
     * called when user selects a new directory
     *
     * @param newSelection
     */
    @Override
    public void onDirectorySelected(Directory newSelection) {

    }

    /**
     * called when user cancels selection of a new directory
     */
    @Override
    public void onDirectorySelectCancel() {

    }

    private void setTitle(int id, String description) {
        String title = getString(id);

        if (null != description) title += " - " + description;
        this.setTitle(title);
    }

}
