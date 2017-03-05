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


/*

This is sourcecode of uml diagrams using
http://plantuml.com/
notation

pc2android.png
@startuml
    title Workflow from PC to Android
	package "PC" {
        [Irfanview.exe]
        [apmJpg2xmp.cmd]
        [apm2csv.cmd]
    	package "Folder structure with hires Images" {
	    	[JpegFile hires]
    	}
        ' [JpegFile hires] --> [TODO:syncPC]
	}
	package "Android" {
    	package "Folder structure with 1024er Images" {
	    	[JpegFile 1024]
    		[XmpFile]
    		[CsvFile]
    	}

    	package "System" {
	    	[MediaScanner]
    		[MediaDB]
    	}

    	["A Photo Manager" Scanner]
    	["A Photo Manager" Gallery]
        ' [JpegFile 1024] <-- [TODO:syncAndroid]
        ' [XmpFile] <-- [TODO:syncAndroid]
        ' [TODO:syncAndroid] --> [MediaDB]
    }

    [JpegFile hires] -> [Irfanview.exe]
    [Irfanview.exe] -> [JpegFile 1024] : {a}

    [JpegFile hires] -> [apmJpg2xmp.cmd]
    [apmJpg2xmp.cmd] -> [XmpFile] : {b}

    [JpegFile hires] -> [apm2csv.cmd]
    [apm2csv.cmd] -> [CsvFile] : {c}

    [JpegFile 1024] -> [MediaScanner]
    [MediaScanner] -> [MediaDB]

    [JpegFile 1024] -> ["A Photo Manager" Scanner]
    [XmpFile] -> ["A Photo Manager" Scanner]
    [CsvFile] -> ["A Photo Manager" Scanner]
    ["A Photo Manager" Scanner] -> [MediaDB]
    [MediaDB] -> ["A Photo Manager" Gallery] : filter path date tags geo

    ' [TODO:syncAndroid] <-- [TODO:syncPC] : {c}

@enduml

android2pc.png
@startuml
    title Workflow from Android to PC
	package "PC" {
    	package "Folder structure with hires Images" {
	    	[JpegFile hires]
    	}
        [apmXmp2jpg.cmd] --> [JpegFile hires]
        [apm*.cmd] --> [JpegFile hires]
        [TODO:syncPC] --> [JpegFile hires]
	}
	package "Android" {
    	package "Folder structure with 1024er Images" {
	    	[JpegFile 1024]
    		[XmpFile]
    	}

      	package "System" {
    		[MediaDB]
    	}

  	    ["A Photo Manager"] -> [Modfy jpg copy del setGeo ...]

  	    [Modfy jpg copy del setGeo ...] -> [transactionlog]
  	    [transactionlog] --> [TODO:syncAndroid]
  	    [Modfy jpg copy del setGeo ...] --> [aPhotoManager.log.cmd]
        [MediaDB] <- [Modfy jpg copy del setGeo ...]
        [Modfy jpg copy del setGeo ...] -> [JpegFile 1024]
        [Modfy jpg copy del setGeo ...] -> [XmpFile]
    }

    [aPhotoManager.log.cmd] --> [apm*.cmd] : {y}
    [XmpFile] --> [apmXmp2jpg.cmd] : {x}
    [TODO:syncAndroid]-->[TODO:syncPC] : {TODO:z}
@enduml




*/