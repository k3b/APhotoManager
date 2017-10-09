/*
 * Copyright (c) 2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
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

package de.k3b.io.collections;

import java.util.Iterator;

/**
 * Everything necessary to process one or more media files:
 * iterator over {@link IMediaFile} with id, source and destination.
 *
 * Created by k3b on 14.10.2017.
 */

public class MediaFileIterator implements Iterator<IMediaFile>, IMediaFile {
    private final Iterator<IMediaFileSource> sourceIterator;
    private final IPathProcessor source2DestPath;

    private IMediaFileSource current = null;

    /**
     *
     * @param sourceIterator  where id and sourcepath comes from.
     * @param source2DestPath translates sourcepath to destpath. Null means source=dest
     */
    public MediaFileIterator(Iterator<IMediaFileSource> sourceIterator, IPathProcessor source2DestPath) {

        this.sourceIterator = sourceIterator;
        this.source2DestPath = source2DestPath;
    }

    /**
     * Databse key of jpg. 0 == unknown
     */
    @Override
    public long getID() {
        return current.getID();
    }

    /**
     * Normalized absolute path to jpg file
     */
    @Override
    public String getFullJpgSourcePath() {
        return current.getFullJpgSourcePath();
    }

    @Override
    public boolean hasNext() {
        return sourceIterator.hasNext();
    }

    @Override
    public IMediaFile next() {
        current = sourceIterator.next();
        return this;
    }

    @Override
    public void remove() {
        sourceIterator.remove();
    }

    /** Normalized absolute path to jpg file */
    @Override
    public String getFullJpgDestinationPath() {
        String fullJpgSourcePath = current.getFullJpgSourcePath();
        if (source2DestPath != null) {
            return source2DestPath.getFullJpgDestinationPath(fullJpgSourcePath);
        }
        return fullJpgSourcePath;
    }
}
