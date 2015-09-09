/*
 * Copyright (c) 2015 by k3b.
 *
 * This file is part of LocationMapViewer.
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

/**
 * This Package defines Android independant code to handle *.gpx files with locations/trackpoints.
 *
 * <ul>
 *     <li>{@link de.k3b.geo.api.GeoPointDto}:
 *          a location or trackpoint that can be represented in a gpx file.</li>
 *     <li>{@link de.k3b.geo.io.gpx.GpxFormatter}:
 *          Formats {@link de.k3b.geo.api.GeoPointDto}-s or {@link de.k3b.geo.api.ILocation}-s as geo-xml.</li>
 *     <li>{@link de.k3b.geo.io.gpx.GpxReader}:
 *          reads {@link de.k3b.geo.api.GeoPointDto} from file or stream.</li>
 * </ul>
 *
 **/
package de.k3b.geo.io.gpx;
