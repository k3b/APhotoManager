package de.k3b.android.fotoviewer.queries;

/**
 * Data to be shared between activities and fragments
 *
 * Created by k3b on 16.06.2015.
 */
public class FotoViewerParameter {
    /** getFrom data for directory chooser */
    public static QueryParameterParcelable currentDirContentQuery = FotoSql.queryGroupByDir; //queryGroupByDate;

    /** getFrom orderBy for directory chooser */
    public static QueryParameterParcelable currentDirOrderByQuery = null;

    /** true: load images from folder and its subfolders. false: do not load images from subfolders */
    public static boolean includeSubItems = true;

    public static String currentDirContentValue = "/";

    /** getFrom data for gallery */
    public static QueryParameterParcelable currentGalleryContentQuery = FotoSql.queryDetail;

    /** true: gallery contains inline dir-picker */
    public static boolean galleryHasEmbeddedDirPicker = false;
}
