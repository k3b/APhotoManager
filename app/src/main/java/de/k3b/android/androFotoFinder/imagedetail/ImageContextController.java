/*
 * Copyright (c) 2017-2018 by k3b.
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

package de.k3b.android.androFotoFinder.imagedetail;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.k3b.android.GuiUtil;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.tagDB.TagSql;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.Dialogs;
import de.k3b.database.SqlTemplateEngine;
import de.k3b.io.FileUtils;

/**
 * #70: Handles optional extra context specific detail text that can be displayed in
 * the image detail view.
 *
 * Created by k3b on 09.05.2017.
 */

public abstract class ImageContextController {
    /** file name where definitions are stored */
    public static final String DEFINITION_FILE = "ImageContextSqlDef.properties";

    private static final String EXPR_DATE_TAKEN = "datetime(" + TagSql.SQL_COL_DATE_TAKEN + " /1000, 'unixepoch', 'localtime')";
    private static final String EXPR_DATE_MODIFIED = "datetime(" + TagSql.SQL_COL_LAST_MODIFIED + ", 'unixepoch', 'localtime')";
    private static final String EXPR_XMP_LAST_MODIFIED_DATE = "datetime(" + TagSql.SQL_COL_EXT_XMP_LAST_MODIFIED_DATE + ", 'unixepoch', 'localtime')";
    private static final String EXPR_RATING = "substr('*****',1," + TagSql.SQL_COL_EXT_RATING + ")";
    private static final String EXPR_TAGS = "replace(" + TagSql.SQL_COL_EXT_TAGS + ",';;', '; ')";
    private static final String EXPR_LAT_LON = TagSql.SQL_COL_LAT + " || ',' || " + TagSql.SQL_COL_LON;

    private Activity mContext   = null;
    private Menu mMenu = null;

    public ImageContextController(Activity context) {
        mContext = context;
    }

    /** called from gui to load an item from the list of possible items */
    public void onLoadFromQuestion() {
        final Properties properties = loadFromFile();

        Dialogs dlg = new Dialogs() {
            @Override protected boolean onContextMenuItemClick(int menuItemId, int itemIndex, String[] items) {
                if ((items != null) && (itemIndex >= 0) && (itemIndex < items.length)) {
                    String key = items[itemIndex];
                    String value = SqlTemplateEngine.toSql(properties.getProperty(key));
                    switch (menuItemId) {
                        case R.id.action_edit:
                            return onEdit(key, value, getPropertiesFile());

                        default:break;
                    }

                }
                return false;
            }

            @Override protected void onDialogResult(String name, Object[] parameters) {
                if (name != null) {
                    onListItemClick(name, SqlTemplateEngine.toSql(properties.getProperty(name)));
                }
            }
        };
        List<String> names = new ArrayList<>(properties.stringPropertyNames());

        dlg.pickFromStrings(mContext, mContext.getString(R.string.view_context_menu_title),
                R.menu.menu_image_detail_text_context, names);
    }

    private static File getPropertiesFile() {
        return new File(Global.reportDir, DEFINITION_FILE);
    }

    protected Properties loadFromFile() {
        final Properties properties = new Properties();
        File propFile = getPropertiesFile();

        if (propFile.exists()) {
            Reader br = null;

            try {

                br = new BufferedReader(new InputStreamReader(new FileInputStream(propFile), "UTF-8"));
                properties.load(br);

            } catch (IOException ex) {
                Log.e(Global.LOG_CONTEXT,
                        "Error reading " + propFile.getAbsolutePath() + ":" + ex.getMessage(), ex);

            }
            FileUtils.close(br, "after load");
        }
        includeMissingKeys(properties);
        return properties;
    }

