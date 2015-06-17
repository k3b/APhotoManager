package de.k3b.android.fotoviewer.queries;

import android.app.Activity;

/**
 * An Api that clients to getFrom the content of of it-s fragment(s) and/or adapter(s).
 *
 * Created by k3b on 04.06.2015.
 */
public interface Queryable {
    /**
     * interface Queryable: Initiates a database requery in the background
     */
    void requery(Activity context, QueryParameterParcelable parameters);
}
