## next version not released yet

## v0.4.2.* (not released yet)

* [Intent API](https://github.com/k3b/AndroFotoFinder/wiki/intentapi): intent VIEW mime="image/*" + data=file:xxx or android.intent.extra.STREAM=file:xxx
	* [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) if xxx is an existing image file. 
	* else [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) where xxx is used as a path filter
	
## [v0.4.1.150910](https://github.com/k3b/AndroFotoFinder/releases/tag/v0.4.1.150910)

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

## [v0.4.0.150816](https://github.com/k3b/AndroFotoFinder/releases/tag/v0.4.0.150816)

* [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) : Implemented [#1 Multi selection and share/send image](https://github.com/k3b/AndroFotoFinder/issues/1)
* [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) in multi selection mode : Implemented Delete/Copy/Move/CreateFolder/Write-Log

## [v0.3.4.150728](https://github.com/k3b/AndroFotoFinder/releases/tag/v0.3.4.150728)

* Initial public release on FDroid
