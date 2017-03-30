/*
 * Copyright (c) 2016-2017 by k3b.
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

package de.k3b.media;

import java.util.Date;
import java.util.List;

/*
This is sourcecode of uml diagrams using
http://plantuml.com/
notation

pc2android.png
@startuml
    title IMetaApi implementation
    [IMetaApi] --> [MediaContentValues]
	[IMetaApi] --> [MediaCursor]
	[IMetaApi] --> [ExifInterfaceEx]
	[IMetaApi] --> [MediaCsvItem]
	[MediaDTO] <- [IMetaApi]
	[IMetaApi] --> [MediaXmpSegment]
	[IMetaApi] -> [MetaApiWrapper]
	
    [MediaContentValues] --> [MediaDB]
    [MediaCursor] <-- [MediaDB]

    [AndroidFileCommands44] -> [JPG file]
    [ExifInterfaceEx] <-- [JPG file]
    [ExifInterfaceEx] --> [JPG file]

    [MediaCsvItem] --> [CSV file]
    [MediaCsvItem] <-- [CSV file]

	[MediaXmpSegment]  --> [XMP file]
	[MediaXmpSegment]  <-- [XMP file]

@enduml


 */

/**
 * All Properties that are supported by "A Photo Manager".
 *
 * Created by k3b on 09.10.2016.
 */
public interface IMetaApi {
    /** Normalized absolute path to file (jpg or xmp) */
    String getPath();
    IMetaApi setPath(String filePath);

    /** When the photo was taken (not file create/modify date) in local time or utc*/
    Date getDateTimeTaken();
    IMetaApi setDateTimeTaken(Date value);

    /** Latitude, in degrees north. */
    IMetaApi setLatitude(Double latitude);
    /** Longitude, in degrees east. */
    IMetaApi setLongitude(Double longitude);
    Double getLatitude();
    Double getLongitude();

    /** Title = Short Descrioption used as caption */
    String getTitle();
    IMetaApi setTitle(String title);

    /** Longer description = comment. may have more than one line */
    String getDescription();
    IMetaApi setDescription(String description);

    /** Tags/Keywords/Categories/VirtualAlbum used to find images */
    List<String> getTags();
    IMetaApi setTags(List<String> tags);

    /** 5=best .. 1=worst or 0/null unknown */
    Integer getRating();
    IMetaApi setRating(Integer value);

}
