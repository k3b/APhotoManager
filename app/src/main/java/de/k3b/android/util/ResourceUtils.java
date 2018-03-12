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
package de.k3b.android.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/**
 * Created by k3b on 08.06.2017.
 */

public class ResourceUtils {
    public static Drawable getDrawable(Context context, int resID) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getDrawable(resID);
        }
        return context.getResources().getDrawable(resID);
    }

    public static void setFocusWithKeyboard(final EditText textView) {
        textView.requestFocus();
        textView.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) textView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(textView, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    public interface IFilter {

        boolean match(View t);
    }

    // owner.getRootView()
    public static View findLast(View owner, IFilter filter) {
        if (owner == null) return null;
        if (filter.match(owner)) return owner;
        if (owner instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) owner;
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                View child = findLast(parent.getChildAt(i), filter);
                if (child != null) return child;
            }
        }
        return null;
    }

    public static View findLast(View owner, final String id) {
        final String search = "." + id + ".";
        return findLast(owner, new IFilter() {
            @Override
            public boolean match(View t) {
                return t.getClass().getName().contains(search);
            }
        });
    }
}
