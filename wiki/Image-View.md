You can reach the [Image-View](Image-View) via

* [Gallery-View](Gallery-View): taping a photo
* [Gallery-View](Gallery-View) in multi selection mode: long taping a photo
* **android filemanager** or **android gallery** if you open/view/send/send_to an image from there.

## Features:

* image zoom
* swiping for next/previous image
* show extended image info (Exif, IPTC, XMP, ICC)
* file operations copy/move/rename/delete/change geo info (Since Version 0.4.1)
	* Move and Rename is blocked while Media Scanner is active to avoid conflicts/inconsistent data.
* menu command slideshow: automatic showing next image every 1.5 seconds. Tuching the image stops the slideshow

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
