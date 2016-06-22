package de.k3b.android.util;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

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
}
