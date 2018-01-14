/*
 * Copyright (c) 2016-2018 by k3b.
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

package de.k3b.media;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.options.SerializeOptions;
import com.adobe.xmp.properties.XMPProperty;
import com.adobe.xmp.properties.XMPPropertyInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.k3b.FotoLibGlobal;
import de.k3b.io.FileUtils;
import de.k3b.tagDB.TagConverter;

/**
 * Hides Implementation details of xmp lib
 * Created by k3b on 20.10.2016.
 */

public class XmpSegment {
    public static final String DBG_PREFIX = "XmpSegment: ";
    private static String dbg_context = DBG_PREFIX;
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);
    // public: can be changed in settings dialog
    public  static boolean DEBUG = false;

    private XMPMeta xmpMeta = null;
    private static XMPSchemaRegistry registry = XMPMetaFactory.getSchemaRegistry();

    /** when xmp sidecar file was last modified (in secs 1970) or 0 */
    private long filelastModified = 0;

    protected String getPropertyAsString(String debugContext, MediaXmpFieldDefinition... definitions) {
        List<String> values = getPropertyArray("", definitions);
        if ((values != null) && (values.size() > 0)) {
            return TagConverter.asDbString(null, values);
        }

        XMPProperty result = getProperty(definitions);
        if (result != null) {
            return result.getValue();
        }
        return null;
    }

    protected Date getPropertyAsDate(String debugContext, MediaXmpFieldDefinition... definitions) {
        try {
            String result = getPropertyAsString(debugContext, definitions);
            if ((result != null) && (result.length() > 0)) return XMPUtils.convertToDate(result).getCalendar().getTime();
        } catch (XMPException e) {
            onError("getPropertyAsDate", e);
        }
        return null;
    }

    protected XMPProperty getProperty(MediaXmpFieldDefinition... definitions) {
        for (MediaXmpFieldDefinition definition: definitions) {
            if (!definition.isArray()) {
                XMPProperty result = null;
                try {
                    result = getXmpMeta().getProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
                } catch (XMPException e) {
                    onError("getProperty(" + definition + ")", e);
                    result = null;
                }
                if (result != null) return result;
            }
        }
        return null;
    }

    protected MediaXmpFieldDefinition findFirst(boolean returnNullIfNotFound, MediaXmpFieldDefinition... definitions)  {
        for (MediaXmpFieldDefinition definition: definitions) {
            XMPProperty result = null;
            try {
                result = getXmpMeta().getProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
            } catch (XMPException e) {
                onError("findFirst(" + definition + ")", e);
                result = null;
            }
            if (result != null) return definition;
        }
        return definitions[0];
    }

    /** sets all existing from definitions or first if not found */
    protected void setProperty(Object value, MediaXmpFieldDefinition... definitions) {
        try {
            boolean mustAdd = true;
            for (MediaXmpFieldDefinition definition: definitions) {
                XMPProperty result = getXmpMeta().getProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
                if (result != null) {
                    setPropertyInternal(value, definition);
                    mustAdd = false;
                }
            }
            if (mustAdd) {
                setPropertyInternal(value, definitions[0]);
            }
        } catch (XMPException e) {
            onError("setProperty", e);
        }
    }

    private void setPropertyInternal(Object value, MediaXmpFieldDefinition definition) throws XMPException {
        if (definition != null) {
            if (value == null) {
                // XMPProperty prop = getXmpMeta().getProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
                // getXmpMeta().setProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName(), "");
                getXmpMeta().deleteProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
            } else if (definition.isArray()) {
                replacePropertyArray(TagConverter.fromString(value), definition);
            } else {
                getXmpMeta().setProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName(), value);
            }
        } // else both porperty and value do not exist
    }

    protected void replacePropertyArray(List<String> values, MediaXmpFieldDefinition... definitions) {
        try {
            MediaXmpFieldDefinition definition = findFirst(false, definitions);
            if ((definition != null) && definition.isArray()) {
                XMPMeta meta = getXmpMeta();
                int oldItemCount = meta.countArrayItems(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
                for (int i = oldItemCount; i > 0; i--) {
                    meta.deleteArrayItem(definition.getXmpNamespace().getUriAsString(), definition.getShortName(), i);
                }

                if (values != null) {
                    PropertyOptions option = new PropertyOptions(definition.getArrayOption());
                    for (String value : values) {
                        meta.appendArrayItem(definition.getXmpNamespace().getUriAsString(),
                                definition.getShortName(), option, value, null);
                    }
                }
            }
        } catch (XMPException e) {
            onError("replacePropertyArray", e);
        }
    }

    protected List<String>  getPropertyArray(String debugContext, MediaXmpFieldDefinition... definitions) {
        try {
            XMPMeta meta = getXmpMeta();
            for (MediaXmpFieldDefinition definition : definitions) {
                if (definition.isArray()) {
                    int oldItemCount = meta.countArrayItems(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
                    if (oldItemCount > 0) {
                        List<String> values = new ArrayList<String>();
                        for (int i = 1; i <= oldItemCount; i++) {
                            XMPProperty item = meta.getArrayItem(definition.getXmpNamespace().getUriAsString(), definition.getShortName(), i);
                            values.add(item.getValue());
                        }
                        return values;
                    }
                }
            }
        } catch (XMPException e) {
            onError("getPropertyArray", e);
        }
        return null;
    }

    protected void onError(String message, Exception e) {
        logger.error(dbg_context + message, e);
    }

    protected XMPMeta getXmpMeta() {
        if (xmpMeta == null) xmpMeta = XMPMetaFactory.create();
        return xmpMeta;
    }

    public XmpSegment setXmpMeta(XMPMeta xmpMeta, String dbg_context) {
        if (dbg_context != null) {
            this.dbg_context = dbg_context + DBG_PREFIX;
        }

        this.xmpMeta = xmpMeta;

        return this;
    }

    public XmpSegment load(InputStream is, String dbg_context) {
        try {
            setXmpMeta(XMPMetaFactory.parse(is), dbg_context);
        } catch (XMPException e) {
            onError("->XmpSegment.load", e);
        }
        return this;
    }

    public XmpSegment load(File file, String dbg_context) throws FileNotFoundException {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            setXmpMeta(XMPMetaFactory.parse(stream), dbg_context + " file:" + file);
        } catch (XMPException e) {
            onError("->XmpSegment.load " + file, e);

            // workaround: my android-4.2 tahblet cannot re-read it-s xmp without trailing "\n"
            if ((file != null) && file.exists()) {
                try {
                    setXmpMeta(XMPMetaFactory.parse(FileUtils.streamFromStringContent(FileUtils.readFile(file) + "\n")), this.dbg_context);
                } catch (IOException e1) {
                    onError("->XmpSegment.load-via-string " + file, e);
                } catch (XMPException e1) {
                    onError("->XmpSegment.load-via-string " + file, e);
                }
            }
        } finally {
            FileUtils.close(stream, file);
            setFilelastModified(file);
        }
        return this;
    }

    public XmpSegment save(File file, boolean humanReadable, String dbg_context) throws FileNotFoundException {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            save(stream, humanReadable, dbg_context + file);
        } finally {
            FileUtils.close(stream, file);
            setFilelastModified(file);
        }
        return this;

    }

    public XmpSegment save(OutputStream os, boolean humanReadable, String dbg_context) {
        if (dbg_context != null) {
            this.dbg_context = dbg_context + DBG_PREFIX;
        }

        // humanReadable = false;
        try {
            SerializeOptions options = new SerializeOptions(0);
            options.setPadding(1);
            if (!humanReadable) options.setIndent("");
            XMPMetaFactory.serialize(getXmpMeta(), os, options);

            // workaround: my android-4.2 tahblet cannot re-read it-s xmp without this. on my android-4.4 handset this is not neccessary
            os.write("\n".getBytes());
        } catch (IOException e) {
            onError("save", e);
        } catch (XMPException e) {
            onError("save", e);
        }
        return this;
    }

    public String toString() {
        final StringBuilder result = new StringBuilder();
        appendXmp(null, result);
        if (result.length() > 0) return result.toString();
        return null;
    }

    public void appendXmp(String printPrefix, StringBuilder result) {
        XMPMeta meta = getXmpMeta();
        try {
            for (XMPIterator it = meta.iterator(); it.hasNext();)
            {
                XMPPropertyInfo prop = (XMPPropertyInfo) it.next();
                appendXmpPropertyInfo(result, printPrefix, prop);
            }
        } catch (XMPException e) {
            result.append(e.toString());
            onError("appendXmp",e);
        }
    }

    /**
     * @param printPrefix
     * @param prop an <code>XMPPropertyInfo</code> from the <code>XMPIterator</code>.
     */
    private void appendXmpPropertyInfo(final StringBuilder result, String printPrefix, XMPPropertyInfo prop) {
        String path = prop.getPath();
        if (path == null) {
            result.append("\n");
        } else {
            if (printPrefix != null) result.append(printPrefix);

            String namespace = prop.getNamespace();
            String prefix = registry.getNamespacePrefix(namespace);
            if (prefix == null) prefix = namespace;

            if (prefix != null) result.append(prefix).append(".");

            result  .append(path)
                    .append("=").append(prop.getValue());
            if (DEBUG) result  .append(" (").append(prop.getOptions().getOptionsString()).append(")");
            result  .append("\n");
        }
    }

    /** when xmp sidecar file was last modified or 0 */
    public void setFilelastModified(File file) {
        if (file != null) this.filelastModified = file.lastModified() / 1000; // File/Date has millisecs
    }

    /** when xmp sidecar file was last modified in secs since 1970 or 0 */
    public long getFilelastModified() {
        return filelastModified;
    }
}
