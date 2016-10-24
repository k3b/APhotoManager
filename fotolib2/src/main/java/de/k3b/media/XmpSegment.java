/*
 * Copyright (c) 2016 by k3b.
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

package de.k3b.media;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPUtils;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.options.SerializeOptions;
import com.adobe.xmp.properties.XMPProperty;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by k3b on 20.10.2016.
 */

public class XmpSegment {
    private XMPMeta xmpMeta = null;

    protected String getPropertyAsString(XmpFieldDefinition... definitions) {
        try {
            XMPProperty result = getProperty(definitions);
            if (result != null) return result.getValue();
        } catch (XMPException e) {
            onError("getPropertyAsString", e);
        }
        return null;
    }

    protected Date getPropertyAsDate(XmpFieldDefinition... definitions) {
        try {
            String result = getPropertyAsString(definitions);
            if (result != null) return XMPUtils.convertToDate(result).getCalendar().getTime();
        } catch (XMPException e) {
            onError("getPropertyAsDate", e);
        }
        return null;
    }

    protected XMPProperty getProperty(XmpFieldDefinition... definitions) throws XMPException {
        for (XmpFieldDefinition definition: definitions) {
            XMPProperty result = getXmpMeta().getProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
            if (result != null) return result;
        }
        return null;
    }

    protected XmpFieldDefinition findFirst(boolean returnNullIfNotFound, XmpFieldDefinition... definitions) throws XMPException {
        for (XmpFieldDefinition definition: definitions) {
            XMPProperty result = getXmpMeta().getProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
            if (result != null) return definition;
        }
        return definitions[0];
    }

    protected void setProperty(Object value, XmpFieldDefinition... definitions) {
        try {
            XmpFieldDefinition definition = findFirst(value == null, definitions);
            if (definition != null) {
                getXmpMeta().setProperty(definition.getXmpNamespace().getUriAsString(), definition.getShortName(), value);
            } // else both porperty and value do not exist
        } catch (XMPException e) {
            onError("setProperty", e);
        }
    }

    protected void replacePropertyArray(XmpFieldDefinition definition, List<String> values) {
        try {
            XMPMeta meta = getXmpMeta();
            int oldItemCount = meta.countArrayItems(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
            for (int i = oldItemCount; i > 0; i--) {
                meta.deleteArrayItem(definition.getXmpNamespace().getUriAsString(), definition.getShortName(), i);
            }

            PropertyOptions option = new PropertyOptions(PropertyOptions.ARRAY);
            for (String value : values) {
                meta.appendArrayItem(definition.getXmpNamespace().getUriAsString(), definition.getShortName(),option, value,null);
            }
        } catch (XMPException e) {
            onError("replacePropertyArray", e);
        }
    }

    protected List<String>  getPropertyArray(XmpFieldDefinition definition) {
        try {
            XMPMeta meta = getXmpMeta();
            int oldItemCount = meta.countArrayItems(definition.getXmpNamespace().getUriAsString(), definition.getShortName());
            if (oldItemCount > 0) {
                List<String> values = new ArrayList<String>();
                for (int i = 1; i <= oldItemCount; i++) {
                    XMPProperty item = meta.getArrayItem(definition.getXmpNamespace().getUriAsString(), definition.getShortName(), i);
                    values.add(item.getValue());
                }
                return values;
            }
        } catch (XMPException e) {
            onError("getPropertyArray", e);
        }
        return null;
    }

    private void onError(String debugContext, XMPException e) {
        e.printStackTrace();
    }

    protected XMPMeta getXmpMeta() {
        if (xmpMeta == null) xmpMeta = XMPMetaFactory.create();
        return xmpMeta;
    }

    protected void setXmpMeta(XMPMeta xmpMeta) {
        this.xmpMeta = xmpMeta;
    }

    public XmpSegment load(InputStream is) {
        try {
            setXmpMeta(XMPMetaFactory.parse(is));
        } catch (XMPException e) {
            onError("load", e);
        }
        return this;
    }

    public XmpSegment save(OutputStream os, boolean humanReadable) {
        // humanReadable = false;
        try {
            SerializeOptions options = new SerializeOptions(0);
            options.setPadding(1);
            if (!humanReadable) options.setIndent("");
            XMPMetaFactory.serialize(getXmpMeta(), os, options);
        } catch (XMPException e) {
            onError("save", e);
        }
        return this;
    }
}