    protected void saveToFile(File propFile, Properties properties) {
        includeMissingKeys(properties);

        Writer br = null;

        try {
            // create sensible defaults for domain-independant QueryParameter parsing
            String header = mContext.getString(R.string.image_context_properties_header,
                    mContext.getString(R.string.app_name),
                    GuiUtil.getAppVersionName(mContext));

            br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(propFile), "UTF-8"));
            br.write(header);
            br.write("\n");
            properties.store(br, null);

        } catch (IOException ex) {
            Log.e(Global.LOG_CONTEXT,
                    "Error writing " + propFile.getAbsolutePath() + ":" + ex.getMessage(), ex);

        }
        FileUtils.close(br, "after load");
    }

    /** opens text editor with the properties file */
    private boolean onEdit(String key, String value, File file) {
        Global.reportDir.mkdirs();
        saveToFile(file, loadFromFile());
        Intent sendIntent = new Intent();
        IntentUtil.setDataAndTypeAndNormalize(sendIntent, Uri.fromFile(file), "text/plain");
        sendIntent.setAction(Intent.ACTION_EDIT);
        mContext.startActivity(sendIntent);
        return true;
    }

    /** in display the "|" will be replaced by "\n" */
    private static final String nl = "'|'";
    /** add default definitions to newly created file */
    private void includeMissingKeys(Properties properties) {
        // add blanks to tag list to allow wrap to next line in display


        StringBuilder sqlAllFields = new StringBuilder();
        /*
        createString(sqlAllFields, "'#'" ,TagSql.SQL_COL_PK ,"' '" , TagSql.SQL_COL_PATH, nl);
        expr(sqlAllFields, TagSql.SQL_COL_EXT_TITLE);
        expr(sqlAllFields, TagSql.SQL_COL_EXT_DESCRIPTION);
        expr(sqlAllFields, EXPR_DATE_TAKEN);
        expr(sqlAllFields, EXPR_DATE_MODIFIED);
        expr(sqlAllFields, EXPR_XMP_LAST_MODIFIED_DATE);
        expr(sqlAllFields, EXPR_TAGS);
        expr(sqlAllFields, EXPR_LAT_LON);
        expr(sqlAllFields, EXPR_RATING);
        */
        sqlAllFields.append("#${").append(TagSql.SQL_COL_PK).append(
                "} ${").append(TagSql.SQL_COL_PATH).append("}|");
        sqlAllFields.append("${").append(TagSql.SQL_COL_EXT_TITLE).append("|}");
        sqlAllFields.append("${").append(TagSql.SQL_COL_EXT_DESCRIPTION).append("|}");
        sqlAllFields.append("${").append(EXPR_DATE_TAKEN).append("|}");
        sqlAllFields.append("${").append(EXPR_DATE_MODIFIED).append("|}");
        sqlAllFields.append("${").append(EXPR_XMP_LAST_MODIFIED_DATE).append("|}");
        sqlAllFields.append("${").append(EXPR_TAGS).append("|}");
        sqlAllFields.append("${").append(EXPR_LAT_LON).append("|}");
        sqlAllFields.append("${").append(EXPR_RATING).append("|}");

        include(properties,R.id.menu_item_rename, TagSql.SQL_COL_PATH);
        include(properties,R.id.cmd_move, TagSql.SQL_COL_PATH);
        include(properties,R.id.cmd_copy, TagSql.SQL_COL_PATH);
        include(properties,R.id.action_details, sqlAllFields.toString());
        include(properties,R.id.cmd_show_geo, EXPR_LAT_LON);
        include(properties,R.id.cmd_edit_geo, EXPR_LAT_LON);
        include(properties,R.id.cmd_show_geo_as, EXPR_LAT_LON);
        include(properties,R.id.cmd_edit_tags, EXPR_TAGS);
        include(properties,R.id.menu_exif, sqlAllFields.toString());
        include(properties,"html-test", "hello <b><font color='red'>${" + TagSql.SQL_COL_PATH + "}</font></b>");
    }

    private void include(Properties properties, int menuId, String value) {
        if (mMenu != null) {
            MenuItem menuItem = mMenu.findItem(menuId);
            CharSequence title = (menuItem == null) ? null : menuItem.getTitle();
            if (title != null) {
                include(properties, title.toString(), value);
            }
        }
    }

    private void include(Properties properties, String name, String value) {
        if (properties.getProperty(name, null) == null) {
            properties.setProperty(name, value);
        }
    }

    /** in display the "|" will be replaced by "\n" and html be expanded. */
    public static CharSequence sqlFormat(String detailsText) {
		String textWithNewLine = detailsText.replace ("|","<br/>");
        return Html.fromHtml(textWithNewLine, null, null);
    }

    private static String expr(StringBuilder result, String expr) {
        // sql: sting concatenation "||" becomes null if one of the substrings is null
        // example: 'hello ' || 'world' || null => null
        // sqLite special: ifnull(expr,'') ==> '' if expr is null
        result.append(" || ifnull(").append(expr).append("||'|','')");
        return "";
    }

    private static String createString(StringBuilder result, String... parts) {
        result.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(" || ").append(parts[i]);
        }
        return result.toString();
    }
    // !!!!! howto format date
    // "|datetime(" + TagSql.SQL_COL_DATE_TAKEN + " /1000, 'unixepoch', 'localtime')"

    /** called when the user selects an item from the list */
    abstract protected void onListItemClick(String name, String value);

    public void close() {
        this.mContext = null;
    }

    public String getPropertyValue(String name) {
        final Properties properties = loadFromFile();
        if (properties != null) {
            return SqlTemplateEngine.toSql(properties.getProperty(name));
        }
        return null;
    }

    public void setMenu(Menu menu) {
        this.mMenu = menu;
    }

    public Menu getMenu() {
        return mMenu;
    }
}
