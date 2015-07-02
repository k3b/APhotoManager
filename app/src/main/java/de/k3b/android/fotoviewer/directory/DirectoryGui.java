package de.k3b.android.fotoviewer.directory;

import de.k3b.io.Directory;

/**
 * Created by k3b on 02.07.2015.
 */
public interface DirectoryGui {
    /** Defines Directory Navigation */
    void defineDirectoryNavigation(Directory root, int dirTypId, String initialAbsolutePath);

    /** Set curent selection to absolutePath */
    void navigateTo(String absolutePath);
}
