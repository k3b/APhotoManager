/*
 * Copyright (c) 2018 by k3b.
 *
 * This file is part of AndroFotoFinder and of ToGoZip.
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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/**
 * An activity that memorizes the activity call stack for debugging purposes.
 *
 * Created by k3b on 25.08.2018.
 */

public class ActivityWithCallContext extends Activity {
    private static final String PARAM_CALLSTACK = "callstack";

    /**
     * for debugging the activity call sequence
     */
    private String parentCallContext = "";

    public static String additionalCallContext = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.parentCallContext = savedInstanceState.getString(PARAM_CALLSTACK, parentCallContext);
        } else if (getIntent() != null) {
            this.parentCallContext = getIntent().getStringExtra(PARAM_CALLSTACK);
            if (this.parentCallContext == null) this.parentCallContext = "";
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            savedInstanceState.putString(PARAM_CALLSTACK, parentCallContext);
        }
    }

    public String getCallContext() {
        Activity owner = this;
        return parentCallContext + " => " + getCallerDescription(owner);
    }

    private static String getCallerDescription(Activity owner) {
        return owner.getClass().getSimpleName() +
                "{" + owner.getTitle() + "}";
    }

    protected static void addContext(String debugContext, Intent targetIntent, Activity context) {
        if ((targetIntent != null) && (context != null)) {
            CharSequence caller = (context instanceof LocalizedActivity)
                    ? ((LocalizedActivity) context).getCallContext()
                    : getCallerDescription(context);
            targetIntent.putExtra(PARAM_CALLSTACK, caller + debugContext);
        }
    }

    /** called by all variants of startActivity(ForResult): add context to call.*/
    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        startActivityForResultImpl(intent, requestCode, options);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        startActivityForResultImpl(intent, requestCode, null);
    }

    @Override
    public void startActivity(Intent intent, Bundle options) {
        startActivityForResultImpl(intent, -1, options);
    }

    @Override
    public void startActivity(Intent intent) {
        startActivityForResultImpl(intent, -1, null);
    }

    /** called by all variants of startActivity(ForResult): add context to call.*/
    private void startActivityForResultImpl(Intent intent, int requestCode, Bundle options) {
        addContext(additionalCallContext, intent, this);
        additionalCallContext = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            super.startActivityForResult(intent, requestCode, options);
        } else {
            super.startActivityForResult(intent, requestCode);
        }
    }
}