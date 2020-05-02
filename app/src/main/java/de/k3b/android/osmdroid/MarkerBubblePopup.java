/*
 * Copyright (c) 2016-2020 by k3b.
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
package de.k3b.android.osmdroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.osmdroid.api.IMapView;
import org.osmdroid.library.R;
import org.osmdroid.views.MapView;

import java.lang.ref.WeakReference;

import de.k3b.android.osmdroid.infowindow.IMarkerInfoData;

/**
 * Created by k3b on 12.09.2016.
 */
public class MarkerBubblePopup<Datatype extends IMarkerInfoData> extends MarkerBubblePopupBase<Datatype> {
    // to execute button click
    private WeakReference<Activity> owner;
    private final int m_id_bubble_title;
    private final int m_id_bubble_description;
    private final int m_id_bubble_subdescription;
    private final int m_id_bubble_image;
    private final int m_id_bubble_moreinfo;

    /** is called to populate the popupBubble */

    public MarkerBubblePopup(Activity owner) {
        this(owner, org.osmdroid.library.R.layout.bonuspack_bubble, R.id.bubble_title,
                R.id.bubble_description, R.id.bubble_subdescription,
                R.id.bubble_image, R.id.bubble_moreinfo);
    }

    public MarkerBubblePopup(Activity owner, int id_bubble_layout, int id_bubble_title, int id_bubble_description, int id_bubble_subdescription, int id_bubble_image, int id_bubble_moreinfo) {
        super(id_bubble_layout);
        this.owner = (owner == null) ? null :  new WeakReference(owner);
        m_id_bubble_title = id_bubble_title;
        m_id_bubble_description = id_bubble_description;
        m_id_bubble_subdescription = id_bubble_subdescription;
        m_id_bubble_image = id_bubble_image;
        m_id_bubble_moreinfo = id_bubble_moreinfo;
    }

    @Override
    protected void onCreate(MapView mapView, View popupView, Datatype data) {
        bindString("R.id.bubble_title", popupView, m_id_bubble_title, data.getTitle());
        bindString("R.id.bubble_description", popupView, m_id_bubble_description, data.getSnippet());
        bindString("R.id.bubble_subdescription", popupView, m_id_bubble_subdescription, data.getSubDescription());
        bindString("R.id.bubble_image", popupView, m_id_bubble_image, data.getImage());
        bindString("R.id.bubble_moreinfo", popupView, m_id_bubble_moreinfo, data.getLink());
    }

    protected void bindString(final String debugContext, View view, int resourceId, final Object value) {
        if ((resourceId != 0) && (value != null) && (value.toString().length() > 0)) {
            View childView = view.findViewById(resourceId);
            final String strValue = value.toString();
            if (childView != null) {
                childView.setVisibility(View.VISIBLE);
                if (childView instanceof Button) {
                    childView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MarkerBubblePopup.this.onBubbleButtonClick(debugContext, v, strValue);
                        }
                    });
                } else if (childView instanceof TextView) {
                    Spanned html = Html.fromHtml(strValue);
                    ((TextView) childView).setText(html);
                } else if (childView instanceof ImageButton) {
                    childView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MarkerBubblePopup.this.onBubbleButtonClick(debugContext, v, strValue);
                        }
                    });

                } else if (childView instanceof ImageView) {

                }

            } else {
                Log.w(IMapView.LOGTAG,this.getClass().getSimpleName() + ".bindString : resource "
                        + debugContext + " not found");
            }
        }
    }

    private void onBubbleButtonClick(String debugContext, View v, String value) {
        Activity _owner = (owner == null) ? null : owner.get();
        if (_owner != null) {
            _owner.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value)));
        }
    }
}
