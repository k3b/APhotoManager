/*
 * Copyright (c) 2019-2020 by k3b.
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.zip.LibZipGlobal;

public abstract class ProgressActivity<RESULT> extends FilePermissionActivity {
    private static String mDebugPrefix = "ProgressActivity: ";

    abstract protected ProgressableAsyncTask<RESULT> getAsyncTask();

    abstract protected void setAsyncTask(ProgressableAsyncTask<RESULT> asyncTask);

    @Override
    protected void onDestroy() {
        setAsyncTaskProgessReceiver(mDebugPrefix + "onDestroy ", null);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        setAsyncTaskProgessReceiver(mDebugPrefix + "onResume ", this);
        Global.debugMemory(mDebugPrefix, "onResume");
        super.onResume();

    }

    /**
     * (Re-)Connects this activity back with static asyncTask
     */
    protected void setAsyncTaskProgessReceiver(String why, Activity progressReceiver) {
        boolean isActive = ProgressableAsyncTask.isActive(getAsyncTask());
        boolean running = (progressReceiver != null) && isActive;

        String debugContext = why + mDebugPrefix + " setBackupAsyncTaskProgessReceiver isActive=" + isActive +
                ", running=" + running +
                " ";

        if (getAsyncTask() != null) {
            final ProgressBar progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
            final TextView status = (TextView) this.findViewById(R.id.lbl_status);
            final Button buttonCancel = (Button) this.findViewById(R.id.cmd_cancel);

            // setVisibility(running, progressBar, buttonCancel);

            if (running) {
                getAsyncTask().setContext(debugContext, this, progressBar, status);
                final String _debugContext = debugContext;
                buttonCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (LibZipGlobal.debugEnabled) {
                            Log.d(LibZipGlobal.LOG_TAG, mDebugPrefix + " button Cancel backup pressed initialized by " + _debugContext);
                        }
                        getAsyncTask().cancel(false);
                        buttonCancel.setVisibility(View.INVISIBLE);
                    }
                });

            } else {
                getAsyncTask().setContext(debugContext, null, null, null);
                buttonCancel.setOnClickListener(null);
                if (!isActive) {
                    setAsyncTask(null);
                }
            }
        }
    }
}
