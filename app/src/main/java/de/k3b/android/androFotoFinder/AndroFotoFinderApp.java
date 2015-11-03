/*
 * Copyright (c) 2015 by k3b.
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
 
package de.k3b.android.androFotoFinder;

import android.app.Application;

import java.util.ArrayList;
import java.util.Date;

import de.k3b.android.GuiUtil;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.database.QueryParameter;

// import com.squareup.leakcanary.LeakCanary;
// import com.squareup.leakcanary.RefWatcher;

/**
 * Created by k3b on 14.07.2015.
 */
public class AndroFotoFinderApp extends Application {
    /*
    private RefWatcher refWatcher;

    public static RefWatcher getRefWatcher(Context context) {
        AndroFotoFinderApp application = (AndroFotoFinderApp) context.getApplicationContext();
        return application.refWatcher;
    }

    @Override public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
    */
    @Override public void onCreate() {
        super.onCreate();

        // create sensible defaults for domain-independant QueryParameter parsing
        QueryParameter.sParserComment = getString(R.string.query_param_comment,
                getString(R.string.app_name),
                GuiUtil.getAppVersionName(this),
                new Date().toString());

        QueryParameter.sParserDefaultFrom = FotoSql.SQL_TABLE_EXTERNAL_CONTENT_URI.toString();
        QueryParameter.sParserDefaultQueryTypeId = FotoSql.QUERY_TYPE_DEFAULT;
        QueryParameter.sParserDefaultSelect = new ArrayList<String>();
        for (String columnName : FotoSql.DEFAULT_GALLERY_COLUMNS) {
            QueryParameter.sParserDefaultSelect.add(columnName);
        }
    }
}
