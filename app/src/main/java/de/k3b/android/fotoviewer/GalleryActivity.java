package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

public class GalleryActivity extends Activity implements
        OnGalleryInteractionListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery); // .gallery_activity);
    }

    @Override
    public void onGalleryClick(int activityType, Bitmap image, Uri imageUri, String description, String sqlWhere, String[] sqlWhereParameter) {
        //Create intent
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putExtra("title", description);

        if (image != null) intent.putExtra("image", image); // does not work for images > 1mb. there we need to use uri-s instead
        if (imageUri != null) intent.setData(imageUri);

        //Start details activity
        startActivity(intent);
    }
}
