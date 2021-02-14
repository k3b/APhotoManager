/*
 * Copyright (c) 2015-2021 by k3b.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import de.k3b.LibGlobal;
import de.k3b.io.filefacade.FileCache;
import de.k3b.io.filefacade.FileCacheItem;
import de.k3b.io.filefacade.FileFacade;
import de.k3b.io.filefacade.IFile;

/**
 * Operating System Directory with load on demand
 * <p>
 * Created by k3b on 04.08.2015.
 */
public class OSDirectory extends FileCacheItem<OSDirectory> implements IDirectory<OSDirectory> {
    private static final Logger logger = LoggerFactory.getLogger(LibGlobal.LOG_TAG);

    private int mDirFlags = DIR_FLAG_NONE;

    /**
     * if null use mCurrent.getName()
     */
    private String virtualName = null;

    private static final OsDirFileCache theCache = new OsDirFileCache();

    // protected constructor to allow unittesting with fake children
    public OSDirectory(IFile current, OSDirectory parent, OSDirectory[] childen) {
        super(current);
        super.setParent(parent);
        super.setChildDirs(childen);

        if ((getDirFlags() == DIR_FLAG_NONE)
                && (getParent() != null)
                && getParent().isDirFlagsNomedia()) {
            // inherit nomedia from parent
            setDirFlags(DIR_FLAG_NOMEDIA);
        }
    }

    public static OsDirFileCache getTheCache() {
        return theCache;
    }

    public static void setTheCache(OsDirFileCache theCache) {
        theCache = theCache;
    }

    // package to allow unit testing
    protected static OSDirectory find(OSDirectory root, String path) {
        return find(root, FileFacade.convert("OSDirectory find", path));
    }

    // protected to allow unit testing
    protected static OSDirectory find(OSDirectory root, IFile file) {
        if (file == null) return null;
        if (root.getCurrent().equals(file)) {
            return root;
        }

        OSDirectory provider = root.findViaFile(root, file);
        if (provider != null) return provider;

        OSDirectory parentDir = find(root, file.getParentFile());
        if (parentDir == null) return null;

        String name = file.getName();
        OSDirectory result = (OSDirectory) findChildByRelPath(parentDir.getChildDirs(null), name);

        if (result == null) {
            result = root.createOsDirectory(file, parentDir, null);
            parentDir.addChild(result);
        }
        return result;
    }

    public static IDirectory findChildByRelPath(IDirectory[] children, String name) {
        if (children != null) {
            for (IDirectory cur : children) {
                if (name.equals(cur.getRelPath())) {
                    return cur;
                }
            }
        }
        return null;
    }

    public static void toTreeString(StringBuffer result, String indent, IDirectory dir) {
        result.append(indent).append(dir.getRelPath()).append("\n");

        // avoid load on demand
        IDirectory[] children = dir.getChildDirs();
        if (children != null) {
            String childIndent = indent + "-";
            for (IDirectory child : children) {
                toTreeString(result, childIndent, child);
            }
        }
    }

    private static OSDirectory[] add(OSDirectory[] oldChildren, OSDirectory... newChildren) {
        if (newChildren == null || newChildren.length == 0) return oldChildren;
        if (oldChildren == null || oldChildren.length == 0) return newChildren;

        OSDirectory[] result = new OSDirectory[newChildren.length + oldChildren.length];
        System.arraycopy(oldChildren, 0, result, 0, oldChildren.length);

        System.arraycopy(newChildren, 0, result, oldChildren.length, newChildren.length);
        return result;
    }

    /**
     * reloads entry where dirFlag is one of the DIR_FLAG_... values
     */
    @Override
    public void refresh() {
        setDirFlags(getCalculateFlags(getCurrent()));
    }

    @Override
    public OSDirectory setCurrent(IFile current) {
        super.setCurrent(current);
        destroy();
        setDirFlags(getCalculateFlags(getCurrent()));
        return this;
    }

    private OSDirectory getRoot() {
        IDirectory current = this;
        IDirectory parent = current.getParent();
        while ((parent != null) && (parent != this)) {
            current = parent;
            parent = current.getParent();
        }
        return (OSDirectory) current;
    }

    protected int getCalculateFlags(IFile directory) {
        int result = 0;
        if ((directory != null) && (directory.isDirectory())) {
            if (directory.create(FileUtils.MEDIA_IGNORE_FILENAME).exists()) {
                result = DIR_FLAG_NOMEDIA_ROOT;
            } else if (FileUtils.isHiddenFolder(directory.getAbsolutePath())) {
                result = DIR_FLAG_NOMEDIA;
            }

            if (directory.create(RuleFileNameProcessor.APM_FILE_NAME).exists()) {
                result |= DIR_FLAG_APM_DIR;
            }
        }

        return result;
    }

    @Override
    public String getRelPath() {
        if (this.virtualName != null) return this.virtualName;
        return getCurrent().getName();
    }

    @Override
    public String getAbsolute() {
        return getCurrent().getAbsolutePath();
    }

    /**
     * #114: update internal data after a folder has been renamed in the gui
     */
    @Override
    public void rename(String oldFolderName, String newFolderName) {
        this.setCurrent(getCurrent().getParentFile().create(newFolderName));
    }

