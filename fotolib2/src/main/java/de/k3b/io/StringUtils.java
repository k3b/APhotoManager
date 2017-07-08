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
package de.k3b.io;

/**
 * Created by k3b on 29.05.2017.
 */

public class StringUtils {

    public static int compare(String lhs, String rhs) {
        if (lhs != null) return (rhs != null) ? lhs.compareTo(rhs) : +1;
        return (rhs == null) ? 0 : -1;
    }

    /** null save function: return if both are null or both non-null are the same */
    public static boolean equals(Object lhs, Object rhs) {
        if (lhs != null) return (rhs != null) ? lhs.equals(rhs) : false;
        return (rhs == null);
    }

}
