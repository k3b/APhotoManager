/*
 * Copyright (c) 2015-2016 by k3b.
 *
 * This file is part of AndroFotoFinder.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */

package de.k3b.android.androFotoFinder;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;

import java.io.File;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.androFotoFinder.queries.FotoThumbFile;

/**
 * Created by k3b on 02.07.2016.
 */
public class ThumbNailUtils {
    private static final String THUMBNAIL_DIR_NAME = ".thumbCache";
    private final SharedPreferences mPrefs;
    private File mThumbRoot;

    public static final String LOG_TAG = "ImageLoader";
    public static boolean DEBUG = false;

    public ThumbNailUtils(Context context) {
        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        String thumbRoot = mPrefs.getString("thumbCache", null);

        mThumbRoot = null;

        if (thumbRoot != null) {
            mThumbRoot = new File(thumbRoot);
        } else {
            mThumbRoot = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),THUMBNAIL_DIR_NAME);
            mThumbRoot.mkdirs();
        }
    }

    public static File getThumbRoot(Context context) {
        return new ThumbNailUtils(context).getThumbRoot();
    }

    public File getThumbRoot() {
        return mThumbRoot;
    }

    public ThumbNailUtils setThumbRoot(File newValue) {
        mThumbRoot = newValue;

        mPrefs.edit()
                .putString("thumbCache", (newValue != null) ? newValue.getAbsolutePath() : null)
                .commit();
        return this;
    }

    public static File getTumbDir(String fullPath, int maxDepth) {
        File candidateParent  = (fullPath != null) ? new File(fullPath) : null;
        while ((candidateParent != null) && (maxDepth >= 0)) {
            File thumbsDir = new File(candidateParent, THUMBNAIL_DIR_NAME);
            if (thumbsDir.exists()) return thumbsDir;
            maxDepth--;
            candidateParent = candidateParent.getParentFile();
        }
        return null;
    }

    public static void init(Context context) {
        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        config.diskCache(new UnlimitedDiskCache(getThumbRoot(context)));
        final Md5FileNameGenerator fileNameGenerator = new Md5FileNameGenerator();
        config.diskCacheFileNameGenerator(fileNameGenerator);
        config.diskCacheSize(50 * 1024 * 1024); // 50 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }

    public static DisplayImageOptions createThumbnailOptions() {
        return new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.image_loading)
                .showImageForEmptyUri(R.drawable.image_loading)
                .showImageOnFail(R.drawable.image_loading)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .displayer(new SimpleBitmapDisplayer())
                .build();
    }

    private static final DisplayImageOptions mDisplayImageOptions = ThumbNailUtils.createThumbnailOptions();

    public static void getThumb(int iconID, ImageView imageView) {
        ImageLoader.getInstance().displayImage( FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI + "/" + iconID, imageView, mDisplayImageOptions);
    }

    public static void getThumb(String fullPath, ImageView imageView) {
        ImageLoader.getInstance().displayImage( "file://" + fullPath, imageView, mDisplayImageOptions);
    }

}
