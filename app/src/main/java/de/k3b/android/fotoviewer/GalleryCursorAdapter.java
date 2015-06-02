package de.k3b.android.fotoviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Created by k3b on 02.06.2015.
 */
public class GalleryCursorAdapter extends CursorAdapter {
    public int taskA = 0;

    public GalleryCursorAdapter(Context context, Cursor c) {
        super(context, c);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // TODO Auto-generated method stub
        int index = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        long id = cursor.getLong(index);

        Bundle idBundle = new Bundle();
        idBundle.putLong("id", id);

        Message msg = new Message();
        msg.setData(idBundle);

        ImageHandler imgHandler = new ImageHandler(context, (ImageView) view);
        imgHandler.sendMessage(msg);

        view.setTag(imgHandler);
        Log.w("task s", " count");
    }

    @SuppressLint({ "NewApi", "NewApi" })
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ImageView iView = new ImageView(context);
        iView.setLayoutParams(new GridView.LayoutParams(200, 200));
        taskA++;
        Log.w("task s", taskA+ " count");
        return iView;
    }
    static class ImageHandler extends Handler {

        private ImageView mView;
        private Context mContext;

        public ImageHandler(Context c, ImageView v) {
            mView = v;
            mContext = c;
        }

        @Override
        public void handleMessage(Message msg) {

            Bundle idBundle = msg.getData();

            Long id = idBundle.getLong("id");
            Bitmap image = MediaStore.Images.Thumbnails.getThumbnail(
                    mContext.getContentResolver(),
                    id,
                    MediaStore.Images.Thumbnails.MICRO_KIND,
                    new BitmapFactory.Options());

            mView.setImageBitmap(image);
        }
    }
}
