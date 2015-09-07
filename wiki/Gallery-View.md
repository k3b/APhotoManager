You can reach the [Gallery-View](Gallery-View) via

* opening from android-s app manager
* the [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map)'s contextmenu by long tapping a marker.

The [Gallery-View](Gallery-View) show photos in a scrollable view:

* If you swipe up or down you will see more photos.
* If you tap on a photo it will be opend in the [Image-View](Image-View).
* If you long-tap on a photo the app enters [multi selection mode](Gallery-View#Multiselection)
* The Symbol(s) in the actionbar and the menu item(s)
    * ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_unchecked.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_folder.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png)
    * define which photos you want to see (see [Current Visible Photos](Gallery-View#CurrentSet) below).

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/Gallery.png)


## <a name='CurrentSet'>Current Visible Photos</a>

The [Gallery-View](Gallery-View) show the photos of the device which are _current visible_.

This visibility is either the result of

* SearchFiter ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png) + CurrentFolder ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_folder.png) + Sorter or
* SearchFiter ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png) + CurrentGeoArea ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) + Sorter
* or shows the selected items ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_checked.png) (See [Multi Selection](Gallery-View#Multiselection) below)

The **[Search Fiter](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)**
![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png)
is used to find a set of photos by search criteria.

The **CurrentFolder** ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_folder.png) or **CurrentGeoArea** ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) 
is used to navigate in the SearchFiter`s resultset.

The **Sorter** determines the order in which the 
result photos are presented in the [Gallery-View](Gallery-View) and 
in the [Image-View](Image-View).

### <a name='Navigation'>Navigation</a>

The [Gallery-View](Gallery-View) has these elements:

* via the **SearchFiter** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_filter.png) (or menu item "Filter") you can open the [Filter-View](Filter-View) to define the SearchFiter. 
* via the **CurrentFolder** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_folder.png) (or menu item "Folder Filter") you can open a [Folder-Picker](Folder-Picker) to pick the CurrentFolder.
* via the **CurrentGeoArea** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) (or menu item "Map Area Filter") you can open a [Geographic-Map](Geographic-Map) to pick the CurrentGeoArea.
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

## <a name='Multiselection'>Multiselection Mode</a>

* If you long-tap on a photo the app enters **[multi selection mode](Gallery-View#Multiselection):** with different menu items and symbols in the actionbar:
	* ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_cancel.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_unchecked.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_checked.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_share.png) ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_delete.png)
* Taping on any photo toggles selection of that photo. A Selected photo contains a [X] symbol.
* Long-tap on a photo it will open it in the [Image-View](Image-View).
* If you open the [Geographic-Map](Geographic-Map) in [multi selection mode](Gallery-View#Multiselection) selected photos will be shown as blue markers.
* Additional symbols in the Actionbar (or overflow menu):
    * taping on the cancel symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_cancel.png) exits the [multi selection mode](Gallery-View#Multiselection).
    * taping on the **selected only** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_unchecked.png) will show only the seleceted photos. The symbol changes to ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_checked.png) 
	* taping on the **selected only** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_checked.png) again will show all photots. The symbol changes to ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_unchecked.png).
    * taping on the **Selection: add all visible** will select all [Current Visible Photos](Gallery-View#CurrentSet). (Since Version 0.4.1)
    * taping on the **Selection: remove all visible** will unselect all [Current Visible Photos](Gallery-View#CurrentSet). (Since Version 0.4.1)
	* taping on the **delete** symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_delete.png) or the "Delete" menu will delete the selected photo(s).
	* the **move/copy** menu will move/copy the selected photo(s) to a different folder.
	    * Pressing the "..." button or long pressing a folder in the [foder picker](https://github.com/k3b/AndroFotoFinder/wiki/Folder-Picker) allows you to create a new folder.
	* the **Edit geo** menu allow you to change the photos geo location (latitude/longitude).  (Since Version 0.4.1)
    * you can send/share the selected photos via the share symbol ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_share.png).
        * if you have installed [k3b's ToGoZip](https://f-droid.org/wiki/page/de.k3b.android.toGoZip) ![](https://raw.githubusercontent.com/k3b/ToGoZip/master/app/src/main/res/drawable-mdpi/ic_launcher.png): Add the photo to a zip file
        * other options are: send photo(s) via bluetooth or mail

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/Gallery-MultiSelection.png)

The [Intent API](https://github.com/k3b/AndroFotoFinder/wiki/intentapi) for [Gallery-View](Gallery-View)  support

* [de.k3b.extra.FILTER string](intentapi#filter) (optional)
