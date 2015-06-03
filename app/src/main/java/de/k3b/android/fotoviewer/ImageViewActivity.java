package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class ImageViewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view_activity);

        String title = getIntent().getStringExtra("title");
        Uri uri = getIntent().getData();

        Bitmap bitmap;
        if (uri != null) {
            bitmap = getBitmap(uri);
        } else {
            bitmap = getIntent().getParcelableExtra("image");
        }

        TextView titleTextView = (TextView) findViewById(R.id.title);
        titleTextView.setText(title);

        ImageView imageView = (ImageView) findViewById(R.id.image);
        imageView.setImageBitmap(bitmap);
    }

    private Bitmap getBitmap(final Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(
                    this.getContentResolver(), uri);
        } catch (IOException e) {
            Log.e("ImageViewer", "cannot load bitmap for uri '" + uri + "':" + e.getMessage(), e);
        }

        return null;
    }


}
