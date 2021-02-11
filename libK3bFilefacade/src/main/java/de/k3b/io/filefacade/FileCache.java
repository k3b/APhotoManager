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

public class FileCache {
    protected final Map<String, FileCacheItem> items = new TreeMap<>();

    public int size() {
        return items.size();
    }

    public FileCacheItem get(IFile fullPath) {
        FileCacheItem result = null;
        if (fullPath != null) {
            result = get(fullPath.getCanonicalPath());
        }
        return result;
    }

    public FileCacheItem get(String fullPath) {
        FileCacheItem result = null;
        if (fullPath != null) {
            result = items.get(fullPath.toLowerCase());
        }
        return result;
    }

    public FileCacheItem put(IFile file) {
        String canonicalPath = (file != null) ? file.getCanonicalPath() : null;
         FileCacheItem result = null;

        if (canonicalPath != null) {
            result = items.get(canonicalPath);
            if (result == null) {
                result = new FileCacheItem(file);
                items.put(canonicalPath.toLowerCase(), result);
            }
        }
        return result;
    }

    public FileCacheItem[] getChildDirs(FileCacheItem root) {
        FileCacheItem[] result = null;
        if (root != null) {
            result = root.childDirs;
            if (result == null) {
                boolean containsNomedia = false;
                IFile[] iFiles = root.item.listFiles();
                List<FileCacheItem> resultList = new ArrayList<>(iFiles.length);
                for (IFile i : iFiles) {
                    String path = i.getCanonicalPath();
                    if (path != null && path.toLowerCase().contains(FileUtilsBase.MEDIA_IGNORE_FILENAME)) {
                        containsNomedia = true;
                    }
                    if (i.isDirectory()) {
                        FileCacheItem item = this.put(i);
                        item.parent = root;
                        resultList.add(item);
                    }
                }
                root.isNomedia = containsNomedia;
                result = resultList.toArray(new FileCacheItem[resultList.size()]);
                root.childDirs = result;
            }

        }
        return result;
    }

    public void remove(IFile file) {
        remove(get(file));
    }

    public void remove(FileCacheItem item) {
        if (item != null) {
            if (item.childDirs != null) {
                for (FileCacheItem i : item.childDirs) {
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

    protected static class FileCacheItem implements Comparable<FileCacheItem> {
        final IFile item;
        FileCacheItem[] childDirs;
        FileCacheItem parent;
        Boolean isNomedia;
        Boolean isPrivate;

        FileCacheItem(IFile item) {
            this.item = item;
        }

        public IFile getItem() {
            return item;
        }

        public FileCacheItem[] getChildDirs() {
            return childDirs;
        }

        public FileCacheItem getParent() {
            return parent;
        }

        public Boolean getNomedia() {
            return isNomedia;
        }

        public Boolean getPrivate() {
            return isPrivate;
        }

        @Override
        public int compareTo(FileCacheItem fileCache) {
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
