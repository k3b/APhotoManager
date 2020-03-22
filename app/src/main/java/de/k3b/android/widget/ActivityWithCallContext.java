/*
 * Copyright (c) 2018-2020 by k3b.
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
import android.util.Log;

import de.k3b.android.androFotoFinder.Global;

/**
 * An activity that memorizes the activity call stack (parent Activities) for debugging purposes.
 * <p>
 * Created by k3b on 25.08.2018.
 */
public abstract class ActivityWithCallContext extends Activity {
    /**
     * the CallContext is an intent-extra with this name
     */
    private static final String PARAM_CALLSTACK = "callstack";

    /**
     * set to false to disable CallContext for privacy reasons
     */
    public static final Boolean isCallContextEnabled = true;

    /**
     * what the current activity is doing. This will become part of the callstack when a child activity is invoked
     */
    public static String additionalCallContext = "";

    /**
     * if there is a crash this can become part of the logcat
     */
    public static String lastKnownCallContext = "";

    /**
     * the CallContext of the parent-activity that called this activity
     */
    private String parentCallContext = "";

    public static void addContext(String debugContext, Intent targetIntent, Activity context) {
        if ((isCallContextEnabled) && (targetIntent != null) && (context != null)) {
            CharSequence caller = (context instanceof ActivityWithCallContext)
                    ? ((ActivityWithCallContext) context).getCallContext()
                    : getCallerDescription(context);
            if ((debugContext != null) && (debugContext.length() > 0)) {
                targetIntent.putExtra(PARAM_CALLSTACK, caller + "\n\t[" + debugContext + "]");
            } else {
                targetIntent.putExtra(PARAM_CALLSTACK, caller);
            }
            if (Global.debugEnabled) {
                Log.d(Global.LOG_CONTEXT, caller + ":" + targetIntent.toUri(Intent.URI_INTENT_SCHEME));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isCallContextEnabled) {
            Intent intent = getIntent();
            this.parentCallContext = readCallContext(intent);
            lastKnownCallContext = getCallContext();
        }
    }

    public static String readCallContext(Intent intent) {
        String parentCallContext = (intent != null)
                ? intent.getStringExtra(PARAM_CALLSTACK) : null;
        if (parentCallContext == null) parentCallContext = "";
        return parentCallContext;

    }

    public boolean hasParentCallContext() {
        return this.parentCallContext.length() > 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCallContextEnabled) {
            lastKnownCallContext = getCallContext();
        }
    }

    private static String getCallerDescription(Activity owner) {
        return owner.getClass().getSimpleName() +
                "{" + owner.getTitle() + "}";
    }

    public String getCallContext() {
        return parentCallContext + "\n=> " + getCallerDescription(this);
    }

    /**
     * called by all variants of startActivity(ForResult): add context to call.
     */
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

    /**
     * called by all variants of startActivity(ForResult): add context to call.
     */
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