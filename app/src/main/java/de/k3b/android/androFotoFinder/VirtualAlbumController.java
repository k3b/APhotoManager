package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;

import java.io.File;

import de.k3b.android.androFotoFinder.directory.SaveAsPickerFragment;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.widget.Dialogs;
import de.k3b.database.QueryParameter;

/**
 * Encapsulates logic to handle virtual albums (former known as Bookmarks)
 *
 * Created by k3b on 21.05.2018.
 */

public class VirtualAlbumController  {
    public static final String DLG_SAVE_ALBUM_AS = "GalleryFilterActivitySaveAs";
    private final Activity mContext;

    public VirtualAlbumController(Activity context) {
        super();
        mContext = context;
    }

    public static class SaveAs extends SaveAsPickerFragment {
        private final VirtualAlbumController mVirtualAlbumController;
        private final QueryParameter mCurrentFilter;

        // only needed to prevent crash on rotation
        public SaveAs() {this(null, null, null);}
        public SaveAs(VirtualAlbumController virtualAlbumController, final File valbum, final QueryParameter currentFilter) {
            super(valbum);
            this.mVirtualAlbumController = virtualAlbumController;
            this.mCurrentFilter = currentFilter;
        }
        @Override
        protected void onFilePick(File pickedOrCreatedFile) {
            if (mVirtualAlbumController != null) mVirtualAlbumController.onSaveAsVirutalAlbumAnswer(pickedOrCreatedFile, mCurrentFilter);
        }
    }
    // workflow onSaveAsVirutalAlbumQuestion-onSaveAsVirutalAlbumAnswer-onSaveAsVirutalAlbumAllowOverwriteAnswer
    public DialogFragment onSaveAsVirutalAlbumQuestion(final File valbum, final QueryParameter currentFilter) {
        SaveAs dirDialog = new SaveAs(this, valbum, currentFilter);

        final FragmentManager manager = this.mContext.getFragmentManager();
        dirDialog.show(manager, DLG_SAVE_ALBUM_AS);
        return dirDialog;
    }

    // workflow onSaveAsVirutalAlbumQuestion-onSaveAsVirutalAlbumAnswer-onSaveAsVirutalAlbumAllowOverwriteAnswer
    private void onSaveAsVirutalAlbumAnswer(final File valbum, final QueryParameter currentFilter) {
        if (mustAskOverwrite(valbum)) {
            Dialogs dialog = new Dialogs() {
                @Override
                protected void onDialogResult(String result, Object... parameters) {
                    if (result != null) {
                        // yes, overwrite
                        onSaveAsVirutalAlbumAllowOverwriteAnswer(valbum, currentFilter);
                    } else {
                        // no, do not overwrite, ask again
                        onSaveAsVirutalAlbumQuestion(valbum, currentFilter);
                    }
                }
            };
            dialog.yesNoQuestion(this.mContext, this.mContext.getString(R.string.overwrite_question_title) ,
                    this.mContext.getString(R.string.image_err_file_exists_format, valbum.getAbsolutePath()));

        } else {
            // does not exist yet
            onSaveAsVirutalAlbumAllowOverwriteAnswer(valbum, currentFilter);
        }
    }

    private boolean mustAskOverwrite(File valbum) {
        if (valbum.exists()) return true;
        return false;
    }

    // workflow onSaveAsVirutalAlbumQuestion-onSaveAsVirutalAlbumAnswer-onSaveAsVirutalAlbumAllowOverwriteAnswer
    private void onSaveAsVirutalAlbumAllowOverwriteAnswer(File valbum, final QueryParameter currentFilter) {
        AndroidAlbumUtils.saveAs(mContext, valbum, currentFilter);
    }
}
