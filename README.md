# ![](https://raw.githubusercontent.com/k3b/APhotoManager/master/app/src/main/res/drawable-hdpi/foto_gallery.png) "A Photo Manager" with "A Photo Map", AndroFotoFinder

Enhanced, privacy aware Android **[Gallery App](https://github.com/k3b/APhotoManager/wiki/Gallery-View)** to manage local photos: 

* Free, opensource, no adds, no user tracking, available on [f-droid](https://f-droid.org/)
* [Translations](https://crowdin.com/project/AndroFotoFinder): &nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/ar-home">ar</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/de-home">de</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/home"   >en</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/it-home">it</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/fr-home">fr</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/ja-home">ja</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/nl-Home">nl</a>,&nbsp;pl,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/pt-home">pt</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/ro-home">ro</a>,&nbsp;ru&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/tr-home">tr</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/uk-home">uk</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/zh-CN-home">zh-CN</a>,&nbsp;<a href="https://github.com/k3b/APhotoManager/wiki/zh-TW-home">zh-TW</a>
	* [![Crowdin](https://d322cqt584bo4o.cloudfront.net/androFotoFinder/localized.svg)](https://crowdin.com/project/androFotoFinder)<a href="https://github.com/k3b/APhotoManager/issues/21">Help us to translate into other languages.</a>
	
## Highlights:

* [geotagging with map](https://github.com/k3b/APhotoManager/wiki/example-geosearch), 
* [Virtual Albums](https://github.com/k3b/APhotoManager/wiki/Bookmarks) filesystem-folder indepenant,
* Filemanagement: copy, move, rename, delete, send, [Incremental Photo Backup](https://github.com/k3b/APhotoManager/wiki/Backup-to-zip), ... .
* [Edit exif metadata](https://github.com/k3b/AndroFotoFinder/wiki/Exif-Edit): date, title, description, [tags(keywords)](https://github.com/k3b/APhotoManager/wiki/Tags), geo, rating, ....
* management:  on-move automatically [rename photo-files](https://github.com/k3b/AndroFotoFinder/wiki/AutoProcessing) and/or [add photo properties(exif)](https://github.com/k3b/AndroFotoFinder/wiki/AutoProcessing),
* Can handle big image collections (20000+ images in 1000+ folders).
* Small memory footprint (1.5 MB), Works on old low-memory android 4.0 devices
* available in [many languages](https://github.com/k3b/APhotoManager/issues/21)
* Extended photo media scanner for Exif, IPTC, XMP

## Privacy:

* can [hide photos](https://github.com/k3b/APhotoManager/wiki/Exif-Edit#Visibility) from other gallery-apps/image-pickers.
* [Vault mode](https://github.com/k3b/APhotoManager/wiki/AppPinning): If enabled unwanted photos cannot be seen.
* photos are kept on local device. No upload to third party.
* no adds, no usertracking, free open source, available on f-droid

## Required Android Permissions:

* INTERNET: to download map data from Open Streetmap Server
* ACCESS_NETWORK_STATE and ACCESS_WIFI_STATE: to find out if wifi/internet is online to start downloaded geodata
* WRITE_EXTERNAL_STORAGE to cache downloaded map data in local file system and to do file operations with the photos
* READ_LOGS to read and save potential crash-logs to a textfile.

---<br/>
[<img src="https://github.com/k3b/APhotoManager/wiki/uptodown.png" alt="available on upToDown store" height="82" width="324">](https://a-photo-manager.en.uptodown.com/android) [<img src="https://github.com/k3b/APhotoManager/wiki/fdroid.png" alt="available on F-Droid app store" height="82" width="324">](https://f-droid.org/app/de.k3b.android.androFotoFinder)<br/>
[Downloads](https://github.com/k3b/APhotoManager/wiki/Download)<br/>
**[DISCLAIMER Be cautious if you download "A Photo Manager" where the apk size is bigger than 1.5 Megabytes](https://github.com/k3b/APhotoManager/wiki/Download)**:<br/>
---<br/>

## Features

[... features more detailed](https://github.com/k3b/APhotoManager/wiki/features)

**Help wanted for further [Translations](https://crowdin.com/project/AndroFotoFinder)**

## Current Project Status

* Current release
  * <img src="https://img.shields.io/github/release/k3b/APhotoManager.svg?maxAge=3600" />
  * [<img src="https://github.com/k3b/APhotoManager/wiki/uptodown.png" alt="available on upToDown store" height="82" width="324">](https://a-photo-manager.en.uptodown.com/android) [<img src="https://github.com/k3b/APhotoManager/wiki/fdroid.png" alt="available on F-Droid app store" height="82" width="324">](https://f-droid.org/app/de.k3b.android.androFotoFinder) 
  * [<img src="https://img.shields.io/github/license/k3b/APhotoManager.svg"></img>](https://github.com/k3b/APhotoManager/blob/master/LICENSE) or later.
  * Code Quality [![Codacy Badge](https://api.codacy.com/project/badge/Grade/df65509fc428454791603de5f3bb7707)](https://www.codacy.com/app/klaus3b-github/APhotoManager?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=k3b/APhotoManager&amp;utm_campaign=Badge_Grade)
  * Source code [branch FDroid <img src="https://travis-ci.org/k3b/APhotoManager.svg?branch=FDroid" alt="link to buldserver"  />](https://travis-ci.org/k3b/APhotoManager)
  * Recent changes see [History](https://github.com/k3b/APhotoManager/wiki/History)  
* Current development version
  * Source code [branch master <img src="https://travis-ci.org/k3b/APhotoManager.svg?branch=master" alt="link to branches on buldserver" />](https://travis-ci.org/k3b/APhotoManager/branches)
  * Translations [![Crowdin](https://d322cqt584bo4o.cloudfront.net/androFotoFinder/localized.svg)](https://crowdin.com/project/androFotoFinder)<a href="https://github.com/k3b/APhotoManager/issues/21">Help us to translate into other languages.</a>

## Table of Contents

* [Overview](https://github.com/k3b/APhotoManager/wiki/features)
* [Download](https://github.com/k3b/APhotoManager/wiki/Download)
* [Walk Through: Gallery, Filter, Map, Folder picker](https://github.com/k3b/APhotoManager/wiki/example-geosearch)
* [Gallery-View](https://github.com/k3b/APhotoManager/wiki/Gallery-View)
* [Geographic-Map](https://github.com/k3b/APhotoManager/wiki/geographic-map)
* [Image-View](https://github.com/k3b/APhotoManager/wiki/Image-View)
* [Edit Exif infos](Exif-Edit) of photo(s)
* [Filter-View](https://github.com/k3b/APhotoManager/wiki/Filter-View)
* [tags (keywords)](https://github.com/k3b/APhotoManager/wiki/Tags)
* [Virtual Albums](https://github.com/k3b/APhotoManager/wiki/Bookmarks)
* [Incremental Photo Backup](https://github.com/k3b/APhotoManager/wiki/Backup-to-zip)
* [AutoProcessing](https://github.com/k3b/AndroFotoFinder/wiki/AutoProcessing)
* [Folder-Picker](https://github.com/k3b/APhotoManager/wiki/Folder-Picker)
* [Settings](https://github.com/k3b/APhotoManager/wiki/settings)
* [Intent API](https://github.com/k3b/APhotoManager/wiki/intentapi)


![](https://raw.githubusercontent.com/k3b/APhotoManager/master/wiki/png/SelectArea.png)

![](https://raw.githubusercontent.com/k3b/APhotoManager/master/wiki/png/Gallery.png)

![](https://raw.githubusercontent.com/k3b/APhotoManager/master/wiki/png/FolderPicker.png)


## Contributing and Bugreports

Feedback, translation and contributions are welcomed. 

* [Discuss pro and cons, features, featurerequests and to provide news and support about the app on reddit.](https://www.reddit.com/r/APhotoManager/)
* [Issue tracker](https://github.com/k3b/APhotoManager/issues)
* You can help to translate this app via https://crowdin.com/project/AndroFotoFinder
-----

## Donations: 

If you like this app please consider to donating to https://wiki.openstreetmap.org/wiki/Donations .

Since android-developping is a hobby (and an education tool) i donot want any 
money for my apps so donation should go to projects i benefit from.
