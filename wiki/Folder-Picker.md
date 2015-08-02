# ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) [Folder-Picker](Folder-Picker)

You can reach the [Folder-Picker](Folder-Picker) via

* the "folder symbol" ![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/s_map.png) in the [Gallery-View](Gallery-View)
* the "folder picker" in the [Filter-View](Filter-View)

In the top area is the **"path bar"** : a list of button where each button represent a folder in the current path.

Below you find a **list** of folders and sub-folders.

In the button area you have buttons **OK**, **Cancel** and a **preview image**

![](https://raw.githubusercontent.com/k3b/AndroFotoFinder/master/wiki/png/FolderPicker.png)

## Features:

* The numers in the names indicates how many folders/photos belong to the item. Example _"AgiosNicolaus (2):(33+15)"_ has the following meaning:
  * "(2)" The folder "AgiosNicolaus" has 2 subfolders with images
  * ":(33+15)" The folder "AgiosNicolaus" contains 33 photos and subfolders of it contain +15 photos.
* If you tap on a (sub)folder in the **list** or a button in the **path bar** 
  * the **current folder path** is updated
  * the folder picker navigates to that path 
  * and a photo belonging to the folder is displayed in the lower right corner.
* The "Ok" button takes the current folder-path and updates the caller:
  * [Gallery-View](Gallery-View): to update which photos are visible.
  * [Filter-View](Filter-View): set the path value.
* The "Cancel" button or the back button closes the picker without affecting [Gallery-View](Gallery-View) or [Filter-View](Filter-View)
* You can swipe the **path bar** to left/right to scroll to path-buttons on the right or left.
