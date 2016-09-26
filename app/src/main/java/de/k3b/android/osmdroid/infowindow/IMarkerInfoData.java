package de.k3b.android.osmdroid.infowindow;

import android.graphics.drawable.Drawable;

import org.osmdroid.views.overlay.OverlayWithIW;

/**
 * {@link IMarkerInfoData} implemented by
 * {@link MarkerInfoData}
 * contain Data that can be
 * displayed in an {@link de.k3b.android.osmdroid.MarkerBubblePopup} (Bubble).
 *
 * <img alt="Class diagram around Marker class and InfoWindow" width="686" height="413" src='https://github.com/osmdroid/osmdroid/tree/master/osmdroid-android/src/main/doc/marker-infowindow-classes.png' />
 *
 * Extracted from {@link OverlayWithIW} .
 */
public interface IMarkerInfoData {
	String getTitle();

	String getSnippet();

	String getSubDescription();

	String getLink();

	Drawable getImage();
}
