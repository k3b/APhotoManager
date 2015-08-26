package de.k3b.android.androFotoFinder.testgui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;

/**
 * Add to Clipboard ContextActionBar: star (history) to open a popupmenu with previous values.
 * Long-Press if no selection => popup with previous values.
 *
 * Created by k3b on 26.08.2015.
 */
public class HistoryEditText {
    private final Context mContext;
    private final EditorHandler[] mEditorHandlers;

    /** ContextActionBar for one EditText */
    private class EditorHandler implements ActionMode.Callback, View.OnLongClickListener  {
        private static final String DELIMITER = "\n";
        private static final int MAX_HISTORY_LEN = 300;
        private final EditText mEditor;
        private final String mId;

        public EditorHandler(int id, EditText editor) {

            mId = HistoryEditText.class.getSimpleName()+"_"+id;
            mEditor = editor;
            mEditor.setCustomSelectionActionModeCallback(this);
            mEditor.setOnLongClickListener(this);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_history_edit_text, menu);
            // menu.removeItem(android.R.id.selectAll);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch(item.getItemId()) {

                case R.id.action_history:
                    showHistory();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }

        private void showHistory() {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
            String history = getHistory(sharedPref);
            String[] items = history.split(DELIMITER);

            PopupMenu popup = new PopupMenu(mContext, mEditor);
            Menu root = popup.getMenu();
            int len = items.length;
            if (len > 10) len = 10;
            for (int i = 0; i < len; i++) {
                String text = items[i].trim();

                if ((text != null) && (text.length() > 0)) {
                    root.add(text);
                }
            }

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    mEditor.setText(item.getTitle());
                    mEditor.setSelection(0,mEditor.length());
                    return true;
                }
            });
            popup.show();
        }

        private String getHistory(SharedPreferences sharedPref) {
            return sharedPref.getString(mId, DELIMITER);
        }

        void saveHistory(SharedPreferences sharedPref, SharedPreferences.Editor edit) {
            String history = getHistory(sharedPref);
            history = include(history, mEditor.getText().toString().trim());
            edit.putString(mId, history);
        }

        private String include(String history, String newValue) {
            if ((newValue.length() == 0) || (history.contains(DELIMITER + newValue + DELIMITER))) {
                // no newValue or already contained
                return history;
            }

            history = DELIMITER + newValue + history;

            if (history.length() >= MAX_HISTORY_LEN) {
                int start = MAX_HISTORY_LEN - (2 * newValue.length());
                start = history.lastIndexOf(DELIMITER, start);

                if (start >= 0) {
                    history = history.substring(0, start + 1);
                }
            }

            return history;
        }

        @Override
        public boolean onLongClick(View v) {

            if (mEditor.getSelectionEnd() - mEditor.getSelectionStart() > 0) {
                return false;
            }
            showHistory();
            return true;
        }
    }

    /** define history function for these editors */
    public HistoryEditText(Context context, EditText... editors) {
        this.mContext = context;
        mEditorHandlers = new EditorHandler[editors.length];

        for (int i = 0; i < editors.length; i++) {
            mEditorHandlers[i] = new EditorHandler(i,editors[i]);
        }

    }

    /** include current editor-content to history and save to settings */
    void saveHistory() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor edit = sharedPref.edit();

        for (EditorHandler instance: mEditorHandlers) {
            instance.saveHistory(sharedPref, edit);
        }
        edit.commit();
    }

}
