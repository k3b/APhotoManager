package de.k3b.android.util;

import android.support.annotation.NonNull;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.EditText;

import static android.view.MenuItem.SHOW_AS_ACTION_NEVER;

/**
 * Created by k3b on 20.06.2016.
 */
public class MenuUtils {
    public static void mov2SubMenu(Menu menue, String subMenue, int... ids2move) {
        if (menue != null) {
            SubMenu subMenu = menue.addSubMenu(Menu.NONE, Menu.NONE, 32767, subMenue);

            for (int idSub : ids2move) {
                MenuItem oldMenuItem = menue.findItem(idSub);

                if (oldMenuItem != null) {
                    menue.removeItem(idSub);
                    menue.removeItem(idSub);
                    menue.removeItem(idSub);
                    MenuItem newMenuItem = subMenu.add(oldMenuItem.getGroupId(), oldMenuItem.getItemId(), oldMenuItem.getOrder(),oldMenuItem.getTitle());
                    newMenuItem.setCheckable(oldMenuItem.isCheckable());
                    newMenuItem.setVisible(oldMenuItem.isVisible());
                }
            }
        }
    }

    /**
     *
     * @param menu
     * @param actionEnum How the item should display. One of
     *      {@link MenuItem#SHOW_AS_ACTION_ALWAYS}, {@link MenuItem#SHOW_AS_ACTION_IF_ROOM}, or
     *      {@link MenuItem#SHOW_AS_ACTION_NEVER}. SHOW_AS_ACTION_NEVER is the default.
     * @param menuIds 0 or more menu-ids to be modified.
     */
    public static void setShowAsActionFlags(Menu menu, int actionEnum, int... menuIds) {
        for (int idEdit : menuIds) {
            MenuItem sub = menu.findItem(idEdit);
            if (sub != null) sub.setShowAsActionFlags(SHOW_AS_ACTION_NEVER);
        }
    }

    public static void changeShowAsActionFlags(EditText edit, final int actionEnum, final int... menuIds) {
        ActionMode.Callback callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuUtils.setShowAsActionFlags(menu, actionEnum,
                        menuIds);
                return true; // create menue
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                /*
                enable(menu, android.R.id.copy, edit.getSelectionStart() >= 0);
                enable(menu, android.R.id.cut, edit.getSelectionStart() >= 0);
                */
                return false;
            }

            private void enable(Menu menu, int idEdit, boolean enabled) {
                MenuItem sub = menu.findItem(idEdit);
                if (sub != null) sub.setEnabled(enabled);
            }

            private void setNoAction(Menu menu, int idEdit) {
                MenuItem sub = menu.findItem(idEdit);
                if (sub != null) sub.setShowAsActionFlags(SHOW_AS_ACTION_NEVER);
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        };
        edit.setCustomSelectionActionModeCallback(callback);

    }


}
