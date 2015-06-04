package de.k3b.android.fotoviewer;

/**
 * Created by k3b on 03.06.2015.
 */

import android.graphics.Bitmap;
import android.net.Uri;

import de.k3b.android.database.QueryParameterParcelable;

/**
 * This interface must be implemented by activities that contain this
 * fragment to allow an interaction in this fragment to be communicated
 * to the activity and potentially other fragments contained in that
 * activity.
 * <p/>
 * See the Android Training lesson <a href=
 * "http://developer.android.com/training/basics/fragments/communicating.html"
 * >Communicating with Other Fragments</a> for more information.
 */
public interface OnGalleryInteractionListener {
    /** GalleryFragment tells the Owning Activity that an Item in the FotoGallery was clicked */
    public void onGalleryClick(Bitmap image, Uri imageUri, String description, QueryParameterParcelable parentQuery);

    /** GalleryFragment tells the Owning Activity that querying data has finisched */
    void setResultCount(int count);
}

