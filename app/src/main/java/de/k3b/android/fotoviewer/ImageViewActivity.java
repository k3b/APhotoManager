package de.k3b.android.fotoviewer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class ImageViewActivity extends Activity {

    public static final String EXTRA_IMAGE = "image";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_view_activity);

        String title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
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
            case R.id.action_settings:
                openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);;
        startActivity(intent);
    }

    private Bitmap getBitmap(final Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(
                    this.getContentResolver(), uri);
        } catch (IOException e) {
            Log.e(Global.LOG_CONTEXT, "ImageViewer: cannot load bitmap for uri '" + uri + "':" + e.getMessage(), e);
        }

        return null;
    }
}
