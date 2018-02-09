/*
 * Copyright (c) 2017-2018 by k3b.
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;

/**
 * #105: Management of app locking (aka Android "Screen"-pinning, "Kiosk Mode", "LockTask")
 * https://developer.android.com/about/versions/android-5.0.html#ScreenPinning.
 *
 * Encapsulates special handling for android-4.0-4.4; 5.0; 6.0ff
 *
 * Created by k3b on 28.12.2017.
 */
@TargetApi(Build.VERSION_CODES.M)
public class LockScreen {
    private static boolean OS_APPLOCK_ENABLED = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

    public static boolean onOptionsItemSelected(Activity parent, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cmd_app_pin:
                if (!isLocked(parent)) {
                    if (OS_APPLOCK_ENABLED) {
                        parent.startLockTask();
                        parent.invalidateOptionsMenu();
                    } else {
                        Global.locked = true;
                        SettingsActivity.global2Prefs(parent.getApplication());
                    }
                }
                return true;
            case R.id.cmd_app_unpin2:
                // only for old android (< 5.0). Else use app-pinning-end
                Global.locked = false;
                SettingsActivity.global2Prefs(parent.getApplication());
                parent.invalidateOptionsMenu();
                return true;
        }
        return false;
    }

    public static boolean isLocked(Context ctx) {
        if (OS_APPLOCK_ENABLED && (ctx != null)) {
            ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return activityManager.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
            }

            // deprecated
            return activityManager.isInLockTaskMode();
        }
        return Global.locked;
    }

    public static void fixMenu(Menu menu) {
        if ((menu != null) && OS_APPLOCK_ENABLED) {
            MenuItem unlock = menu.findItem(R.id.cmd_app_unpin2);
            if (unlock != null) menu.removeItem(R.id.cmd_app_unpin2);

            menu.removeItem(R.id.cmd_show_geo);
            menu.removeItem(R.id.cmd_gallery);
        }
    }
}
