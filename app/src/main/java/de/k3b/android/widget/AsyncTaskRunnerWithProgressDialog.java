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

import android.content.Context;

import de.k3b.io.IProgessListener;

/**
 * {@link ActivityWithAsyncTaskDialog} is an Activity that shows a Progress-Dialog while
 * {@link AsyncTaskRunnerWithProgressDialog} runs a {@link ITaskRunner} in the Background
 */

public class AsyncTaskRunnerWithProgressDialog extends AsyncTaskWithProgressDialog<ITaskRunner> implements IProgessListener {
    private final Context context;

    public AsyncTaskRunnerWithProgressDialog(ActivityWithAsyncTaskDialog activity, int idResourceTitle) {
        super(activity, idResourceTitle);
        context = activity.getApplicationContext();
    }

    @Override
    protected Integer doInBackground(ITaskRunner... executers) {
        int result = 0;
        for (ITaskRunner executer : executers) {
            ActivityWithAsyncTaskDialog.setRunner(this);
            result += executer.run(context, this);
        }
        return result;
    }

    @Override
    public void destroy() {
        setActivity(null);
        super.destroy();
    }

}
