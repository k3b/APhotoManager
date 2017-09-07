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
package de.k3b.android.androFotoFinder.ads;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by k3b on 07.09.2017.
 */

public class IptcView extends TextView {
    public IptcView(Context context) {
        super(context);
    }

    public IptcView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public IptcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public IptcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
