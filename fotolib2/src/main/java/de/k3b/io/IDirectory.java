package de.k3b.io;

import java.util.List;

/**
 * Abstraction of Directory
 *
 * Created by k3b on 04.08.2015.
 */
public interface IDirectory {
    String getRelPath();
    String getAbsolute();

    IDirectory getParent();

    List<IDirectory> getChildren();

    IDirectory find(String path);

    void destroy();

    int getIconID();

}
