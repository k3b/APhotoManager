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
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.List;

import de.k3b.android.androFotoFinder.R;

/**
 * An {@link AsyncTask} that handles {@link TagWorflow} a {@link android.app.ProgressDialog}.
 *
 * Created by k3b on 30.01.2017.
 */

public abstract class TagTask<param> extends AsyncTask<param, String, Integer> {
    private Activity parent;
    private ProgressDialog dlg = null;
    private TagWorflow workflow;

    public TagTask(Activity parent, int idResourceTitle) {
        this.parent = parent;
        this.workflow = new TagWorflow() {
            @Override
            protected void onProgress(int itemCount, int total, String message) {
                StringBuilder msg = new StringBuilder();
                if (itemCount > 0) {
                    msg.append("(").append(itemCount);
                    if (total > 0) {
                        msg.append("/").append(total);
                    }
                    msg.append(") ");
                }
                msg.append(message);
                publishProgress(msg.toString());
            }

        };
        dlg = new ProgressDialog(parent);
        dlg.setTitle(idResourceTitle);
    }

    protected void onProgressUpdate(String... values) {
        if (dlg != null) {
            if (!dlg.isShowing()) dlg.show();
            dlg.setMessage(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Integer itemCount) {
        super.onPostExecute(itemCount);
        String message = parent.getString(R.string.tags_update_result_format, itemCount);
        Toast.makeText(parent, message, Toast.LENGTH_LONG).show();

        destroy();
    }

    @Override
    protected void onCancelled(Integer result) {
        super.onCancelled(result);
        destroy();
    }

    public void destroy() {
        workflow = null;
        if ((dlg != null) && dlg.isShowing()) {
            dlg.dismiss();
        }
        dlg = null;
        parent = null;
    }

    public TagWorflow getWorkflow() {
        return workflow;
    }
}
