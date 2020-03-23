/*
 * Copyright (c) 2020 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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

import android.os.Bundle;

import de.k3b.android.widget.ActivityWithAutoCloseDialogs;
import de.k3b.android.widget.ActivityWithCallContext;
import de.k3b.android.widget.BaseQueryActivity;
import de.k3b.android.widget.FilePermissionActivity;
import de.k3b.android.widget.LocalizedActivity;

/**
 * Nearly all activities are inherited from {@link BaseActivity}.
 * * {@link FilePermissionActivity} manage permission write to external-storage and to sdcard
 * * {@link ActivityWithAutoCloseDialogs} Automatically closes pop-dialogs
 * * {@link LocalizedActivity} Change the locale (language) and translation of its content at runtime
 * * {@link ActivityWithCallContext} memorizes the activity call stack (parent Activities) for debugging purposes.
 * <p>
 * * {@link BaseQueryActivity} filtered collection of photos used by
 * ** {@link de.k3b.android.androFotoFinder.FotoGalleryActivity}
 * ** {@link de.k3b.android.androFotoFinder.locationmap.MapGeoPickerActivity}
 */
public abstract class BaseActivity extends FilePermissionActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Global.debugMemory(Global.LOG_CONTEXT, "onCreate " + getClass().getSimpleName());
        super.onCreate(savedInstanceState);
    }
}
