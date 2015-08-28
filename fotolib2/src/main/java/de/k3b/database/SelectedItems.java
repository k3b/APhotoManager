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

package de.k3b.database;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * #1 Gallery-Multiselection
 * Container for selected items.
 *
 * Created by k3b on 01.08.2015.
 */
public class SelectedItems extends java.util.TreeSet<Long> implements Set<Long> {

    private static final String DELIMITER = ",";

    /** converts this into komma seperated list */
    public String toString() {
        StringBuilder result = new StringBuilder();
        boolean mustAddDelimiter = false;
        Iterator<Long> iter = this.iterator();
        while(iter.hasNext()) {
            if (mustAddDelimiter) {
                result.append(DELIMITER);
            }
            mustAddDelimiter = true;
            result.append(iter.next());
        }
        return result.toString();
    }

    /** converts this into komma seperated list */
    public SelectedItems parse(String itemListAsString) {

        if ((itemListAsString != null) && (itemListAsString.length() > 0)) {
            String itemsAsString[] = itemListAsString.split(DELIMITER);
            for (String itemAsString : itemsAsString) {
                Long key = Long.valueOf(itemAsString);
                this.add(key);
            }
        }
        return this;
    }

    public Long[]  getIds() {
        return toArray(new Long[this.size()]);
    }

    /** return true if included; false if excluded */
    public boolean toggle(Long key) {
        if (contains(key)) {
            remove(key);
            return false;
        } else {
            add(key);
            return true;
        }
    }
}
