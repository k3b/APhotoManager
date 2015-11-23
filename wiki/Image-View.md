You can reach the [Image-View](Image-View) via

* [Gallery-View](Gallery-View): taping a photo
* [Gallery-View](Gallery-View) in multi selection mode: long taping a photo
* **android filemanager** or **android gallery** if you open/view/send/send_to an image from there.

## Features:

* image zoom
* swiping for next/previous image
* show extended image info (Exif, IPTC, XMP, ICC)
* file operations copy/move/rename/delete/set-geo
	* Move and Rename is blocked while Media Scanner is active to avoid conflicts/inconsistent data.
	* The **set geo** menu allows you to change the photos geo location (latitude/longitude).
	  * The previous geo-picks for "Set Geo" are shown as blue markers if you select the geo from a map.
	  * Note: You can change size and filename of the history through **Set Geo History XXX** in the [Settings View](settings).
* menu command slideshow: automatic showing next image every 1.5 seconds. Tuching the image stops the slideshow
	* Note: You can change the slide time through **Slide Show Intervall in millisecs** in the [Settings View](settings).
* the actionbar is automatically hidden after a short time if the screen is not touched.
	* Note: You can change this time through **Auto Hide Actionbar after millisecs** in the [Settings View](settings). "0" means do not hide.

## <a name='api'>Intent-API</a> 

* action=VIEW/SEND/SENDTO
* mime="image/*"
* [extra[android.intent.extra.TITLE string]](intentapi#EXTRA_TITLE)
* [extra[de.k3b.extra.FILTER]](intentapi#filter)
	* if there is no filter then swiping left/right will browse to images in the same (sub-)folder as the initial file
* data=file:{/path/path/.../file.ext} or extra[android.intent.extra.STREAM]=file:{/path/path/.../file.ext}
  * example file:/storage/sdcard0/copy/hello.jpg
* data=content:/external/images/media/{id} or extra[android.intent.extra.STREAM]=content:/external/images/media/{id}
  * opens image belonging to {id} in [Image-View](https://github.com/k3b/AndroFotoFinder/wiki/Image-View) 
  * example content:/external/images/media/52173
	* opens image with id=52173
