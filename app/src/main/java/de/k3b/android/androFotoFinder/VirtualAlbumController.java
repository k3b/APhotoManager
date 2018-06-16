package de.k3b.android.androFotoFinder;

import android.app.Activity;
import android.app.FragmentManager;

import java.io.File;

import de.k3b.android.androFotoFinder.directory.SaveAsPickerFragment;
import de.k3b.android.widget.Dialogs;
import de.k3b.database.QueryParameter;

/**
 * Encapsulates logic to handle virtual albums (former known as Bookmarks)
 *
 * Created by EVE on 21.05.2018.
 */

public class VirtualAlbumController extends BookmarkController {
    public static final String DLG_SAVE_AS_TAG = "GalleryFilterActivitySaveAs";
    private final Activity mContext;

    public VirtualAlbumController(Activity context) {
        super(context);
        mContext = context;
    }

    // workflow onSaveAsVirutalAlbumQuestion-onSaveAsVirutalAlbumAnswer-onSaveAsVirutalAlbumAllowOverwriteAnswer
    public void onSaveAsVirutalAlbumQuestion(final File valbum, final QueryParameter currentFilter) {
        SaveAsPickerFragment dirDialog = new SaveAsPickerFragment(valbum) {
            @Override
            protected void onFilePick(File pickedOrCreatedFile) {
                onSaveAsVirutalAlbumAnswer(pickedOrCreatedFile, currentFilter);
            }
        };

        final FragmentManager manager = this.mContext.getFragmentManager();
        dirDialog.show(manager, DLG_SAVE_AS_TAG);
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
        onSaveAs(valbum, currentFilter);
    }

    public File getlastBookmarkFile() {
        String name = getlastBookmarkFileName();
        return name == null ? null : new File(name);
    }
}
