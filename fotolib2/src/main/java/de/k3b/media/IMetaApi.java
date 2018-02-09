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

import de.k3b.io.VISIBILITY;

/*
This is sourcecode of uml diagrams using
http://plantuml.com/
notation

metadata.png
@startuml
    title Meta Data
	package "Jpg file" {
	    package "file" {
            [Path]
            [lastModified]
	    }
		[lastModified]-[hidden]-[Jpg.Description]
    	package "Exif-Extended (A Photo Manager read/write)" {
			package "Exif (Android Stock Media Scanner)" {
				[Jpg.Description]-[hidden]-[DateTimeTaken(1)]
				[Jpg.Description]-[hidden]-[Latitude Longitude(1)]
			}
			[DateTimeTaken(1)]-[hidden]-[Title(2)]
			[DateTimeTaken(1)]-[hidden]-[Description(1)]
            [DateTimeTaken(1)]-[hidden]-[Tags(2)]
            [DateTimeTaken(1)]-[hidden]-[Rating(2)]
		}
		package "Iptc (A Photo Manager read only)" {
		    ' appendend one blank
			[Description(1)]-[hidden]-[Title(3)]
			[Description(1)]-[hidden]-[Description(3)]
			[Description(1)]-[hidden]-[Tags(3)]
		}
		package "Xmp in jpg (A Photo Manager read only)" {
		    ' appendend two blanks
			[Description(3)]-[hidden]-[DateTimeTaken(3)]
			[Description(3)]-[hidden]-[Latitude Longitude(3)]
			[DateTimeTaken(3)]-[hidden]-[Title(4)]
			[DateTimeTaken(3)]-[hidden]-[Description(4)]
			[DateTimeTaken(3)]-[hidden]-[Tags(4)]
			[DateTimeTaken(3)]-[hidden]-[Rating(3)]
		}
	}
	package "Xmp file(external)" {
	    ' appendend two blanks
        [Description(4)]-[hidden]-[file.Path   ]
		package "Xmp (A Photo Manager read/write)" {
			[file.Path   ]-[hidden]-[DateTimeTaken(2)]
			[file.Path   ]-[hidden]-[Latitude Longitude(2)]
			[DateTimeTaken(2)]-[hidden]-[Title(1)]
			[DateTimeTaken(2)]-[hidden]-[Description(2)]
			[DateTimeTaken(2)]-[hidden]-[Tags(1)]
			[DateTimeTaken(2)]-[hidden]-[Rating(1)]
		}
	}
@enduml






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
 * All Photo-Properties that are supported by "A Photo Manager".
 * Different implementaions for jpg-exif-file, xmp-file, MediaDB-io, csv-io
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

    /** latitude, in degrees north. (-90 .. +90); longitude, in degrees east.  (-180 .. + 180)    */
    IMetaApi setLatitudeLongitude(Double latitude, Double longitude);
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

    /** real photo-files and database entries have either visiblity PUBLIC or PRIVATE (if it has a tag PRIVATE)  */
    VISIBILITY getVisibility();
    IMetaApi setVisibility(VISIBILITY visibility);

}
