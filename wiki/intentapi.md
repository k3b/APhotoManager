Other Android apps can use parts of "a Photo Manager" throught the "Intent api".

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
  
"a Photo Manager" supports these uri formats for data={xxx} or extra[android.intent.extra.STREAM]={xxx}

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
 
"a Photo Manager" supports these optional intent parameters

### <a name='EXTRA_TITLE'>android.intent.extra.TITLE string</a>

* purpose define the title of the gui

### <a name='filter'>de.k3b.extra.FILTER string</a>

* purpose define which items should be visible
	* see [Gallery Navigation](Gallery-View#Navigation) for details.
* the content is the same as in [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)
* format {latitude min},{longitude min};{latitude max},{longitude max};{date min},{date max};{path} 
* format noGeoInfo;;{date min},{date max};{path} 
* example 1.23,2.34;3.45,4.56;2001-02-03,2005-12-31;/some/path/
* example noGeoInfo;;2001-02-03,2005-12-31;/some/path/
	* **noGeoInfo** is used to show only those photos that have no geo infos.
* technically this filter defines the **sql-where** part without ordering or colums formulas.

### <a name='EXTRA_SQL'>de.k3b.extra.SQL string</a>

* purpose define current sql that defines which photos are affected.
	* used when opening a sub-[gallery view](Gallery-View)
	* used when opening a [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) 
	* used when navigating to [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) so that the image view show the same images and in the same order as the originating Gallery-View
* format: the same as [Internal sql format of .query files](intentapi#sql) generated in the [bookmark menues](Filter-View#Bookmarks) of [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View).
* technically this defines a complete sql including sql-where, ordering and colums formulas. 
	
### <a name='OFFSET'>OFFSET</a>

* de.k3b.extra.OFFSET offset in sql that should be initally visible in [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View).
	* Example: OFFSET=5 in the gallery view will open the 5th image in the [sql-query](intentapi#EXTRA_SQL)

### <a name='SelectedItems'>de.k3b.extra.SELECTED_ITEMS string</a>

* purpose define which items are currently selcted if [gallery is in multi selection mode](Gallery-View#Multiselection)
* used in [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) to define the "blue selection marker"
* format {id},{id},{id},{id},{id},...

## <a name='sql'>internal sql format of .query files (since version 0.4.2)</a>

* purpose defintion of Bookmark files in folder /storage/sdcard0/databases/sql/*.query
* created in [Filter-View](Filter-View) via menu "Save Bookmark as ..."
* loaded into [Filter-View](Filter-View) via menu "Load Bookmark from ..."
* loaded into [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) via menu "Load Bookmark from ..."

".query files" can be used to create summery queries with cont, group by, having....

Note: The same format is also used in the [intent api for extra parameter de.k3b.extra.SQL](intentapi#EXTRA_SQL)

Format: 

* Lines, seperated by newline character. Each line is either 
	* a comment starting with '#', '//' or '--'
	* a keyword: one of FROM, QUERY-TYPE-ID, SELECT, WHERE, WHERE-PARAMETERS, GROUP-BY, HAVING, HAVING-PARAMETERS, ORDER-BY
		* the keyword is similar to sql 
			* select [SELECT] from [FROM] where [WHERE+WHERE-PARAMETERS] group by [GROUP-BY] having [HAVING+HAVING-PARAMETERS] order by [ORDER-BY]
	* a value, that belongs to the keyword. 

See also
	
* [MediaStore Images colums](http://developer.android.com/reference/android/provider/MediaStore.Images.ImageColumns.html)	
* [SqLite core functions](https://www.sqlite.org/lang_corefunc.html) 

### Examples

#### simple filter "Path"

Purpose: show only images that contain "kreta" in the full file path

<pre>
	WHERE 
		_data like ?
		WHERE-PARAMETERS 
			%kreta%
</pre>

This is the same as setting "Path" in [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)

The above query is the same as 

<pre>
FROM 
	# where data comes from: media gallery images
	content://media/external/images/media
	QUERY-TYPE-ID
		# 11 gallery; # 13 album. See FotoSql.java QUERY_TYPE_xxx 
		11
SELECT 
	# NOTE: there are no commas between the colum-definitions.
	# _id ist the image id
	_id
	
	# disp_txt is the photo title
	_data AS disp_txt
	
	# count is the number of photos represented by one item. Displayed as (xxx) in the title if > 1
	0 AS count
	
	# longitude is an indicator that item has geo data. If not null displayed as "#" to indicate geo data. 
	longitude
	
WHERE 
	_data like ?
	WHERE-PARAMETERS 
		%kreta%
	# meaning "where _data like '%kreta%'
	
</pre>

#### complex filter

Purpose: show only images that 

* has geolocation (34.691944,23.480529) .. (36.071302,24.732971)
* photo was taken between 2013-01-01 and 2014-01-01
* contain "kreta" in the full file path

<pre>
WHERE 
	# NOTE: there are no "and2-S between the colum-definitions.
	latitude >= ?
	latitude < ?
	longitude >= ?
	longitude < ?
	datetaken >= ?
	datetaken < ?
	_data like ?
	WHERE-PARAMETERS 
		34.691944
		36.071302
		23.480529
		24.732971
		2013-01-01
		2014-01-01
		%kreta%
</pre>

This is the same as setting all fields in [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)

Note: column "datetaken" value can be millisecs since 1970 or iso-date in format yyyy-mm-dd

Note: the simple sql parser can translate this to a filter only if WHERE parameter-elements are exactly written like the example above.
If you put additional blanks or change the casing the translation from query to filter does not work.

Example
 
* "latitude >= ?" will be translated to filter item lat-min
* "latitude  >= ?" will not be translated to any filter item because there is an additional blank
* "Latitude >= ?" will not be translated to any filter item because there is a capital "L" instead of "l"

#### non standard filter

Purpose: show only panorama pictures

<pre>
Where 
  ((1.0*width/height<0.3) or (1.0*width/height>3))
</pre>
 
This cannot be done in the [Filter-View](https://github.com/k3b/AndroFotoFinder/wiki/Filter-View)

#### advanced complex grouping-drill-down-query 

a query can be as complex as this:

<pre>
FROM 
	content://media/external/images/media
	QUERY-TYPE-ID
		13
SELECT 
	max(_id) AS _id
	
	# folder name calculated from full-path "_data" and filename "_display_Name" to be displayed
	Substr(_data,1, length(_data) - length(_display_Name)) AS disp_txt
	
	# if folder contain more than 1 item the count will be appended as (...) to the name
	count(*) AS count
	
	# folder name calculated from full-path "_data" and filename "_display_Name" to be displayed	
	Substr(_data,1, length(_data) - length(_display_Name)) AS _display_Name
	
	# not null if at least one photo in folder has geo info
	max(longitude) AS longitude
	
	# "where_param" is used to drill down i.e. clicking on a folder in the gallery view will open a child  gallery view with the folder contant.
	'Substr(_data,1, length(_data) - length(_display_Name)) = ''' || Substr(_data,1, length(_data) -  length(_display_Name))  || '''' AS where_param
GROUP-BY 
	# group by folder name
	Substr(_data,1, length(_data) - length(_display_Name))
ORDER-BY 
    Substr(_data,1, length(_data) - length(_display_Name))
</pre>
	
note: you have to load this from the [Gallery-View](https://github.com/k3b/AndroFotoFinder/wiki/Gallery-View) to make it work.

