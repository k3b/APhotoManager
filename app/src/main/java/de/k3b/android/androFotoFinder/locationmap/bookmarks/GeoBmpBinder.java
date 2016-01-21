/*
 * Copyright (c) 2015-2016 by k3b.
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

package de.k3b.android.androFotoFinder.locationmap.bookmarks;


import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;

import de.k3b.android.androFotoFinder.R;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoFormatter;
import de.k3b.util.IsoDateTimeParser;

/**
 * Connects a {@link GeoBmpDto} data element
 * with the corresponding gui elements.
 *
 * This implements convention over configuration.
 * gui elements have always the same R.id.
 *
 * Created by k3b on 26.03.2015.
 */
public class GeoBmpBinder {
    /** hack for adapter in listview where i cannot controll the class generated */
    public static void toGui(final View gui, GeoBmpDto item) {
        toGui(new IViewHolder() {
            @Override
            public View findViewById(int id) {
                return gui.findViewById(id);
            }
        }, item);
    }

    public static void toGui(IViewHolder gui, GeoBmpDto item) {
        TextView textView;

        textView = (TextView) gui.findViewById(R.id.name);
        if (textView != null) textView.setText(item.getName());

        textView = (TextView) gui.findViewById(R.id.description);
        if (textView != null) textView.setText(item.getDescription());

        textView = (TextView) gui.findViewById(R.id.link);
        if (textView != null) textView.setText(item.getLink());

        textView = (TextView) gui.findViewById(R.id.id);
        if (textView != null) textView.setText(item.getId());

        textView = (TextView) gui.findViewById(R.id.latitude);
        if (textView != null) textView.setText(GeoFormatter.formatLatLon(item.getLatitude()));

        textView = (TextView) gui.findViewById(R.id.longitude);
        if (textView != null) textView.setText(GeoFormatter.formatLatLon(item.getLongitude()));

        textView = (TextView) gui.findViewById(R.id.zoom);
        if (textView != null) textView.setText(GeoFormatter.formatZoom(item.getZoomMin()));

        // read only fields
        textView = (TextView) gui.findViewById(R.id.time);
        if (textView != null) textView.setText(GeoFormatter.formatDate(item.getTimeOfMeasurement()));

        textView = (TextView) gui.findViewById(R.id.summary);
        if (textView != null) textView.setText(item.getSummary());

        final ImageView thumbnail = (ImageView) gui.findViewById(R.id.thumbnail);
        if (thumbnail != null) thumbnail.setImageBitmap(item.getBitmap());
    }

    public static void fromGui(IViewHolder gui, GeoBmpDto currentItem) {
        EditText textView;

        // id and bitmap are not read from gui

        textView = (EditText) gui.findViewById(R.id.name);
        if (textView != null) currentItem.setName(getStringOrNull(textView));

        textView = (EditText) gui.findViewById(R.id.description);
        if (textView != null) currentItem.setDescription(getStringOrNull(textView));

        textView = (EditText) gui.findViewById(R.id.link);
        if (textView != null) currentItem.setLink(getStringOrNull(textView));

        textView = (EditText) gui.findViewById(R.id.latitude);
        if (textView != null) currentItem.setLatitude(getLatLon(textView));

        textView = (EditText) gui.findViewById(R.id.longitude);
        if (textView != null) currentItem.setLongitude(getLatLon(textView));

        textView = (EditText) gui.findViewById(R.id.time);
        if (textView != null) currentItem.setTimeOfMeasurement(IsoDateTimeParser.parse(textView.getText().toString()));

        textView = (EditText) gui.findViewById(R.id.zoom);
        if (textView != null) currentItem.setZoomMin(GeoFormatter.parseZoom (textView.getText().toString()));
    }

    private static String getStringOrNull(EditText edit) {
        String result = edit.getText().toString();
        if ((result != null) && (result.length() > 0)) return result;
        return null;
    }

    private static double getLatLon(EditText edit) {
        try {
            return GeoFormatter.parseLatOrLon(edit.getText().toString());
        } catch (ParseException e) {
        }

        return IGeoPointInfo.NO_LAT_LON;
    }
}
