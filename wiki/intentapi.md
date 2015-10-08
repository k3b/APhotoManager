Other Android apps can use parts of "a Foto Finder" throught the "Intent api".

* open/view/send/sendto an image (jpg/png/gif) from a **android filemanager** will open the [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View)
  * if mimeType="image/*" and uri starts with "file:/"
* send/sendto an image (jpg/png/gif) from **android gallery** will open the [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View)
  * if mimeType="image/*" and uri starts with "content:/external/images/media/"
* view/send/sendto a geo-position in a [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map)
  * if uri starts with "geo:"
* **pick a geo-position** from a [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) or from a photo that contain geo information
  * if input-intent-uri starts with "geo:"
  * returns null (cancel) or intent with geo-uri for the selected posion in the callers onActivityResult()

## <a name='uri'>Uri formats</a>
  
"a Foto Finder" supports these uri formats for data={xxx} or extra[android.intent.extra.STREAM]={xxx}

* file:{/path/path/.../file.ext}
  * opens in [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) if mime="image/*" for action=VIEW/SEND/SENDTO
  * examle file:/storage/sdcard0/copy/hello.jpg
* file:{xxx}
  * opens [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) with pathfilter={xxx} if mime="image/*" for action=VIEW/SEND/SENDTO if xxx is not an image file
  * example file:*kreta*
	* opens [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) showing all files that contain "kreta" in its full path
* content:/external/images/media/{id}
  * opens image belonging to {id} in [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) for action=VIEW/SEND/SENDTO/SEND_MULTIBLE
  * example content:/external/images/media/52173
	* opens image #52173. If there is no extra[de.k3b.extra.FILTER] swiping left/right will browse to images in the same (sub-)folder as #52173
* <a name='uri-geo'>geo:{latitude},{longitude}?z={zoomlevel}</a>
  * opens [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) at {latitude} {longitude} {zoomlevel}
  * example geo:53.036258,8.721771?z=11
  * example geo:53,8.7
  * zoomlevel 1..14 is optional. z=3 continent; z=11 streetlevel
  
## <a name='extra'>Intent-Extra parameter</a> 
 
"a Foto Finder" supports these intent parameter

### <a name='filter'>de.k3b.extra.FILTER string</a>

* purpose define which items should be visible
	* see [Gallery Navigation](Gallery-View#Navigation) for details.
* the content is the same as in [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)
* format {latitude min},{longitude min};{latitude max},{longitude max};{date min},{date max};{path} 
* format noGeoInfo;;{date min},{date max};{path} 
* example 1.23,2.34;3.45,4.56;2001-02-03,2005-12-31;/some/path/
* example noGeoInfo;;2001-02-03,2005-12-31;/some/path/
	* **noGeoInfo** is used to show only those photos that have no geo infos.

### <a name='SelectedItems'>de.k3b.extra.SELECTED_ITEMS string</a>

* purpose define which items are currently selcted if [gallery is in multi selection mode](Gallery-View#Multiselection)
* used in [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) to define the "blue selection marker"
* format {id},{id},{id},{id},{id},...

### <a name='EXTRA_TITLE'>android.intent.extra.TITLE string</a>

* purpose define the title of the gui

### <a name='EXTRA_SQL'>de.k3b.extra.SQL string</a>

* purpose current sql from [Gallery-View](Gallery-View) to [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View)
	* so that the image view show the same images and in the same order as the Gallery-View
* format: the same as [bookmark files *.query](intentapi#sql)
	
### Internal extras, not for public use

* de.k3b.extra.OFFSET offset in sql that should be initally visible in image view.
	* Example: opening the 5th image in the gallery view will open the 5th image in the sql-query
	
## <a name='sql'>internal sql file format *.query (since version 0.4.2)</a>

* purpose defintion of Bookmarks in folder /storage/sdcard0/databases/sql/*.query
* created in [Filter-View](Filter-View) via menu "Save Bookmark as ..."
* loaded into [Filter-View](Filter-View) via menu "Load Bookmark from ..."

Note: The same format is also used in the [intent api for extra parameter de.k3b.extra.SQL](intentapi#EXTRA_SQL)

Format: 

* Lines, seperated by newline character. Each line is either 
	* a comment starting with '#', '//' or '--'
	* a keyword: one of FROM, QUERY-TYPE-ID, SELECT, WHERE, WHERE-PARAMETERS, GROUP-BY, HAVING, HAVING-PARAMETERS, ORDER-BY
		* the keyword is similar to sql 
			* select [SELECT] from [FROM] where [WHERE+WHERE-PARAMETERS] group by [GROUP-BY] having [HAVING+HAVING-PARAMETERS] order by [ORDER-BY]
	* a value, that belongs to the keyword.
