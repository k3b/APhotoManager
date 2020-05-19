/*
 * Copyright (C) 2011-2020 by k3b
 * 
 * This file is part of de.k3b.android.toGoZip (https://github.com/k3b/ToGoZip/) .
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
package de.k3b.android.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import de.k3b.android.GuiUtil;
import de.k3b.android.androFotoFinder.R;

/**
 * html/Webview about preference entry showing
 * R.string.about_content with placeholders
 * $versionName$ and
 * $about$ (R.string.about_content_about)<br/>
 * Copyright (C) 2014-2020 by k3b
 */
public class AboutDialogPreference extends DialogPreference {
    private Context context;

    public AboutDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogIcon(R.drawable.foto_gallery);
        setDialogTitle(getAboutTitle(context));
        setDialogLayoutResource(R.layout.dialog_about);
        this.context = context;
    }

    @NonNull
    public static String getAboutTitle(Context context) {
        return context.getString(R.string.about_summary,context.getString(R.string.version_postfix));
    }

    public static void onPrepareOptionsMenu(Context context, Menu menu) {
        MenuItem item = menu.findItem(R.id.cmd_about);

        if (item != null) {
            item.setTitle(getAboutTitle(context));
        }
    }


    private static String getHtml(Context context) {
        String html = context.getResources().getString(R.string.about_content); // "<html><body>some <b>html</b> here</body></html>";

        final String versionName = GuiUtil.getAppVersionName(context);
        if (versionName != null) {
            html = html.replace("$versionName$", versionName);
        }

        html = html.replace("$translate$",
                context.getText(R.string.about_translate));
        html = html.replace("$about$",
                context.getText(R.string.about_content_about));
        return html;
    }

    public static Dialog createAboutDialog(Context context) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(getAboutTitle(context));
        alert.setIcon(R.drawable.foto_gallery);
        alert.setNeutralButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(
                            final DialogInterface paramDialogInterface,
                            final int paramInt) {
                        paramDialogInterface.cancel();

                    }
                }
        );

        WebViewUtil.setHtml(alert, context, getHtml(context));

        return alert.create();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        String html = getHtml(this.context);
        WebViewUtil.setHtml((WebView) view.findViewById(R.id.content), html);
    }


}
