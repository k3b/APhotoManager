/*
 * Copyright (c) 2021 by k3b.
 *
 * This file is part of AndroFotoFinder.
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

package de.k3b.io.filefacade;

import java.util.ArrayList;
import java.util.List;

import de.k3b.io.FileUtilsBase;

public class FileCacheItem<T extends FileCacheItem> implements Comparable<T> {
    protected IFile current;
    private T[] childDirs;
    private T parent;
    private Boolean isNomedia;
    private Boolean isPrivate;

    public FileCacheItem(IFile current) {
        this.current = current;
    }

    public void remove(List<String> removedPaths) {
        if (removedPaths != null && getCurrent() != null) {
            removedPaths.add(getCurrent().getCanonicalPath());
        }
        if (getChildDirs(null) != null) {
            for (T i : (T[]) getChildDirs(null)) {
                i.remove(removedPaths);
            }
            setChildDirs(null);
        }
        setParent(null);
        setCurrent(null);
    }

    public IFile getCurrent() {
        return current;
    }

    @SuppressWarnings("unchecked")
    public T setCurrent(IFile current) {
        this.current = current;
        return (T) this;
    }

    /**
     * will load on demand if param cache is not null
     */
    public T[] getChildDirs(IFileCache<T> cache) {
        if (childDirs == null && current != null && cache != null) {
            IFile[] iFiles = current.listDirs();
            if (iFiles != null) {
                List<T> resultList = new ArrayList<>(iFiles.length);
                boolean containsNomedia = false;
                for (IFile i : iFiles) {
                    String path = i.getCanonicalPath();
                    if (path != null && path.toLowerCase().contains(FileUtilsBase.MEDIA_IGNORE_FILENAME)) {
                        containsNomedia = true;
                    }
                    if (isDirectory(i)) {
                        T item = cache.put(i);
                        item.setParent(this);
                        resultList.add(item);
                    }
                }
                childDirs = resultList.toArray(cache.create(resultList.size()));
                setNomedia(containsNomedia);
            }
        } // if must load on demand

        return childDirs;
    }

    protected boolean isDirectory(IFile file) {
        return file != null
                && file.isDirectory()
                && !file.getName().startsWith(".")
                && !FileUtilsBase.isSymlinkDir(file.getFile(), true);
    }

    public T getParent() {
        return parent;
    }

    public void setParent(T parent) {
        this.parent = parent;
    }

    public Boolean getNomedia() {
        return isNomedia;
    }

    public void setNomedia(Boolean nomedia) {
        isNomedia = nomedia;
    }

    public Boolean getPrivate() {
        return isPrivate;
    }

    public void setPrivate(Boolean aPrivate) {
        isPrivate = aPrivate;
    }

    @Override
    public int compareTo(T fileCache) {
        String otherPath = (fileCache == null) ? null : fileCache.getCurrent().getCanonicalPath();
        String thisPath = getCurrent().getCanonicalPath();
        if (thisPath == null) {
            if (otherPath == null) return 0;
            return 1;
        }
        return thisPath.compareTo(otherPath);
    }

    public void setChildDirs(T[] childDirs) {
        this.childDirs = childDirs;
    }
}
