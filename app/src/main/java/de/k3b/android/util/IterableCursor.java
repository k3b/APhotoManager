/*
 * Copyright (c) 2017 by k3b.
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
package de.k3b.android.util;

import java.util.Iterator;
import android.database.Cursor;

/** 
	Iterator that iterates over a Cursor or CursorWrapper.
    inspired by http://stackoverflow.com/questions/10723770/whats-the-best-way-to-iterate-an-android-cursor/28765773#28765773 
*/
public class IterableCursor<T extends Cursor> implements Iterable<T>, Iterator<T> {
    protected T cursor;
    protected int toVisit;
    public IterableCursor(T cursor) {
        this.cursor = cursor;
        toVisit = cursor.getCount();
    }
    public Iterator<T> iterator() {
        cursor.moveToPosition(-1);
        toVisit = cursor.getCount();
        return this;
    }
    public boolean hasNext() {
        return toVisit>0;
    }
    public T next() {
    //  if (!hasNext()) {
    //      throw new NoSuchElementException();
    //  }
        cursor.moveToNext();
        toVisit--;
        return cursor;
    }
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
