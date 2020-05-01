/*
 * Copyright (c) 2017-2020 by k3b.
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
package de.k3b.io;

/**
 *
 */
public class XmpFile extends FileWrapper implements IFile {
    private static final String EXT_SIDECAR = ".xmp";

    /**
     * true: file.jpg.xmp; false: file.xmp
     */
    private final boolean longFormat;
    private boolean hasAlsoOtherFormat = false;

    @Deprecated
    public XmpFile(String absolutePath, boolean longFormat) {
        this(FileFacade.convert("FileProcessor.XmpFile.absolutePath", absolutePath), longFormat);
    }

    public XmpFile(IFile file, boolean longFormat) {
        super(file);
        this.longFormat = longFormat;
    }

    public static boolean isSidecar(IFile file) {
        if (file == null) return false;
        return isSidecar(file.getName());
    }

    public static XmpFile getSidecar(IFile file, boolean longFormat) {
        if (file == null) return null;
        String name = file.getName();
        return getSidecar(file.getParentFile(), name, longFormat);
    }

    public static XmpFile getSidecar(IFile parent, String name, boolean longFormat) {
        XmpFile result;
        if (longFormat) {
            result = new XmpFile(parent.create(name + EXT_SIDECAR), longFormat);
        } else {
            result = new XmpFile(parent.create(FileUtils.replaceExtension(name, EXT_SIDECAR)), longFormat);
        }
        return result;

    }

    // private static final String LOG_FILE_ENCODING = "UTF-8";

    @Deprecated
    public static XmpFile getSidecar(String absolutePath, boolean longFormat) {
        XmpFile result;
        if (longFormat) {
            result = new XmpFile(absolutePath + EXT_SIDECAR, longFormat);
        } else {
            result = new XmpFile(FileUtils.replaceExtension(absolutePath, EXT_SIDECAR), longFormat);
        }
        return result;
    }

    public static boolean isSidecar(String name) {
        if (name == null) return false;
        return name.toLowerCase().endsWith(EXT_SIDECAR);
    }

    @Deprecated
    public static XmpFile getExistingSidecarOrNull(String absolutePath) {
        return getExistingSidecarOrNull(FileFacade.convert("getExistingSidecarOrNull from File", absolutePath));
    }

    public static XmpFile getExistingSidecarOrNull(IFile absolutePath) {
        XmpFile result = null;
        if (absolutePath != null) {
            XmpFile resultLong = getExistingSidecarOrNull(absolutePath, true);
            XmpFile resultShort = getExistingSidecarOrNull(absolutePath, false);

            if (resultLong != null) {
                result = resultLong;
                result.setHasAlsoOtherFormat(resultShort != null);
            } else {
                result = resultShort;
            }
        }
        return result;
    }

    @Deprecated
    public static XmpFile getExistingSidecarOrNull(String absolutePath, boolean longFormat) {
        return getExistingSidecarOrNull(FileFacade.convert("getExistingSidecarOrNull from path", absolutePath), longFormat);

    }

    public static XmpFile getExistingSidecarOrNull(IFile absolutePath, boolean longFormat) {
        XmpFile result = XmpFile.getSidecar(absolutePath, longFormat);
        if ((result == null) || !result.exists() || !result.isFile()) return null;
        return result;
    }

    /**
     * true: file.jpg.xmp; false: file.xmp
     */
    public boolean isLongFormat() {
        return longFormat;
    }

    public boolean isHasAlsoOtherFormat() {
        return hasAlsoOtherFormat;
    }

    public void setHasAlsoOtherFormat(boolean hasAlsoOtherFormat) {
        this.hasAlsoOtherFormat = hasAlsoOtherFormat;
    }


}
