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

package de.k3b.android.androFotoFinder.tagDB;

import java.util.Iterator;

/**
 * This Processor translates Path to id using dbSorted.
 *
 * Created by k3b on 21.09.2015.
 */
public abstract class Path2IdProcessor {
    /** process all items in csvSorted by getting the id that corresponds to path  */
    protected void process(Iterator<IPathID> csvSorted, Iterator<IPathID> dbSorted) {
        IPathID db = getNext(dbSorted);
        String dbPath = getNameWithoutExt(db);

        IPathID csv = getNext(csvSorted);
        String csvPath = getNameWithoutExt(csv);
        boolean found = false;

        while ((dbPath != null) && (csvPath != null)) {
            int compareResult = dbPath.compareTo(csvPath);
            if (compareResult == 0) {
                // dbPath == csvPath
                process(csv, db.getID());
                csv = getNext(csvSorted);
                csvPath = getNameWithoutExt(csv);
                found = true;
            } else if (compareResult < 0) {
                // dbPath < csvPath
                db = getNext(csvSorted);
                dbPath = getNameWithoutExt(db);
            } else {
                // dbPath > csvPath
                if (!found) {
                    // there is no matching item for csv
                    process(csv, null);
                }
                csv = getNext(csvSorted);
                csvPath = getNameWithoutExt(csv);
                found = false;
            }
        }
    }

    /** process one csv item.
     * id is null if csv does not exist in media db,
     * more than one csv item might match an id in case that there is a jpg and a xmp for it.*/
    abstract void process(IPathID csv, Integer id);

    private String getNameWithoutExt(IPathID csv) {
        if (csv != null) {
            String result = csv.getPath();

            if (result != null) {
                int last = result.lastIndexOf(".");
                return (last >= 0) ? result.substring(0,last) : result;
            }
        }
        return null;
    }

    private IPathID getNext(Iterator<IPathID> iterator) {
        return (iterator.hasNext()) ? iterator.next() : null;
    }
}

