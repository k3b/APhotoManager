
Other Android apps can use parts of "a Foto Finder" throught the "Intent api".

extra de.k3b.filter format



* open/view/send/sendto an image (jpg/png/gif) from a **android filemanager** will open the [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View)
  * if mimeType="image/*" and uri starts with "file:/"
* send/sendto an image (jpg/png/gif) from **android gallery** will open the [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View)
  * if mimeType="image/*" and uri starts with "content:/external/images/media/"
* view/send/sendto a geo-position in a [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map)
  * if uri starts with "geo:"
* **pick a geo-position** from a [Geografic-Map](https://github.com/k3b/AndroFotoFinder/wiki/geographic-map) or from a photo that contain geo information
  * if input-intent-uri starts with "geo:"
  * returns null (cancel) or intent with geo-uri for the selected posion in the callers onActivityResult()

"a Foto Finder" supports these uri formats

* file:{/path/path/.../file.ext}
  * examle file:/storage/sdcard0/copy/hello.jpg
* content:/external/images/media/{id}
  * examle content:/external/images/media/52173
* geo:{latitude},{longitude}?z={zoomlevel}
  * example geo:53.036258,8.721771?z=11
  * example geo:53,8.7
  * zoomlevel 1..14 is optional. z=3 continent; z=11 streetlevel
  
"a Foto Finder" supports these intent parameter

* de.k2b.extra.FILTER string 
	* format {latitude min},{longitude min};{latitude max},{longitude max};{date min},{date max};{path} 
	* format noGeoInfo;;{date min},{date max};{path} 
	* example 1.23,2.34;3.45,4.56;2001-02-03,2005-12-31;/some/path/
	* example noGeoInfo;;2001-02-03,2005-12-31;/some/path/
		* for fotos without geo infos.
