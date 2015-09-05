# ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) Pick an area from a [Geographic-Map](Geographic-Map)

Note: The geograpic data in the map come from [openstreetmap servers](http://www.openstreetmap.org) and is cached on the android device. You need an internet connection to download the data.

You can reach the [Geographic-Map](Geographic-Map) via

* the "map symbol" in the [Gallery-View](Gallery-View)
* the "location picker" in the [Filter-View](Filter-View)
* from any app that support [intents with "geo:"- uris](https://github.com/k3b/AndroFotoFinder/wiki/intentapi) (VIEW/SEND/SENDTO/SEND_MULTIPLE/PICK)

## Features:

* The geografic map shows markers ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/app/src/main/res/drawable-mdpi/marker_green.png) at places where photos were taken.
* The numer in the **green marker** indicates how many photos belong to the marker.
* **Blue markers** represent selected photos if [Gallery-View](Gallery-View) is in [multi selection mode](Gallery-View#Multiselection) or if SEND_MULTIPLE.
	* Note: for performance/memory reason only the first 256 blue markers are shown.
* If you tap on a marker the marker becomes **red** to indicate current selection and a photo belonging to the marker is displayed in the lower right corner.
* Tap on the photo to hide it.
* if you **long-tap on a marker** you get a context menu to
	* to open a new gallery instance filtering the area of the current marker.
	* to zoom to fit the area of the current marker.
* If you tap somewhere in the map the "zoomin"- and "zoomout"-buttons become visible to change the map detail level.
* If you double tap somewhere in the map the map zooms in one level.
* If you swipe left/right/up/down you can change the current map area
* The "Zoombar" below the map can be used to change the current zoom level.
* The "Ok" button takes the current visible map area/zoomlevel to update
  * which photos are visible if called from the [Gallery-View](Gallery-View)
  * lat/lon values in the [Filter-View](Filter-View) if called from there
  * return the current position (**red marker**) if called from [external app via "Pick geo:"](https://github.com/k3b/AndroFotoFinder/wiki/intentapi)
* The "Cancel" button or the back button closes the map without affecting the calling activitry.

If you use the [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) as a ["geo:" picker](https://github.com/k3b/AndroFotoFinder/wiki/intentapi) you can

* tap on a green/blue marker to select the geo-location belonging to that marker (the marker becomes **red**)
* tap somewhere in the map where no green/blue marker exists to select a place with no photo.

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/SelectArea.png)
