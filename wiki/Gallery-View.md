The [Gallery-View](Gallery-View) show photos in a scrollable view:

* If you swipe up or down you will see more photos.
* If you tap on a photo it will be opend in the [Image-View](Image-View).
* The Symbol(s) in the actionbar and the menu item(s)
    * ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_unchecked.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_folder.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png)
    * define which photos you want to see (see _current set_ below).

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/Gallery.png)

##Current Set

The [Gallery-View](Gallery-View) show the photos of the device`s _current set_.

The **current set** is either the result of

* SearchFiter ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png) + CurrentFolder ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_folder.png) + Sorter or
* SearchFiter ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png) + CurrentGeoArea ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) + Sorter
* or shows the selected items ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_checked.png) (See Multi Selection below)

The **SearchFiter**
![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png)
is used to find a set of photos by search criteria.

The **CurrentFolder** ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_folder.png) or **CurrentGeoArea** ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) is used to navigate
in the SearchFiter`s resultset.

The **Sorter** determines the order in which the 
result photos are presented in the [Gallery-View](Gallery-View) and 
in the [Image-View](Image-View).

###Navigation

The [Gallery-View](Gallery-View) has these elements:

* via the **SearchFiter** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png) (or menu item "Filter") you can open the [Filter-View](Filter-View) to define the SearchFiter. 
* via the **CurrentFolder** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_folder.png) (or menu item "Select Folder") you can open a [Folder-Picker](Folder-Picker) to pick the CurrentFolder.
* via the **CurrentGeoArea** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) (or menu item "Select Area") you can open a [Geographic-Map](Geographic-Map) to pick the CurrentGeoArea.
* via the menu item "Sort xxx" you see the current sort order. 
    * Example "Sort: Name ^" means "sort by name ascending". "^" means ascending; "v" means descending.
    * If you click on the "Sort xxx" menuitem you get a submenu with the different sort criteria.
    * If you select the same sort criteria again you toggle between ascending and descending.
        * Example: if current sort is "Sort: Name ^" and you select "Name" again the sort will become "Sort: Name v".

###Example Usecase

* The device contains 15000 photos.
* You [search](Filter-View) is "photos taken in 2007" (that photo date is between _2007-01-01_ and _2008-01-01_").
* If you open the [Current folder picker](Folder-Picker) you will see only those folders that contain images taken in 2007.
    * The resultset contains 79 folders with 1166 photos.
* If you open the [Current geo area picker](Geographic-Map) the map will show markers of photos taken in 2007.
    * The resultset contains 584 photos that contain geo-data.
* If you [pick the current Folder](Folder-Picker) "Job" the gallery will show only photos from folder "Job" or subfolders of "Job" from 2007.
    * Photos from other years are not visible - they are filtered out.

## Multiselection Mode (Since Version 0.4)

* If you long-tap on a photo the app enters **multiselection mode:** with additional symbols in the actionbar:
	* ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_cancel.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_unchecked.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_checked.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_share.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_delete.png)
* Taping on any photo toggles selection of that photo. A Selected photo contains a [X] symbol.
* Long-tap on a photo it will be opend in the [Image-View](Image-View).
* Additional symbols in the Actionbar (or overflow menu):
    * taping on the cancel symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_cancel.png) exits the multiselection mode.
    * taping on the **selected only** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_unchecked.png) will show only the seleceted photos. The symbol changes to ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_checked.png) 
	* taping on the **selected only** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_checked.png) again will show all photots. The symbol changes to ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_unchecked.png).
	* taping on the **delete** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_delete.png) or the "Delete" menu will delete the selected photos.
    * you can send/share the selected photos via the share symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_share.png).
        * if you have installed [gelin's sendtosd](https://f-droid.org/wiki/page/ru.gelin.android.sendtosd) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_send2sd.png): Copy or move photos to a different folder on the sdcard.
        * if you have installed [k3b's ToGoZip](https://f-droid.org/wiki/page/de.k3b.android.toGoZip) ![](https://raw.githubusercontent.com/k3b/ToGoZip/master/app/src/main/res/drawable-mdpi/ic_launcher.png): Add the photo to a zip file
        * other options are: send photo(s) via bluetooth or mail

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/Gallery-MultiSelection.png)
