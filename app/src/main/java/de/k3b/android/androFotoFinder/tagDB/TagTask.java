/*
 * Copyright (c) 2017 by k3b.
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
package de.k3b.android.androFotoFinder.tagDB;

import android.app.Activity;

import de.k3b.android.widget.AsyncTaskWithProgressDialog;

/**
 * An AsyncTask that handles {@link TagWorflow} with a {@link android.app.ProgressDialog}.
 *
 * Created by k3b on 30.01.2017.
 */

public abstract class TagTask<param> extends AsyncTaskWithProgressDialog<param> {
    private TagWorflow workflow;

    public TagTask(Activity parent, int idResourceTitle) {
        super(parent,idResourceTitle);
        this.workflow = new TagWorflow() {
            @Override
            public boolean onProgress(int itemCount, int total, String message) {
                TagTask.this.publishProgress(itemCount, total, message);
                return true;
            }

        };
    }

    @Override
    public void destroy() {
        workflow = null;
        super.destroy();
    }

    public TagWorflow getWorkflow() {
        return workflow;
    }
}
