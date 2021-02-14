/*
 * Copyright (c) 2018-2021 by k3b.
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

import de.k3b.io.filefacade.IFile;

/**
 * Also add *.album files to sub-directories.
 *
 * Created by k3b on 17.04.2018.
 */

public class OSDirOrVirtualAlbumFile extends OSDirectory {
    public OSDirOrVirtualAlbumFile(IFile current, OSDirectory parent, OSDirectory[] childen) {
        super(current, parent, childen);
        if (isAlbum(current)) {
            setDirFlags(DIR_FLAG_VIRTUAL_DIR);
        }
    }

    private boolean isAlbum(IFile candidate) {
        return (candidate != null) && AlbumFile.isQueryFile(candidate.getName());
    }

    @Override
    protected int getCalculateFlags(IFile directory) {
        int result;
        if (isAlbum(directory)) {
            result = DIR_FLAG_VIRTUAL_DIR;
        } else {
            result = super.getCalculateFlags(directory);
        }
        return result;
    }

    @Override
    protected boolean isDirectory(IFile file) {
        if (super.isDirectory(file)) return true;
        return isAlbum(file);
    }

    /**
     * factory method to be overwrittern by derived classes, if tree should consist of derived classes.
     */
    @Override
    public OSDirectory createOsDirectory(IFile file, OSDirectory parent, OSDirectory[] children) {
        return new OSDirOrVirtualAlbumFile(file, parent, children);
    }
}