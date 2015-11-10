The [Settings View](settings) is used to configure the behaviour of "a Photo Manager". 

You can reach the [Settings View](settings) via the **settings** menu of 

* [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View)
* [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map)
* [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)

"a Photo Manager" has these settings

##settings

* **Clear Multi-Selelection**: If checked [multi selection mode](Gallery-View#Multiselection) in [Gallery-View](Gallery-View) is canceled after a command from Actionbar or Menu
* **Max. Sel-Markers in Map**: The maximum number of **Blue selection markers** in the [Geografic-Map](geographic-map).
	* Note: This value is limited for performance/memory reason.
* **Slide Show Intervall in millisecs**: defines the [Image-View's](Image-View) timing of menu command **slideshow**
* **Report Folder** defines the filesystem's directory where [Bookmark files](Bookmarks) are stored and loaded from.

##Diagnostics

The Diagnostics group defines settings to support finding errors.

A Photo Manager uses the android's [logcat system](http://developer.android.com/tools/help/logcat.html) to store debug output.
When A Photo Manager crashes the content of the logcat is saved to a file for later analysis.

There are these diagnostics settings:

* **Error Log Folder**: defines the filesystem's directory where crash reports are written to.
* **LogCat SQL**: Logs Database activity.
* **LogCat Grid/Listview**: Logs Grid View Items of Gallery and other lists.
* **LogCat Memory Consumption**: Logs Memory Consumption.
* **LogCat Misc.**: Other Logs
