package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import de.k3b.android.fotoviewer.gallery.array.GalleryArrayFragment;
import de.k3b.android.fotoviewer.gallery.cursor.GalleryCursorFragment;

public class GalleryActivity extends Activity implements
        GalleryArrayFragment.OnGalleryInteractionListener,
        GalleryCursorFragment.OnGalleryInteractionListener
    {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery); // .gallery_activity);
    }

    @Override
    public void onGalleryClick(Bitmap image, String description) {
        //Create intent
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putExtra("title", description);
        intent.putExtra("image", image);

        //Start details activity
        startActivity(intent);
    }
}
