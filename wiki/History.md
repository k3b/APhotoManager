## next version v0.4.4.? (not released yet)
* *NOT IMPLEMENTED* [#26 option slow-hiqh-quality-detail vs fast-lowRes](https://github.com/k3b/AndroFotoFinder/issues/26): android 5.1: does not support Thumbnails.getThumbnail(...,MediaStore.Images.Thumbnails.FULL_SCREEN_KIND,...) :-(
* TODO [#24 Prevent sleepmode while slideshow is active](https://github.com/k3b/AndroFotoFinder/issues/24)
* [#21 additional languages](https://github.com/k3b/AndroFotoFinder/issues/21)
	* English by k3b and mary-kate
	* German by k3b and v133
	* Japanese by naofum
	* French by Tuuux
	
## [v0.4.3.151201](https://github.com/k3b/AndroFotoFinder/issues?q=milestone%3Av0.4.3.151201)  available in [F-Droid app store](https://f-droid.org)  since 2015-12-01

* [#22 Added "Show in map" action](https://github.com/k3b/AndroFotoFinder/issues/22) to [Gallery-View](Gallery-View) and [Image-View](Image-View) to show the current or selected images in the [Geographic-Map](geographic-map)
* [#23 Set Geo with picker history](https://github.com/k3b/AndroFotoFinder/issues/23)
	* The previous geo-picks for "Set Geo" are shown as blue markers if you select the geo from a map.
* [#17: Image View auto-hide Actionbar](https://github.com/k3b/AndroFotoFinder/issues/17).
* [#20 Add german translations](https://github.com/k3b/AndroFotoFinder/issues/20)
	* Registered app for online crowd translation service [Crowdin Project](https://crowdin.com/project/androFotoFinder)
* [#19 Add Settings dialog](https://github.com/k3b/AndroFotoFinder/issues/19)
    * Added More debug support for 3rd party libs

## [v0.4.2.151105](https://github.com/k3b/AndroFotoFinder/issues?q=milestone%3Av0.4.2.151105) available in [F-Droid app store](https://f-droid.org)  since 2015-11-07

* after app crash the errorlog (logcat) is written to /mnt/sdcard/copy/log/androFotofinder.logcat*.txt
* renamed from "a Foto Finder" to "a Photo Manager" to avoid naming collision with other names.
* added [Bookmark](Bookmarks) support in [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) and [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View).
* [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) :
	* added menu
		* Load [Bookmark](Bookmarks) from ...
		* Details to show current sql 
		* Start Media Scanner
* [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) : 
	* new menu command slideshow: showing next image every 1.5 seconds.
	* [#10 Faster swiping through high resolution images](https://github.com/k3b/AndroFotoFinder/issues/10)
		* initially the view is loaded with low res image. on first zoom it is reloaded with maximum resolution.
	* Improved scaling of big images
	* if image exists but is not in media database restart media scanner for containing folder
		* automatic removal of duplicate images from media db
	* details: added mediaDb-field-values and current sql in addition to the exif-infos
	* Bugfix [#13 Swiping](https://github.com/k3b/AndroFotoFinder/issues/13) : Sometimes the app jumps back
	* Fixed Send via bluetooth 
* [Geographic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) :
	* menu to open [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)
		* purpose: Filter the photos that are visible in the map.
		* if [Geographic-Map](geographic-map) is opened without [intent-extra-de.k3b.extra.FILTER parameter](intentapi#filter) the map is opend with the last used filter.
	* improved menu "zoom to fit"
	* [Intent API](https://github.com/k3b/AndroFotoFinder/wiki/intentapi): support for [de.k3b.extra.FILTER string](intentapi#filter)
	* Bugfix: Map-Contextmenu popup-position at marker.
* [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View) :
	* "Save [Bookmark](Bookmarks) as" and "Load [Bookmark](Bookmarks) from" menu of current filter as query file in /storage/sdcard0/databases/sql/*.query
	* Long-pressing in "Load [Bookmark](Bookmarks) from" opens a contextmenu to edit, rename, delete a bookmarkfile.
* [Folder- or Date-Picker](https://github.com/k3b/AndroFotoFinder/wiki/Folder-Picker):  
	* long-tapping-folder-contextmenu "Show in new Gallery" opens a new prefiltered [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View)
	* [#11](https://github.com/k3b/AndroFotoFinder/issues/11) Folder name in bold and counts in non-bold.
* [Intent API](https://github.com/k3b/AndroFotoFinder/wiki/intentapi): intent VIEW mime="image/*" + data=file:xxx or android.intent.extra.STREAM=file:xxx
    * de.k3b.extra.SQL is now in human readable string format (Same format as [Bookmark](Bookmarks) or .query file.
	* [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) if xxx is an existing image file. 
	* else [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) where xxx is used as a path filter
* [#16 Media scanner for android below 4.4](https://github.com/k3b/AndroFotoFinder/issues/16)
	* Move and Rename is blocked while Media Scanner is active to avoid conflicts/inconsistent data.
* Technical changes
	* Reimplemented database queries with Cursorloader(Manager)
	* improved own media scanner so images can be rescanned after fileoperations in android 4.3 and before
	
## [v0.4.1.150911](https://github.com/k3b/AndroFotoFinder/issues?q=milestone%3Av0.4.1) available in [F-Droid app store](https://f-droid.org) since 2015-09-12

* [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) :
	* [Selection](Gallery-View#Multiselection): add/remove [all visible](Gallery-View#CurrentSet)
	* [#4 set geo: modify photo-s gps-location using a map view](https://github.com/k3b/AndroFotoFinder/issues/4)
* [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) : 
	* [#2 Share/Edit](https://github.com/k3b/AndroFotoFinder/issues/2)/Rename/Delete/Copy/Move
	* Open [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) when [#3 clicking a photo file in a filemanager](https://github.com/k3b/AndroFotoFinder/issues/3) or in android-s "gallery-send menu"
	* [#4 set geo: modify photo-s gps-location using a map view](https://github.com/k3b/AndroFotoFinder/issues/4)
* [Geographic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) : 
	* [#5 Show selected items as blue markers in map](https://github.com/k3b/AndroFotoFinder/issues/5)
	* [#6 Implemented a geo-picker](https://github.com/k3b/AndroFotoFinder/issues/6) that can be used by other apps via the [Intent API](https://github.com/k3b/AndroFotoFinder/wiki/intentapi)
	* current selected photo is visible as red marker
	* long tap on marker opens context menu 
		* to open a new gallery instance filtering the area of the current marker.
		* to zoom to fit the area of the current marker.
* [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View) :
	* New filter option : only photos with no geo infos.
* App can be installed on SD-Card

## [v0.4.0.150816](https://github.com/k3b/AndroFotoFinder/releases/tag/v0.4.0.150816)  available in [F-Droid app store](https://f-droid.org) since 2015-08-18

* [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) : Implemented [#1 Multi selection and share/send image](https://github.com/k3b/AndroFotoFinder/issues/1)
* [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) in multi selection mode : Implemented Delete/Copy/Move/CreateFolder/Write-Log

## [v0.3.4.150728](https://github.com/k3b/AndroFotoFinder/releases/tag/v0.3.4.150728)  available in [F-Droid app store](https://f-droid.org) since 2015-08-01

* Initial public release on FDroid
