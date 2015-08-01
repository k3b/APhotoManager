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

        if (itemListAsString != null) {
            String itemsAsString[] = itemListAsString.split(DELIMITER);
            for (String itemAsString : itemsAsString) {
                Long key = Long.valueOf(itemAsString);
                this.add(key);
            }
        }
        return this;
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
