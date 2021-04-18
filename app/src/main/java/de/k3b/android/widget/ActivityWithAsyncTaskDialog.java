/*
 * Copyright (c) 2021 by k3b.
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

package de.k3b.android.widget;

import android.os.Bundle;

import java.lang.ref.WeakReference;

/**
 * {@link ActivityWithAsyncTaskDialog} is an Activity that shows a Progress-Dialog while
 * {@link AsyncTaskRunnerWithProgressDialog} runs a {@link ITaskRunner} in the Background
 */
public abstract class ActivityWithAsyncTaskDialog extends ActivityWithAutoCloseDialogs {
    // only one current AsyncTaskDialog can exist at a time.
    private static WeakReference<AsyncTaskRunnerWithProgressDialog> runner = null;

    private static void setActivityIfActive(ActivityWithAsyncTaskDialog activity) {
        AsyncTaskRunnerWithProgressDialog task = (runner == null) ? null : runner.get();
        if (task != null) {
            task.setActivity(activity);
        }
    }

    public static void setRunner(AsyncTaskRunnerWithProgressDialog runner) {
        ActivityWithAsyncTaskDialog.runner = (runner == null) ? null : new WeakReference<AsyncTaskRunnerWithProgressDialog>(runner);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // After rotation the dialog will be destroyed. Make shure that a new progressDialog is created.
        setActivityIfActive(this);
    }
}