package de.k3b.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Operating System Directory with load on demand
 *
 * Created by k3b on 04.08.2015.
 */
public class OSDirectory implements IDirectory {
    private File mCurrent = null;
    private List<IDirectory> mChilden = null;

    private OSDirectory mParent = null;
    public OSDirectory(String current, OSDirectory parent) {
        this(getCanonicalFile(current), parent);
    }

    public OSDirectory(File current, OSDirectory parent) {
        this(current, parent, null);
    }

    // package constructor to allow unittesting with fake children
    OSDirectory(File current, OSDirectory parent, List<IDirectory> childen) {
        setCurrent(current);
        mParent = parent;
        mChilden = childen;
    }

    public OSDirectory setCurrent(File current) {
        destroy();
        mCurrent = current;
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
                    if (file.isDirectory() && !file.isHidden() && !file.getName().startsWith(".")) {
                        mChilden.add(new OSDirectory(file, this));
                    }
                }
            }
        }
        return mChilden;
    }

    // package to allow unit testing
    IDirectory find(OSDirectory root, String path) {
        return find(root, getCanonicalFile(path));
    }

    // package to allow unit testing
    static IDirectory find(OSDirectory root, File file) {
        if (file == null) return null;
        if (root.mCurrent.equals(file)) {
            return root;
        }

        IDirectory parentDir = find(root, file.getParentFile());
        if (parentDir == null) return null;

        String name = file.getName();
        List<IDirectory> childred = parentDir.getChildren();
        for (IDirectory cur : childred) {
            if (name.equals(cur.getRelPath())) {
                return cur;
            }
        }

        OSDirectory result = new OSDirectory(file, (OSDirectory) parentDir);
        childred.add(result);
        return result;
    }

    @Override
    public IDirectory find(String path) {
        if (path == null) return null;
        return find(getCanonicalFile(path));
    }

    protected IDirectory find(File file) {
        return find(getRoot(), file);
    }

    @Override
    public void destroy() {
        destroy(mChilden);
        mChilden = null;
        mCurrent = null;
    }

    private void destroy(List<IDirectory> childen) {
        if (childen != null) {
            for (IDirectory child : childen) {
                child.destroy();
            }
            childen.clear();
        }
        this.mParent = null;
    }

    @Override
    public int getIconID() {
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

    public static File getCanonicalFile(String path) {
        try {
            return new File(path).getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
