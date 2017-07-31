/*
 * Copyright (c) 2015-2016 by k3b.
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
    private final static int id_more = 199827;
    public static void mov2SubMenu(Menu menue, String subMenue, int... ids2move) {
        if (menue != null) {
            MenuItem subMenuOwner = menue.findItem(id_more);
            SubMenu subMenu = (subMenuOwner != null) ? subMenuOwner.getSubMenu() : null;
            if (subMenu == null) subMenu = menue.addSubMenu(Menu.NONE, id_more, 32767, subMenue);

            for (int idSub : ids2move) {
                MenuItem oldMenuItem = menue.findItem(idSub);

                if (oldMenuItem != null) {
                    menue.removeItem(idSub);
                    menue.removeItem(idSub);
                    menue.removeItem(idSub);
                    MenuItem newMenuItem = subMenu.add(oldMenuItem.getGroupId(), oldMenuItem.getItemId(), oldMenuItem.getOrder(),oldMenuItem.getTitle());
                    newMenuItem.setCheckable(oldMenuItem.isCheckable());
                    newMenuItem.setVisible(oldMenuItem.isVisible());
                    newMenuItem.setIcon(oldMenuItem.getIcon());
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

    public static MenuItem findByTitle(Menu menu, String title, int maxDepth) {
        if ((menu != null) && (title != null) && (title.length() > 0)) {
            int size = menu.size();

            MenuItem item;
            for (int i = 0; i < size; i++) {
                item = menu.getItem(i);
                if ((item != null) && (title.compareTo(item.getTitle().toString()) == 0))
                    return item;
            }

            if (maxDepth > 0) {
                int childDepth = maxDepth - 1;
                MenuItem subItem;
                for (int i = 0; i < size; i++) {
                    item = menu.getItem(i);
                    if ((item != null) && (item.hasSubMenu())) {
                        subItem = findByTitle(item.getSubMenu(), title, childDepth);
                        if (subItem != null) return subItem;
                    }
                }
            }
        }
        return null;
    }
}
