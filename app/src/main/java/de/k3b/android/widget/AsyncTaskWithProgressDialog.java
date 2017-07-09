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
package de.k3b.android.widget;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.Closeable;
import java.io.IOException;

import de.k3b.android.androFotoFinder.R;

/**
 * Async task that displays ProgressDialog while running.
 *
 * Created by k3b on 09.07.2017.
 */

abstract public class AsyncTaskWithProgressDialog<param> extends AsyncTask<param, String, Integer>
                    implements Closeable{
    protected Activity parent;
    protected ProgressDialog dlg = null;
    public AsyncTaskWithProgressDialog(Activity parent, int idResourceTitle) {
        this.parent = parent;
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
        String message = parent.getString(R.string.image_success_update_format, itemCount);
        Toast.makeText(parent, message, Toast.LENGTH_LONG).show();

        destroy();
    }

    @Override
    protected void onCancelled(Integer result) {
        super.onCancelled(result);
        destroy();
    }

    public void close() throws IOException {
        destroy();
    }
    public void destroy() {
        if ((dlg != null) && dlg.isShowing()) {
            dlg.dismiss();
        }
        dlg = null;
        parent = null;
    }

    /** periodically called while work in progress.  */
    protected void publishProgress(int itemCount, int total, Object message) {
        StringBuilder msg = new StringBuilder();
        if (itemCount > 0) {
            msg.append("(").append(itemCount);
            if (total > 0) {
                msg.append("/").append(total);
            }
            msg.append(") ");
        }
        if (message != null) {
            msg.append(message);
        }
        publishProgress(msg.toString());
    }

}
