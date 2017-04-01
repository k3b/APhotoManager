/*
 * Copyright (c) 2015-2017 by k3b.
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
 
package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.IntentUtil;
import de.k3b.android.widget.Dialogs;
import de.k3b.database.QueryParameter;
import de.k3b.io.FileUtils;

/**
 * Created by k3b on 07.10.2015.
 */
public class BookmarkController {
    /** #76: used as default for save-as */
    private static final String STATE_LastBookmarkFileName = "LastBookmarkFileName";

    /** virtual bookmarkfile to reset is sourounden by this. */
    private static final String RESET_PREFIX = "<< ";
    private static final String RESET_SUFFIX = " >>";

    private QueryParameter mCurrentFilter = null;

    /** #76: used as default for save-as */
    private String mLastBookmarkFileName = null;

    private final Activity mContext;

    public interface IQueryConsumer {
        void setQuery(String fileName, QueryParameter newQuery);
    }

    public BookmarkController(Activity context) {
        mContext = context;
    }

    public static String getlastBookmarkFileName(Intent intent) {
        return (intent != null) ? intent.getStringExtra(BookmarkController.STATE_LastBookmarkFileName) : null;
    }

    public String getlastBookmarkFileName() {return mLastBookmarkFileName;}
    public void setlastBookmarkFileName(String lastBookmarkFileName) {mLastBookmarkFileName = lastBookmarkFileName;}

    public void saveState(Intent intent, Bundle savedInstanceState) {
        saveState(getlastBookmarkFileName(), intent, savedInstanceState);
    }

