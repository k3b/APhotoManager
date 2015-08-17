/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

import android.util.Log;

/**
 * Global Settings
 *
 * Created by k3b on 04.06.2015.
 */
public class Global {
    public static final String LOG_CONTEXT = "k3bFoto";

    /**
     * TODO: create settings view for these
     */
    public static boolean debugEnabled = true;
    public static boolean debugEnabledViewItem = false;
    public static boolean debugEnabledSql = false;

    public static boolean clearSelectionAfterCommand = false;

    public static void debugMemory(String modul, String message) {
        if (false && Global.debugEnabled) {
            Runtime r = Runtime.getRuntime();
            String formattedMessage = String.format("memory : (total/free/avail) = (%3$dK/%4$dK/%5$dK)\t- %1$s.%2$s",
                    modul, message, r.totalMemory()/1024, r.freeMemory()/1024, r.maxMemory()/1024);

            Log.d(Global.LOG_CONTEXT, formattedMessage);
        }
    }


}
