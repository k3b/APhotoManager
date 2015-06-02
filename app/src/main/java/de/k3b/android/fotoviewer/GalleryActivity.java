package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class GalleryActivity extends Activity implements GalleryArrayFragment.OnGalleryInteractionListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery); // .gallery_activity);
    }

    @Override
    public void onGalleryClick(ImageItem item) {
        //Create intent
        Intent intent = new Intent(this, ImageViewActivity.class);
        intent.putExtra("title", item.getTitle());
        intent.putExtra("image", item.getImage());

        //Start details activity
        startActivity(intent);
    }
}
