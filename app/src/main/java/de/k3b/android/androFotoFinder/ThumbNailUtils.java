/*
 * Copyright (c) 2015-2017 by k3b.
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
import android.graphics.Bitmap;
import android.widget.ImageView;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;

import java.io.File;
import java.io.IOException;

import de.k3b.android.androFotoFinder.queries.FotoSql;

/**
 * Service facade hiding com.nostra13.universalimageloader
 * from the rest of the app.
 *
 * Created by k3b on 02.07.2016.
 */
public class ThumbNailUtils {
    public static final String LOG_TAG = "ImageLoader";
    public static final int MAX_CACHE_SIZE_50MB = 50 * 1024 * 1024;
    public static final int MAX_FILE_COUNT = 1024;
    public static boolean DEBUG = false;

    public static void init(Context context, File previousCacheRoot) {

        // if chache dir has just changed (in SettingsActivity) clear old cache.
        if ((previousCacheRoot != null) && (!previousCacheRoot.equals(Global.thumbCacheRoot))) {
            ImageLoader.getInstance().clearDiskCache();
        }

        // This configuration tuning is custom. You can tune every option, you may tune some of them,
        // or you can create default configuration by
        //  ImageLoaderConfiguration.createDefault(this);
        // method.
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY - 2);
        config.denyCacheImageMultipleSizesInMemory();
        final Md5FileNameGenerator fileNameGenerator = new Md5FileNameGenerator();
        config.diskCacheFileNameGenerator(fileNameGenerator);
        config.diskCacheSize(MAX_CACHE_SIZE_50MB); // 50 MiB
        config.diskCacheFileCount(MAX_FILE_COUNT);
        config.tasksProcessingOrder(QueueProcessingType.LIFO);

        // config.diskCache(new LimitedAgeDiskCache(Global.thumbCacheRoot, 60 * 60 * 24)); // lifetime 1 day
        // config.diskCache(new UnlimitedDiskCache(Global.thumbCacheRoot));

        try {
            // #83: limit size of cache. default factory has no parameter for cache-dir so do it manually.
            config.diskCache(new LruDiskCache(Global.thumbCacheRoot, null, fileNameGenerator, MAX_CACHE_SIZE_50MB, MAX_FILE_COUNT));
        } catch (IOException e) {
            // does not obey the config limits diskCacheSize/diskCacheFileCount
            config.diskCache(new UnlimitedDiskCache(Global.thumbCacheRoot));
        }

        // #83 this should make the cache-items smaller but it makes gallery scrolling much slower on my android-4.4.
        // config.diskCacheExtraOptions(Global.imageDetailThumbnailIfBiggerThan, Global.imageDetailThumbnailIfBiggerThan, null);


        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }

    private static DisplayImageOptions createThumbnailOptions() {
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
        ImageLoader.getInstance().displayImage( FotoSql.getUriString(iconID), imageView, mDisplayImageOptions);
    }

    public static void getThumb(String fullPath, ImageView imageView) {

        if ((imageView != null) && (fullPath != null) && (fullPath.length() > 0)) {
            ImageLoader.getInstance().displayImage("file://" + fullPath, imageView, mDisplayImageOptions);
        }
    }

}