    public static void saveState(String lastBookmarkFileName, Intent intent, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            savedInstanceState.putString(BookmarkController.STATE_LastBookmarkFileName, lastBookmarkFileName);
        } else if (intent != null) {
            intent.putExtra(BookmarkController.STATE_LastBookmarkFileName, lastBookmarkFileName);
        }
    }

    public static boolean isReset(String bookmarkFilname) {
        return (bookmarkFilname != null) && bookmarkFilname.startsWith(RESET_PREFIX);
    }

    public static boolean isReset(Intent intent) {
        return isReset(getlastBookmarkFileName(intent));
    }

    public void loadState(Intent intent, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            setlastBookmarkFileName(savedInstanceState.getString(BookmarkController.STATE_LastBookmarkFileName, null));
        } else if (intent != null) {
            setlastBookmarkFileName(intent.getStringExtra(BookmarkController.STATE_LastBookmarkFileName));
        }
    }

    public void onSaveAsQuestion(final String name, final QueryParameter currentFilter) {
        mCurrentFilter = currentFilter;
        Dialogs dlg = new Dialogs() {
            @Override
            protected void onDialogResult(String fileName, Object[] parameters) {
                onSaveAsAnswer(fileName, true);
            }

        };

        if (isReset(name)) {
            dlg.editFileName(mContext, mContext.getString(R.string.bookmark_save_as_menu_title), null, 0);
        } else {
            dlg.editFileName(mContext, mContext.getString(R.string.bookmark_save_as_menu_title), name, 0);
        }
    }

    private void onSaveAsAnswer(final String fileName, boolean askToOverwrite) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, "onSaveAsAnswer(" + fileName +
                    ")");
        }
        if (fileName != null) {
            Global.reportDir.mkdirs();
            File outFile = getFile(fileName);
            if (askToOverwrite && outFile.exists()) {
                Dialogs dialog = new Dialogs() {
                    @Override
                    protected void onDialogResult(String result, Object... parameters) {
                        if (result != null) {
                            // yes, overwrite
                            onSaveAsAnswer(fileName, false);
                        } else {
                            // no, do not overwrite
                            onSaveAsQuestion(fileName, mCurrentFilter);
                        }
                    }
                };
                dialog.yesNoQuestion(mContext, mContext.getString(R.string.overwrite_question_title) ,
                        mContext.getString(R.string.image_err_file_exists_format, outFile.getAbsoluteFile()));
            } else {
                PrintWriter out = null;
                try {
                    out = new PrintWriter(outFile);
                    out.println(mCurrentFilter.toReParseableString());
                    out.close();
                    out = null;
                } catch (IOException err) {
                    String errorMessage = mContext.getString(R.string.mk_err_failed_format, outFile.getAbsoluteFile());
                    Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e(Global.LOG_CONTEXT, errorMessage, err);
                }
            }
        }
    }

    @NonNull
    private File getFile(String fileName) {
        String fileNameWithExt = fileName;

        if (!fileNameWithExt.contains(".")) {
            fileNameWithExt += Global.reportExt;
        }
        return new File(Global.reportDir, fileNameWithExt);
    }

    public void onLoadFromQuestion(final IQueryConsumer consumer, final QueryParameter currentFilter) {
        mCurrentFilter = currentFilter;
        List<String> fileNamesPlusReset = new ArrayList<String>();
        fileNamesPlusReset.add(RESET_PREFIX + mContext.getString(R.string.bookmark_reset) + RESET_SUFFIX);
        String[] fileNames = Global.reportDir.list(new FilenameFilter() {
            @Override public boolean accept(File dir, String filename) {
                return ((filename != null) && (filename.endsWith(Global.reportExt)));
            }
        });

        if ((fileNames != null) && (fileNames.length > 0)) {
            fileNamesPlusReset.addAll(Arrays.asList(fileNames));
        }
        Dialogs dlg = new Dialogs() {
            @Override protected boolean onContextMenuItemClick(int menuItemId, int itemIndex, String[] items) {
                return onBookmarkMenuItemClick(menuItemId, itemIndex, items);
            }

            @Override protected void onDialogResult(String fileName, Object[] parameters) {onLoadFromAnswer(fileName, consumer);}
        };
        dlg.pickFromStrings(mContext, mContext.getString(R.string.bookmark_load_from_menu_title), R.menu.menu_bookmark_context, fileNamesPlusReset);
    }

    public void onLoadFromAnswer(final String fileName, final IQueryConsumer consumer) {
        if (Global.debugEnabled) {
            Log.d(Global.LOG_CONTEXT, "onLoadFromAnswer(" + fileName + ")");
        }

        // #76: used as default for save-as
        mLastBookmarkFileName = fileName;
        if (fileName != null) {
            if (isReset(fileName)) {
                consumer.setQuery(fileName, new QueryParameter(FotoSql.queryDetail));
            } else {
                File inFile = getFile(fileName);

                String sql;
                try {
                    sql = FileUtils.readFile(inFile);
                    QueryParameter query = QueryParameter.parse(sql);
                    consumer.setQuery(fileName, query);
                } catch (Exception e) {
                    Toast.makeText(mContext,
                            e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(Global.LOG_CONTEXT, "Error load query file '" + inFile.getAbsolutePath() + "'", e);
                    e.printStackTrace();
                }
            }
        }
    }

    protected boolean onBookmarkMenuItemClick(int menuItemId, int itemIndex, String[] items) {
        if ((itemIndex > 0) && (itemIndex < items.length)) {
            switch (menuItemId) {
                case R.id.action_save_as:
                    onSaveAsQuestion(mLastBookmarkFileName, mCurrentFilter); return true;
                case R.id.action_edit:
                    return onEdit(getFile(items[itemIndex]));
                case R.id.menu_item_rename:
                    return onRenameQuestion(items[itemIndex], items[itemIndex]);
                case R.id.cmd_delete:
                    return onDeleteQuestion(itemIndex, items);
                default:break;
            }
        } // ignore index 0 = reset.
        return false;
    }

    private boolean onEdit(File file) {
        Intent sendIntent = new Intent();
        IntentUtil.setDataAndTypeAndNormalize(sendIntent, Uri.fromFile(file), "text/plain");
        sendIntent.setAction(Intent.ACTION_EDIT);
        mContext.startActivity(sendIntent);
        return true;
    }

    private boolean onRenameQuestion(String _newName, final String oldName) {
        if (_newName == null) return false;

        String newName = (_newName.endsWith(Global.reportExt))
                ? _newName.substring(0, _newName.length() - Global.reportExt.length())
                : _newName;

        Dialogs dialog = new Dialogs() {
            @Override
            protected void onDialogResult(String newFileName, Object... parameters) {
                if (newFileName != null) {
                    onRenameAnswer(newFileName, oldName);
                }
            }
        };
        dialog.editFileName(mContext, mContext.getString(R.string.rename_menu_title), newName);
        return true;
    }

    private void onRenameAnswer(String newFileName, String oldName) {
        if ((newFileName != null) || (newFileName.length() > 0)) { //  || (oldName.compareToIgnoreCase(newFileName) == 0)) {
            File from = getFile(oldName);
            File to = getFile(newFileName);

            if ((from.getAbsolutePath().compareToIgnoreCase(to.getAbsolutePath()) != 0) && !to.exists()) {
                from.renameTo(to);
            }
        }
    }

    private boolean onDeleteQuestion(final int itemIndex, final String[] items) {
        Dialogs dlg = new Dialogs() {
            @Override protected void onDialogResult(String result, Object[] parameters) {
                if (result != null) {
                    onDeleteAnswer(getFile(items[itemIndex]), itemIndex, items);
                }
            }
        };

        dlg.yesNoQuestion(mContext, items[itemIndex], mContext.getString(R.string.bookmark_delete_question));
        return true;
    }

    private void onDeleteAnswer(File file, int itemIndex, String[] items) {
        if (file.exists() && file.delete()) {
            String message = mContext.getString(R.string.bookmark_delete_answer_format, file.getAbsoluteFile() );
            Toast.makeText(mContext,
                     message,
                    Toast.LENGTH_LONG).show();
            Log.d(Global.LOG_CONTEXT, message);
            items[itemIndex] = null;
        } else {
            String message = mContext.getString(R.string.bookmark_delete_error_format, file.getAbsoluteFile() );
            Toast.makeText(mContext,
                    message,
                    Toast.LENGTH_LONG).show();
            Log.d(Global.LOG_CONTEXT, message);

        }
    }

}
