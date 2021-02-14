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
public abstract class FileCache<T extends FileCacheItem> implements IFileCache<T> {
    protected final Map<String, T> items = new TreeMap<>();

    @Override
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

    @Override
    public T put(IFile file) {
        T result = null;

        String canonicalPath = (file != null) ? file.getCanonicalPath() : null;

        if (canonicalPath != null) {
            result = items.get(canonicalPath);
            if (result == null) {
                result = create(file);
                items.put(canonicalPath.toLowerCase(), result);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public T[] getChildDirs(T root) {
        if (root == null) return null;
        return (T[]) root.getChildDirs(this);
    }

    public void remove(IFile file) {
        remove(get(file));
    }

    public void remove(T item) {
        if (item != null) {
            List<String> deleted = new ArrayList<>();
            item.remove(deleted);
            for (String s : deleted) {
                if (s != null) {
                    items.remove(s.toLowerCase());
                }
            }
        }
    }

}
