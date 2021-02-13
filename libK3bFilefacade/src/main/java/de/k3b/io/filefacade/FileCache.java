/*
 * Copyright (c) 2021 by k3b.
 *
 * This file is part of #APhotoManager (https://github.com/k3b/APhotoManager/)
 *              and #toGoZip (https://github.com/k3b/ToGoZip/).
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
import java.util.Map;
import java.util.TreeMap;

import de.k3b.io.FileUtilsBase;

/**
 * Cache for {@link FileCacheItem}s that holds {@link IFile}.
 * <p>
 * Example use
 * <p>
 * public static class FileCacheImpl extends FileCache<FileCache.FileCacheItem> {
 *
 * @param <T>
 * @Override protected FileCacheItem create(IFile file) {
 * return new FileCacheItem(file);
 * }
 * @Override protected FileCacheItem[] toArray(List<FileCacheItem> resultList) {
 * return resultList.toArray(new FileCacheItem[resultList.size()]);
 * }
 * }
 */
public abstract class FileCache<T extends FileCache.FileCacheItem> {
    protected final Map<String, T> items = new TreeMap<>();

    abstract protected T create(IFile file);

    abstract protected T[] toArray(List<T> resultList);

    public int size() {
        return items.size();
    }

    public T get(IFile fullPath) {
        T result = null;
        if (fullPath != null) {
            result = get(fullPath.getCanonicalPath());
        }
        return result;
    }

    public T get(String fullPath) {
        T result = null;
        if (fullPath != null) {
            result = items.get(fullPath.toLowerCase());
        }
        return result;
    }

    public T put(IFile file) {
        String canonicalPath = (file != null) ? file.getCanonicalPath() : null;
        T result = null;

        if (canonicalPath != null) {
            result = items.get(canonicalPath);
            if (result == null) {
                result = create(file);
                items.put(canonicalPath.toLowerCase(), result);
            }
        }
        return result;
    }

    public T[] getChildDirs(T root) {
        T[] result = null;
        if (root != null) {
            result = (T[]) root.childDirs;
            if (result == null) {
                boolean containsNomedia = false;
                IFile[] iFiles = root.item.listFiles();
                List<T> resultList = new ArrayList<>(iFiles.length);
                for (IFile i : iFiles) {
                    String path = i.getCanonicalPath();
                    if (path != null && path.toLowerCase().contains(FileUtilsBase.MEDIA_IGNORE_FILENAME)) {
                        containsNomedia = true;
                    }
                    if (i.isDirectory()) {
                        T item = this.put(i);
                        item.parent = root;
                        resultList.add(item);
                    }
                }
                root.isNomedia = containsNomedia;
                result = toArray(resultList);
                root.childDirs = result;
            }

        }
        return result;
    }

    public void remove(IFile file) {
        remove(get(file));
    }

    public void remove(T item) {
        if (item != null) {
            if (item.childDirs != null) {
                for (T i : (T[]) item.childDirs) {
                    remove(i);
                }
                item.childDirs = null;
            }
            if (item.parent != null) {
                // parent-children must be recalculated
                item.parent.childDirs = null;
                item.parent = null;
            }
            items.remove(item.item.getCanonicalPath().toLowerCase());
        }
    }

    protected static class FileCacheItem<T extends FileCache.FileCacheItem> implements Comparable<T> {
        final IFile item;
        T[] childDirs;
        T parent;
        Boolean isNomedia;
        Boolean isPrivate;

        FileCacheItem(IFile item) {
            this.item = item;
        }

        public IFile getItem() {
            return item;
        }

        public T[] getChildDirs() {
            return childDirs;
        }

        public T getParent() {
            return parent;
        }

        public Boolean getNomedia() {
            return isNomedia;
        }

        public Boolean getPrivate() {
            return isPrivate;
        }

        @Override
        public int compareTo(T fileCache) {
            String otherPath = (fileCache == null) ? null : fileCache.getItem().getCanonicalPath();
            String thisPath = getItem().getCanonicalPath();
            if (thisPath == null) {
                if (otherPath == null) return 0;
                return 1;
            }
            return thisPath.compareTo(otherPath);
        }
    }
}
