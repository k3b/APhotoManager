The [Filter-View](Filter-View) is used to find a set of photos by search criteria. 

You can reach the [Filter-View](Filter-View) via

* tapping the filter symbol or filter menu in the [Gallery-View](Gallery-View)
* tapping the filter symbol or filter menu in the [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map)

## You can search by zero, one ore more criteria:

* **Path** name: a combination of folders and photo file name
	* you can [pick a folder](Folder-Picker) with the "..." button.
	* you can enter a search pattern using the **%** wildcard.
	* Example: searching for path "%kreta%" include these files
		* /mnt/extsd/media/photos/Urlaub/0709Kreta/Ausfluee/AgiosNicolaus/07Kr043.jpg
		* /mnt/extsd/media/photos/Maps/Kreta.jpg
* **Date** when the photo was taken. The first field is **from-date** the second field is **to-date** 
	* you can pick a date range through the **Date picker** with the "..." button. 
	* you can enter a start-date and/or a end-date in the format yyyy-mm-dd .
	* Example: 
		* searching for date "2007-01-01" and "" will show photos taken since 2007.
		* searching for date "" and "2008-01-01" will show photos taken before jan 1st 2008.
		* searching for date "2007-01-01" and "2008-01-01" will show photos taken in 2007.
* [ ] With no geo info: When checked 
	* only photos without geo-infos are visible
	* input fields for latitude/longitude are hidden.
* **Geo Area** where the photo was taken as Lat(itude north) and Lon(gitude east).  The first field is **from** the second field is **to** .
	* you can pick a geo area through the [Geographic-Map](Geographic-Map) with the "..." button. 
	* you can enter a start and/or a end valus in decimal degrees format.
	* negagive values indicate latitude south or longitude "west".
* The "Ok" button takes the current filter and updates the caller:
  * [Gallery-View](Gallery-View): to update which photos are visible.
* The "Cancel" button or the back button closes the picker without affecting [Gallery-View](Gallery-View).
* The "Clear Filter" sets all filter fields to empty which means "no filter".

Each field in the [Filter-View](Filter-View) corresponds to a field of [EXTRA de.k3b.extra.FILTER](intentapi#filter) of the [Intent API](https://github.com/k3b/AndroFotoFinder/wiki/intentapi).
