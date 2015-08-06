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

    public OSDirectory(String current) {
        setCurrent(getCanonicalFile(current));
    }

    public OSDirectory(File current) {
        setCurrent(current);
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
        File parentFile = mCurrent.getParentFile();
        return (parentFile != null) ? new OSDirectory(parentFile) : null;
    }

    @Override
    public List<IDirectory> getChildren() {
        if ((mCurrent != null) && (mChilden == null)) {
            mChilden = new ArrayList<IDirectory>();
            File[] files = mCurrent.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && !file.isHidden() && !file.getName().startsWith(".")) {
                        mChilden.add(new OSDirectory(file));
                    }
                }
            }
        }
        return mChilden;
    }

    @Override
    public IDirectory find(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory() && file.canRead()) {
            return new OSDirectory(file);
        }
        return null;
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
    }

    @Override
    public int getIconID() {
        return 0;
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
