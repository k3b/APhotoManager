/*
 * Copyright (c) 2017-2021 by k3b.
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
import android.util.Log;
import android.widget.Toast;

import java.io.Closeable;
import java.lang.ref.WeakReference;

import de.k3b.android.androFotoFinder.R;
import de.k3b.io.IProgessListener;
import de.k3b.zip.LibZipGlobal;

/**
 * Async task that displays ProgressDialog while running.
 * <p>
 * Created by k3b on 09.07.2017.
 */

abstract public class AsyncTaskWithProgressDialog<param> extends AsyncTask<param, String, Integer>
        implements Closeable, IProgessListener {
    private final int idResourceTitle;
    private WeakReference<Activity> activity;

    protected ProgressDialog dlg = null;

    public AsyncTaskWithProgressDialog(Activity activity, int idResourceTitle) {
        this.idResourceTitle = idResourceTitle;
        this.setActivity(activity);
    }

    protected void onProgressUpdate(String... values) {
        if (dlg != null) {
            if (!dlg.isShowing()) {
                dlg.show();
                setDialog(getActivity(), dlg);
            }
            dlg.setMessage(values[0]);
        }
    }

    private void setDialog(Activity activity, ProgressDialog dlg) {
        if (activity instanceof ActivityWithAutoCloseDialogs) {
            ((ActivityWithAutoCloseDialogs) activity).setAutoClose(null, dlg, null);
        }
    }

    @Override
    protected void onPostExecute(Integer itemCount) {
        super.onPostExecute(itemCount);
        Activity activity = getActivity();
        if (activity != null) {
            String message = activity.getString(R.string.image_success_update_format, itemCount);
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        }

        destroy();
    }

    @Override
    protected void onCancelled(Integer result) {
        super.onCancelled(result);
        destroy();
    }

    public void close() {
        destroy();
    }

    public void destroy() {
        setActivity(null);
    }

    /**
     * de.k3b.io.IProgessListener:
     * <p>
     * called every time when command makes some little progress in non gui thread.
     * return true to continue
     */
    @Override
    public boolean onProgress(int itemCount, int total, String message) {
        return publishProgress(itemCount, total, message);
    }

    public boolean publishProgress(int itemCount, int total, Object message) {
        final boolean cancelled = this.isCancelled();
        if (cancelled) {
            if (LibZipGlobal.debugEnabled) {
                Log.d(LibZipGlobal.LOG_TAG, this.getClass().getSimpleName() + " cancel pressed ");
            }
        }
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
        return !cancelled;
    }


    protected Activity getActivity() {
        if (activity == null) return null;
        return activity.get();
    }

    public AsyncTaskWithProgressDialog setActivity(Activity activity) {
        boolean isActive = isNotFinishedYet();

        Activity oldActivity = getActivity();
        if (isActive && (activity == oldActivity)) {
            // no change
            return this;
        }

        this.activity = (isActive && (activity != null)) ? new WeakReference<>(activity) : null;
        if ((dlg != null) && dlg.isShowing()) {
            dlg.dismiss();
            setDialog(activity, null);
        }
        dlg = null;

        if ((activity != null) && isActive) {
            dlg = new ProgressDialog(activity);
            dlg.setTitle(idResourceTitle);
        }
        return this;
    }

    public boolean isNotFinishedYet() {
        return (getStatus() != Status.FINISHED) && !isCancelled();
    }
}
