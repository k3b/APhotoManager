package de.k3b.android.fotoviewer;

import android.app.Activity;

import de.k3b.android.database.QueryParameterParcelable;

/**
 * Created by EVE on 04.06.2015.
 */
public interface Queryable {
    /**
     * Initiates a database requery in the background
     */
    void requery(Activity context, QueryParameterParcelable parameters);
}
