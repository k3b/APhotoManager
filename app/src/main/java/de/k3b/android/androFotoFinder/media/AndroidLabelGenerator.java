/*
 * Copyright (c) 2019 by k3b.
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
package de.k3b.android.androFotoFinder.media;

import android.content.Context;

import de.k3b.android.androFotoFinder.R;
import de.k3b.media.MediaFormatter;
import de.k3b.media.PhotoPropertiesFormatter;

/** translates FieldID to text based on translatable android resources  */
public class AndroidLabelGenerator implements MediaFormatter.ILabelGenerator {
    private final Context context;
    private final String fieldPrefix;

    public AndroidLabelGenerator(final Context context, String fieldPrefix) {
        this.context = context;
        this.fieldPrefix = fieldPrefix;
    }

    /** translates FieldID to text based on translatable android resources  */
    @Override
    public CharSequence get(PhotoPropertiesFormatter.FieldID id) {
        switch (id) {
            case dateTimeTaken:
                return getString2(R.string.lbl_date);
            case title:
                return getString2(R.string.lbl_title);
            case description:
                return getString2(R.string.lbl_description);
            case latitude_longitude:
                return getString2(R.string.lbl_latitude_short) + "/" + context.getString(R.string.lbl_longitude_short) + " ";
            case rating:
                return getString2(R.string.lbl_rating);
            case visibility:
                return getString2(R.string.lbl_image_visibility);
            case tags:
                return getString2(R.string.lbl_tag);
            case find:
                return getString2(R.string.lbl_any);
            case lastModified:
                return getString2(R.string.sort_by_modification);
            case path:
                return getString2(R.string.filter_path_hint);
            case clasz:
                return "";
            //case sort:
            //    return getString2(R.string.sort_menu_title);
            default:
                throw new RuntimeException("undefined get(" + id + ")");
        }
    }

    protected final String getString2(int resId) {
        return getString2(context.getString(resId));
    }
    protected final String getString2(String str) {
        return fieldPrefix + str + " ";
    }
}
