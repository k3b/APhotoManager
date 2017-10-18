/*
 * Copyright (c) 2015-2017 by k3b.
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
import java.util.ArrayList;
import java.util.List;

import de.k3b.FotoLibGlobal;

/**
 * Operating System Directory with load on demand
 *
 * Created by k3b on 04.08.2015.
 */
public class OSDirectory implements IDirectory {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    private File mCurrent = null;
    private List<IDirectory> mChilden = null;

    private OSDirectory mParent = null;

    private int mDirFlags = DIR_FLAG_NONE;

    public OSDirectory(String current, OSDirectory parent) {
        this(FileUtils.tryGetCanonicalFile(current), parent);
    }

    public OSDirectory(File current, OSDirectory parent) {
        this(current, parent, null);
    }

    // protected constructor to allow unittesting with fake children
    protected OSDirectory(File current, OSDirectory parent, List<IDirectory> childen) {
        setCurrent(current);
        mParent = parent;
        mChilden = childen;
        if ((getDirFlags() == DIR_FLAG_NONE)
                && (mParent != null)
                && mParent.isDirFlagsNomedia()) {
            // inherit nomedia from parent
            setDirFlags(DIR_FLAG_NOMEDIA);
        }
    }

    public OSDirectory setCurrent(File current) {
        destroy();
        mCurrent = current;
        if (mCurrent != null) {
            if (new File(current, FileUtils.MEDIA_IGNORE_FILENAME).exists()) {
                setDirFlags(DIR_FLAG_NOMEDIA_ROOT);
            } else if (FileUtils.isHiddenFolder(current.getAbsolutePath())) {
                setDirFlags(DIR_FLAG_NOMEDIA);
            } else if (new File(current, RuleFileNameProcessor.APM_FILE_NAME).exists()) {
                setDirFlags(DIR_FLAG_APM_DIR);
            }
        }
        return this;
    }

    @Override
    public String getRelPath() {
        return mCurrent.getName();
    }

    @Override
    public String getAbsolute() {
        return mCurrent.getAbsolutePath();
    }

    @Override
    public IDirectory getParent() {
        return mParent;
    }

    private OSDirectory getRoot() {
        IDirectory current = this;
        IDirectory parent = current.getParent();
        while (parent != null) {
            current = parent;
            parent = current.getParent();
        }
        return (OSDirectory) current;
    }

    @Override
    public List<IDirectory> getChildren() {
        if ((mCurrent != null) && (mChilden == null)) {
            mChilden = new ArrayList<IDirectory>();
            File[] files = mCurrent.listFiles();
            if (files != null) {
                for (File file : files) {
                    if ((file != null)
                            && file.isDirectory()
                            && !file.isHidden()
                            && !file.getName().startsWith(".")
                            // && file.canWrite() // bugfix: must be visible because writeprotected parentdir may contain writeenabled subdirs
                            && !FileUtils.isSymlinkDir(file,true)) {
                        mChilden.add(new OSDirectory(file, this));
//                    } else if (FotoLibGlobal.debugEnabled) {
//                        logger.debug(FileUtils.getDebugString("OSDirectory.getChildren() rejected ", file));
                    }
                }
            }
        }
        return mChilden;
    }

    // package to allow unit testing
    protected IDirectory find(OSDirectory root, String path) {
        return find(root, FileUtils.tryGetCanonicalFile(path));
    }

    // protected to allow unit testing
    protected static IDirectory find(OSDirectory root, File file) {
        if (file == null) return null;
        if (root.mCurrent.equals(file)) {
            return root;
        }

        IDirectory parentDir = find(root, file.getParentFile());
        if (parentDir == null) return null;

        String name = file.getName();
        List<IDirectory> children = parentDir.getChildren();
        OSDirectory result = (OSDirectory) findChildByRelPath(children, name);

        if (result == null) {
            result = new OSDirectory(file, (OSDirectory) parentDir);
            children.add(result);
        }
        return result;
    }

    public static IDirectory findChildByRelPath(List<IDirectory> children, String name) {
        for (IDirectory cur : children) {
            if (name.equals(cur.getRelPath())) {
                return cur;
            }
        }
        return null;
    }

    @Override
    public IDirectory find(String path) {
        if (path == null) return null;
        return find(FileUtils.tryGetCanonicalFile(path));
    }

    protected IDirectory find(File file) {
        return find(getRoot(), file);
    }

    @Override
    public void destroy() {
        destroy(mChilden);
        mChilden = null;
        mCurrent = null;
        mParent = null;
    }

    private void destroy(List<IDirectory> childen) {
        if (childen != null) {
            for (IDirectory child : childen) {
                child.destroy();
            }
            childen.clear();
        }
    }

    @Override
    public int getSelectionIconID() {
        return 0;
    }

    @Override
    public String toString() {
        return mCurrent.toString();
    }

    public String toTreeString() {
        StringBuffer result = new StringBuffer();
        toTreeString(result, "", this);
        return result.toString();
    }

    public static void toTreeString(StringBuffer result, String indent, IDirectory dir) {
        result.append(indent).append(dir.getRelPath()).append("\n");

        // avoid load on demand
        List<IDirectory> mChilden = (dir instanceof OSDirectory) ? ((OSDirectory) dir).mChilden : dir.getChildren();
        if (mChilden != null) {
            String childIndent = indent + "-";
            for(IDirectory child : mChilden) {
                toTreeString(result, childIndent, child);
            }
        }
    }

    public OSDirectory addChildFolder(String newCildFolderName) {
        return addChildFolder(newCildFolderName, null);
    }

    /** for unittesting without load on demand */
    protected OSDirectory addChildFolder(String newCildFolderName, List<IDirectory> grandChilden) {
        List<IDirectory> children = this.getChildren();
        OSDirectory result = (OSDirectory) findChildByRelPath(children, newCildFolderName);

        if (result == null) {
            File newChildFile = FileUtils.tryGetCanonicalFile(new File(mCurrent, newCildFolderName), null);
            result = new OSDirectory(newChildFile, this, grandChilden);
            if (result != null) {
                children.add(result);
            }
        }

        return result;
    }

    /**
     * creates this folder (including parend-folder) in os-filesystem, if it does not exist.
     * @return false if path cannot be created.
     **/
    public boolean osMkDirs() {
        return mCurrent.mkdirs() || mCurrent.isDirectory();
    }

    @Override
    public int getDirFlags() {
        return mDirFlags;
    }

    public void setDirFlags(int dirFlags) {
        this.mDirFlags = dirFlags;
    }

    public boolean isDirFlagsNomedia() {
        return  (this.getDirFlags() & (DIR_FLAG_NOMEDIA | DIR_FLAG_NOMEDIA_ROOT))  != DIR_FLAG_NONE;
     }
}
