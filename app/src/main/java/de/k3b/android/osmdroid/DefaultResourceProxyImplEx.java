package de.k3b.android.osmdroid;

import android.content.Context;
import android.content.res.Resources;

import org.osmdroid.DefaultResourceProxyImpl;

/**
 * Created by k3b on 16.07.2015.
 */
public class DefaultResourceProxyImplEx extends DefaultResourceProxyImpl {
    private Resources mResources = null;
    public DefaultResourceProxyImplEx(Context pContext) {
        super(pContext);
        if (pContext != null) {
            mResources = pContext.getResources();
        }
    }

    public Resources getResources() {
        return mResources;
    }
}
