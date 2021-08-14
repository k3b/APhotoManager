bug: edit existing .apm autoprocessing does not work anymore

input :

* ~/StudioProjects/APhotoManager/app/src/debug/res/values-de/fdroid.xml

potential output

* ~/StudioProjects/APhotoManager/fastlane/metadata/android/de-DE/full_description.txt
*
* ~/StudioProjects/APhotoManager.wiki/home_de.html
* ~/StudioProjects/APhotoManager.wiki/home_de.md

## todo py script pseudo code:

for each ~/StudioProjects/APhotoManager/app/src/debug/res/values-*/fdroid.xml if fdroid.date > fastlane/metadata/android/de-DE/full_description.txt.date or null regenerate

## resources:

* https://github.com/AumitLeon/markdown_html_converter (last update 5 years ago)
**  python script to convert from md to html license MIT
*
* https://github.com/Python-Markdown/markdown (last update 10 days ago)
** with plugin https://github.com/mikitex70/plantuml-markdown to draw plant-uml

## other

currently in use under linux

* using https://github.com/mapitman/mdview under linux
* mdview -bare -o ~/StudioProjects/APhotoManager/fastlane/metadata/android/de-DE/full_description.html ~/StudioProjects/APhotoManager/fastlane/metadata/android/de-DE/full_description.md

