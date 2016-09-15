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
public class MarkerInfoData implements IMarkerInfoData {
    protected String mTitle, mSnippet, mSubDescription;

    /** uri to be opend when clicking on the link buttotn */
    protected String mLink;
    private Drawable mIcon;

    public void setImage(Drawable icon){mIcon = icon;}

    @Override
    public Drawable getImage(){return mIcon;}

    public void setTitle(String title){
         mTitle = title;
     }

    @Override
     public String getTitle(){
         return mTitle;
     }

     public void setSnippet(String snippet){
         mSnippet= snippet;
     }

     @Override
     public String getSnippet(){
         return mSnippet;
     }

     /** set the "sub-description", an optional text to be shown in the InfoWindow, below the snippet, in a smaller text size */
     public void setSubDescription(String subDescription){
         mSubDescription = subDescription;
     }

     @Override
     public String getSubDescription(){
         return mSubDescription;
     }

     public void setLink(String link){
         mLink = link;
     }

     @Override
     public String getLink(){
         return mLink;
     }

 }
