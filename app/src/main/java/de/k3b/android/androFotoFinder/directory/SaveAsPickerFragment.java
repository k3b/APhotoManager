/*
 * Copyright (c) 2018 by k3b.
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

package de.k3b.android.androFotoFinder.directory;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.io.File;

import de.k3b.android.androFotoFinder.AffUtils;
import de.k3b.android.androFotoFinder.R;
import de.k3b.android.androFotoFinder.imagedetail.ImageDetailActivityViewPager;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.AndroidFileCommands;
import de.k3b.android.util.OsUtils;
import de.k3b.io.AlbumFile;
import de.k3b.io.FileUtils;
import de.k3b.io.IDirectory;
import de.k3b.io.OSDirOrVirtualAlbumFile;
import de.k3b.io.OSDirectory;
import de.k3b.io.StringUtils;
import de.k3b.io.collections.SelectedFiles;

/**
 * a picker with a fale name field and a directory picker.
 *
 * Created by k3b on 12.04.2018.
 */

public abstract class SaveAsPickerFragment extends DirectoryPickerFragment {
    // these get lost on screen rotation so dialog must be closed on screen rotation.
    private EditText edName = null;
    private File path = null;
    private String extension = ".txt";

    public SaveAsPickerFragment(File path) {
        this.path = path;
        if (path != null) {
            this.extension = FileUtils.getExtension(path.getName());

            OSDirectory root = OsUtils.getRootOSDirectory(new OSDirOrVirtualAlbumFile(null, null, null));
            this.defineDirectoryNavigation(root, FotoSql.QUERY_TYPE_UNDEFINED, FileUtils.tryGetCanonicalPath(path, null));
        }
    }

    /* do not use activity callback */
    @Override
    protected void setDirectoryListener(Activity activity) {/* do not use activity callback */}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View result = super.onCreateView(inflater,container,savedInstanceState);

        result.findViewById(R.id.lblName).setVisibility(View.VISIBLE);
        edName = (EditText) result.findViewById(R.id.edName);
        edName.setVisibility(View.VISIBLE);

        if (path != null) {
            edName.setText(FileUtils.replaceExtension(path.getName(), ""));
        }
        return result;
    }

    public File getCurrentPath() {
        if (mCurrentSelection == null) return null;
        return new File(mCurrentSelection.getAbsolute(), edName.getText().toString() + extension );
    }

    /**
     * To be overwritten to check if a path can be picked.
     *
     * @param path to be checked if it cannot be handled
     * @return null if no error else error message with the reason why it cannot be selected
     */
    @Override
    protected String getStatusErrorMessage(String path) {
        // writeprotected
        // overwriteExisting
        // ok (new file or original file name proposal
        String errorMessage = null;
        if (errorMessage != null) {
            int pos = errorMessage.indexOf('\n');
            return (pos > 0) ? errorMessage.substring(0,pos) : errorMessage;
        }
        return super.getStatusErrorMessage(path);
    }

    @Override
    protected void onDirectoryPick(IDirectory selection) {
        IDirectory result = null;

        String filenameWithoutExtension = StringUtils.trim(this.edName.getText());
        String fullPath = (selection == null) ? null : selection.getAbsolute();
        File sel = (StringUtils.isNullOrEmpty(fullPath)) ? null : new File(fullPath);
        if (sel != null) {
            if (sel.isFile()) {
                result = selection;
            } else if (sel.isDirectory() && !StringUtils.isNullOrEmpty(filenameWithoutExtension)) {
                final String newName = filenameWithoutExtension + AlbumFile.SUFFIX_VALBUM;
                final File newFile = new File(sel, newName);
                result = selection.find(newFile.getAbsolutePath());
                if (result == null) {
                    result = selection.createOsDirectory(newFile, selection, null);
                }
            }
        }

        if (result != null) {
            // close dialog and return to caller
            super.onDirectoryPick(result);
            onFilePick(new File(result.getAbsolute()));
            this.notifyDataSetChanged();
            dismiss();
        }
    }

    /** decides if an item can be picked */
    protected boolean isPickable(IDirectory selection) {
        // if ((selection != null) && AlbumFile.isQueryFile (selection.getRelPath()) ) return true;
        return super.isPickable(selection);
    }

    @Override
    protected IDirectory getSelectedDir(String absolutePath) {
        if (absolutePath == null) return null;

        File abs = new File(absolutePath);
        if (!abs.isDirectory()) return super.getSelectedDir(abs.getParent());

        return super.getSelectedDir(absolutePath);
    }

    abstract protected void onFilePick(File pickedOrCreatedFile);
}
