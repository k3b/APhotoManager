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
 
package de.k3b.io;

/**
 * Created by k3b on 13.07.2015.
 */
public interface IGeoRectangle {
    /** minimum latitude, in degrees north. -90..+90 */
    double getLatitudeMin();

    /** maximum latitude, in degrees north. -90..+90 */
    double getLatitudeMax();

    /** minimum longitude, in degrees east. -180..+180 */
    double getLogituedMin();

    /** maximum longitude, in degrees east. -180..+180 */
    double getLogituedMax();

    IGeoRectangle get(IGeoRectangle src);
}
