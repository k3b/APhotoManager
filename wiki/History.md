## next version not released yet

## [v0.4.2.*](https://github.com/k3b/AndroFotoFinder/issues?q=milestone%3Av0.4.2) (not released yet)

* [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) : 
	* new menu command slideshow: showing next image every 1.5 seconds.
	* [#10 Faster swiping through high resolution images](https://github.com/k3b/AndroFotoFinder/issues/10)
		* initially the view is loaded with low res image. on first zoom it is reloaded with maximum resolution.
	* Improved scaling of big images
	* if image exists but is not in media database restart media scanner for containing folder
		* automatic removal of duplicate images from media db
* [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) :
	* menu to open [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)
		* purpose: Filter the photos that are visible in the map.
		* if [Geografic-Map](geographic-map) is opened without [intent-extra-de.k3b.extra.FILTER parameter](intentapi#filter) the map is opend with the last used filter.
	* improved menu "zoom to fit"
	* [Intent API](https://github.com/k3b/AndroFotoFinder/wiki/intentapi): support for [de.k3b.extra.FILTER string](intentapi#filter)
* [Folder- or Date-Picker](https://github.com/k3b/AndroFotoFinder/wiki/Folder-Picker):  
	* long-tapping-folder-contextmenu "Show in new Gallery" opens a new prefiltered [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View)
	* [#11](https://github.com/k3b/AndroFotoFinder/issues/11) Folder name in bold and counts in non-bold.
* [Intent API](https://github.com/k3b/AndroFotoFinder/wiki/intentapi): intent VIEW mime="image/*" + data=file:xxx or android.intent.extra.STREAM=file:xxx
	* [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) if xxx is an existing image file. 
	* else [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) where xxx is used as a path filter
* [#16 Media scanner for android below 4.4](https://github.com/k3b/AndroFotoFinder/issues/16)
	* Move and Rename is blocked while Media Scanner is active to avoid conflicts/inconsistent data.
	
## [v0.4.1.150911](https://github.com/k3b/AndroFotoFinder/issues?q=milestone%3Av0.4.1) available in [F-Droid app store](https://f-droid.org) since 2015-09-12

* [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) :
	* [Selection](Gallery-View#Multiselection): add/remove [all visible](Gallery-View#CurrentSet)
	* [#4 change geo: modify photo-s gps-location using a map view](https://github.com/k3b/AndroFotoFinder/issues/4)
* [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) : 
	* [#2 Share/Edit](https://github.com/k3b/AndroFotoFinder/issues/2)/Rename/Delete/Copy/Move
	* Open [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) when [#3 clicking a photo file in a filemanager](https://github.com/k3b/AndroFotoFinder/issues/3) or in android-s "gallery-send menu"
	* [#4 change geo: modify photo-s gps-location using a map view](https://github.com/k3b/AndroFotoFinder/issues/4)
* [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) : 
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
