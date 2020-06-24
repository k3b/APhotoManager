package de.k3b.io.collections;

import java.io.File;
import java.util.Date;

import de.k3b.io.filefacade.IFile;

public interface ISelectedFiles {
    IFile[] getIFiles();

    @Deprecated
    File[] getFiles();

    int getNonEmptyNameCount();

    /** converts this into komma seperated list of names */
    String toPathListString();

    /** converts this into komma seperated list of names */
    String toIdString();

    /** converts this into komma seperated list of names */
    String toDateString();

    int size();

    File getFile(int i);

    IFile getIFile(int i);

    Long getId(int i);

    Long[] getIds();

    /** needed for AutoRenaming which is based on DatesPhotoTaken.
     * return null if unknwon */
    Date[] getDatesPhotoTaken();
}
