/*
 * Copyright (c) 2018 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager
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

package de.k3b.io;

import java.io.File;
import java.util.List;

/**
 * Also add *.album files to sub-directories.
 *
 * Created by k3b on 17.04.2018.
 */

public class OSDirOrVirtualAlbumFile extends OSDirectory {
    public OSDirOrVirtualAlbumFile(File current, OSDirectory parent, List<IDirectory> childen) {
        super(current, parent, childen);
        if (isAlbum(current)) {
            setDirFlags(DIR_FLAG_VIRTUAL_DIR);
        }
    }

    private boolean isAlbum(File candidate) {
        return (candidate != null) && AlbumFile.isQueryFile(candidate.getName());
    }

    @Override
    protected int getCalculateFlags(File directory) {
        int result;
        if (isAlbum(directory)) {
            result = DIR_FLAG_VIRTUAL_DIR;
        } else {
            result = super.getCalculateFlags(directory);
        }
        return result;
    }

    @Override
    protected boolean isDirectory(File candidate) {
        if (super.isDirectory(candidate)) return true;
        return isAlbum(candidate);
    }

    /** factory method to be overwrittern by derived classes, if tree should consist of derived classes. */
    @Override
    public OSDirectory createOsDirectory(File file, IDirectory parent, List<IDirectory> children) {
        return new OSDirOrVirtualAlbumFile(file, (OSDirectory) parent, children);
    }
}