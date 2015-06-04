package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import de.k3b.android.database.QueryParameterParcelable;
import de.k3b.android.fotoviewer.gallery.cursor.FotoSql;

public class GalleryActivity extends Activity implements
        OnGalleryInteractionListener {

    public static final String EXTRA_QUERY = "query";
    private QueryParameterParcelable parameters = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery); // .gallery_activity);

        this.parameters = getIntent().getParcelableExtra(EXTRA_QUERY);
        if (parameters == null) parameters = FotoSql.queryDirs;

        setTitle(parameters.getID(), getIntent().getStringExtra(Intent.EXTRA_TITLE));

        Queryable query = (Queryable) getFragmentManager().findFragmentById(R.id.galleryCursor);

        if (query != null) {
            query.requery(this,parameters);
        }
    }

    /** GalleryFragment tells the Owning Activity that an Item in the FotoGallery was clicked */
    @Override
    public void onGalleryClick(Bitmap image, Uri imageUri, String description, QueryParameterParcelable parentQuery) {
        Intent intent;
        if ((parentQuery != null) && (parentQuery.getID() == R.string.directory_gallery) ) {
            //Create intent
            intent = new Intent(this, this.getClass());

            QueryParameterParcelable newQuery = new QueryParameterParcelable(FotoSql.queryDetail);
            newQuery.setWhere(parentQuery,false).setOrderBy(parentQuery,false);

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
        String title = this.getTitle().toString();
        if (count > 0) {
            if (!title.contains(" - ") || !title.contains("(") || !title.contains(")")) {
                setTitle(this.parameters.getID(), "(" + count  + ")");
            }
        }
    }

    private void setTitle(int id, String description) {
        String title = getString(id);

        if (null != description) title += " - " + description;
        this.setTitle(title);
    }
}
