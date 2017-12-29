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

import android.app.Activity;
import android.view.MenuItem;

/**
 * Created by k3b on 28.12.2017.
 */

public class LockScreen {
    public static boolean onOptionsItemSelected(Activity parent, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cmd_lock:
                Global.locked = true;
                SettingsActivity.global2Prefs(parent.getApplication());
                return true;
            case R.id.cmd_unlock:
                Global.locked = false;
                SettingsActivity.global2Prefs(parent.getApplication());
                return true;
        }
        return false;
    }
}
