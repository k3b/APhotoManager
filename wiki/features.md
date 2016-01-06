# A Photo Manager ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/app/src/main/res/drawable-hdpi/foto_gallery.png), A Photo Map, AndroFotoFinder

## Features

**Help wanted for further [Translations](https://crowdin.com/project/androFotoFinder)**

* The only enhanced android [Gallery app](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) for local images on [f-droid](https://f-droid.org/)
* Shows photos in a [geographic map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) from [openstreetmap](http://www.openstreetmap.org)
* Buildin file manager for photos: find, sort, view, copy, delete, Show in map, set gps exif data, send, ... .
* Fast find local photos by (sub-)folder, date and/or geographic map.
* [Translations](https://crowdin.com/project/androFotoFinder): English, German(since 0.4.3)

* Can handle big image collections (15000+ images in 1000+ folders)
* Uses Android's image content-provider. No initial image scan neccessary.
* The [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View) can find images via
    * folder (with hierarchical [Folder-Picker](https://github.com/k3b/AndroFotoFinder/wiki/Folder-Picker)). 
		* Non-relevant folders are hidden from folder picker (i.e. they have no images inside) .
    * wildcards that match folder/filename
    * date
    * area in [geographic map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) (if image has GPS Exif info)
	* Searchcriteria can be saved to and later be retrieved from [Bookmarks](Bookmarks) (since 0.4.2).
* Show results in a scrollable [gallery view](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) with 
	* support for multiple photo selection
	* file operations copy, move, delete, share, show in map, set geo
* [Image view](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) features
    * image zoom
    * swiping for next/previous image
    * show extended image info (Exif, IPTC, XMP, ICC)
	* file operations copy, move, delete, rename, share, show in map, set geo
	* can be opened from any filemanager or android-s gallery 
* The [geographic map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) 
    * shows markers at places where photos were taken.
	* can be opened as a map-viewer from any app that support "geo:"-uri-s 
	* can be opened as a "geo:" picker to select a location from a map or photo 
* File operations 
	* will also update Android's image content-provider-database
	* update xmp-sidecar-files
	* preserve file date if local android supports it.

### Screenshots

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/Gallery.png)

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/Gallery-MultiSelection.png)

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/SelectArea.png)

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/FolderPicker.png)

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/Filter.png) 
 