    /**
     * factory method to be overwrittern by derived classes, if tree should consist of derived classes.
     */
    @Override
    public OSDirectory createOsDirectory(IFile file, OSDirectory parent, OSDirectory[] children) {
        return new OSDirectory(file, (OSDirectory) parent, children);
    }

    public OSDirectory[] createOsDirectoryArray(int size) {
        return new OSDirectory[size];
    }

    @Override
    public void destroy() {
        // throw new IllegalStateException();
    }

    @Override
    public void removeChild(OSDirectory... child) {
        setChildDirs(Directory.removeChild(this, getChildDirs(), child));
    }

    @Override
    public void addChild(OSDirectory... child) {
        setChildDirs(add(getChildDirs(), child));
    }

    @Override
    public int childIndexOf(IDirectory child) {
        return Directory.childIndexOf(getChildDirs(), child);
    }

    protected boolean isDirectory(IFile file) {
        return file.isDirectory();
    }

    @Override
    public OSDirectory find(String path) {
        if (path == null) return null;
        return find(FileFacade.convert("OSDirectory find ", path).getCanonicalFile());
    }

    protected OSDirectory find(IFile file) {
        return find(getRoot(), file);
    }

    @Override
    public int getSelectionIconID() {
        return 0;
    }

    @Override
    public String toString() {
        return getCurrent().toString();
    }

    public String toTreeString() {
        StringBuffer result = new StringBuffer();
        toTreeString(result, "", this);
        return result.toString();
    }

    private void destroy(IDirectory[] childen) {
        if (childen != null) {
            for (IDirectory child : childen) {
                child.destroy();
            }
        }
    }

    public OSDirectory includeRoot(IFile file, OSDirectory factory) {
        OSDirectory root = getRoot();
        OSDirectory candidate = findViaFile(root, file);
        if (candidate != null) return candidate;
        if (factory == null) factory = this;
        OSDirectory result = factory.createOsDirectory(file, root, null);
        result.virtualName = file.getCanonicalPath();
        root.addChild(result);

        return result;
    }

    protected OSDirectory findViaFile(OSDirectory parentDir, IFile file) {
        OSDirectory[] childDirs = (parentDir != null) ? parentDir.getChildDirs(null) : null;
        if (childDirs != null) {
            for (IDirectory candidate : childDirs) {
                if (candidate.equals(file)) return (OSDirectory) candidate;
            }
        }
        return null;
    }

    /** return the deepest added childFolder */
    public OSDirectory addChildFolder(String newCildFolderName) {
        if ((newCildFolderName != null) && (!newCildFolderName.isEmpty())) {
            String[] subfolderNames = newCildFolderName.split("/|\\\\");
            return addChildSubFolders(subfolderNames);
        }
        return null;
    }

    /** return the deepest added childFolder */
    private OSDirectory addChildSubFolders(String... newCildFolderNames) {
        OSDirectory current = this;
        if ((newCildFolderNames != null) && (newCildFolderNames.length > 0)) {
            for (String newCildFolderName : newCildFolderNames) {
                if ((newCildFolderName != null) && (!newCildFolderName.isEmpty())) {
                    current = current.addChildFolder(newCildFolderName, null);
                }
            }
        }
        return current;
    }

    private OSDirectory addChildFolder(String newCildFolderName, OSDirectory[] grandChilden) {
        OSDirectory result = null;
        OSDirectory[] children = this.getChildDirs(null);
        File newRelativeChild = new File(newCildFolderName);
        if (!newRelativeChild.isAbsolute()) {
            result = (OSDirectory) findChildByRelPath(children, newCildFolderName);

            if (result == null) {
                IFile newChildFile = getCurrent().create(newCildFolderName).getCanonicalFile();
                result = createOsDirectory(newChildFile, this, grandChilden);
                if (result != null) {
                    this.addChild(result);
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof OSDirectory)
            return this.getCurrent().equals(((OSDirectory) o).getCurrent());
        if (o instanceof IFile) return this.getCurrent().equals(o);
        return super.equals(o);
    }

    /**
     * creates this folder (including parend-folder) in os-filesystem, if it does not exist.
     * @return false if path cannot be created.
     **/
    public boolean osMkDirs() {
        return getCurrent().mkdirs() || getCurrent().isDirectory();
    }

    @Override
    public int getDirFlags() {
        return mDirFlags;
    }

    public void setDirFlags(int dirFlags) {
        this.mDirFlags = dirFlags;
    }

    public boolean isDirFlagsNomedia() {
        return (this.getDirFlags() & (DIR_FLAG_NOMEDIA | DIR_FLAG_NOMEDIA_ROOT)) != DIR_FLAG_NONE;
    }

    @Override
    public OSDirectory[] getChildDirs() {
        return getChildDirs(theCache);
    }


    public static class OsDirFileCache extends FileCache<OSDirectory> {
        @Override
        public OSDirectory create(IFile file) {
            return new OSDirectory(file, null, null);
        }

        @Override
        public OSDirectory[] create(int size) {
            return new OSDirectory[0];
        }

    }

}
