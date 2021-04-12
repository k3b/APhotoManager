/*
 * Copyright (c) 2018-2020 by k3b.
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
package de.k3b.android.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import de.k3b.android.androFotoFinder.R;
import de.k3b.io.StringUtils;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

/**
 * Encapsulates Clipboard IO.
 * Created by k3b on 05.03.2018.
 */

public class ClipboardUtil {
    public static boolean addDirToClipboard(Context context, CharSequence dir, boolean withToast) {
        if (!StringUtils.isNullOrEmpty(dir)) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(context.getString(R.string.lbl_path), dir);
                clipboard.setPrimaryClip(clip);
                if (withToast) {
                    Toast.makeText(context, dir, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        }
        return false;
    }

    public static IFile getClipboardDir(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData data = (clipboard != null) ? clipboard.getPrimaryClip() : null;
        if (data != null) {
            for (int i = data.getItemCount() - 1; i >= 0; i--) {
                CharSequence cipText = data.getItemAt(i).getText();
                if (!StringUtils.isNullOrEmpty(cipText)) {
                    IFile result = FileFacade.convert(
                            "getClipboardDir", cipText.toString());
                    if (isValid(result)) return result;
                }
            }
        }
        return null;
    }

    protected static boolean isValid(IFile dir) {
        return ((dir != null) && dir.exists() && dir.isDirectory());
    }
}
