FotoGallery\ReadMe.txt"

after delete/move no more multi-sel-mode (if global or delete or move)
	??? funktioniert das auch?
tooltip x items deleted/moved/copied

[![Download from F-Droid](https://camo.githubusercontent.com/7df0eafa4433fa4919a56f87c3d99cf81b68d01c/68747470733a2f2f662d64726f69642e6f72672f77696b692f696d616765732f632f63342f462d44726f69642d627574746f6e5f617661696c61626c652d6f6e2e706e67 "Download from F-Droid")](https://f-droid.org/repository/browse/?fdid=uk.co.ashtonbrsc.android.intentintercept)

todo move/copy logging

onLoadComplete what to do make call overlay.draw() happen? invalidate does not work

AndroFotoFinder
	? own copy/move
		v generic OSDirPicker mit load on demand
			exception when selecting unmounted mnt/extusb on tablet
			todo
				has images to enable ok
				status "select folder with images to enable ok"
				create new subfolder
				OSDirPicker: garbage collector 
					after find???
					after ok/cancel
		AndroidFileCommands.java 
			loging file open/close
			mit optionMenu handling public boolean onOptionsItemSelected(MenuItem item)
			

	
##################

todo
- Global-settings:
   share via content-uri or filepath-uri
   clear selecetion after execute or require explicit cancel

// symlink
mklink /D /J osmdroid-android d:\prj\eve\android\prj\fotos-android.wrk\osmdroid\osmdroid-android

LeakCanary

################################		
		
MapPicker DialogFragment
	own lat-lon-group-by-query

	Background task load ClusterMarker from db
		where lat/lon between visible region
		group by ????

todo

	DirectoryPicker
		dialog: ?? set icon

gui
	Action (menu)
		Navigator-FolderSymbol (Dialogfragment) current path as Album-tree/Date-tree/place-tree/map
		
		Filter (DialogFragment)
			List with current filters (edit/delete)
			add
				Album (DialogFragment directory-tree or Edit->where data/path like %value%)
				Date (DialogFragment date-tree or Edits From...To)
				Place (DialogFragment Map to select area)
				
		SortBy path/date/location/pathlength

Used libs
	https://github.com/chrisbanes/PhotoView

		
"C:\Progs.Portable\sdk\Android\Android Design - Icons 20131120\Action Bar Icons\holo_light\04_collections_collection\drawable-xxhdpi\ic_action_collection.png"		
"C:\Progs.Portable\sdk\Android\Android Design - Icons 20131120\Action Bar Icons\holo_light\01_core_search\drawable-xhdpi\ic_action_search.png"
asc "C:\Progs.Portable\sdk\Android\Android Design - Icons 20131120\Action Bar Icons\holo_light\02_navigation_collapse\drawable-xxhdpi\ic_action_collapse.png"

desc "C:\Progs.Portable\sdk\Android\Android Design - Icons 20131120\Action Bar Icons\holo_light\02_navigation_expand\drawable-xhdpi\ic_action_expand.png"

sort "C:\Progs.Portable\sdk\Android\Android Design - Icons 20131120\Action Bar Icons\holo_light\05_content_import_export\drawable-xxhdpi\ic_action_import_export.png"

place "C:\Progs.Portable\sdk\Android\Android Design - Icons 20131120\Action Bar Icons\holo_light\07_location_place\drawable-xxhdpi\ic_action_place.png"

"C:\Progs.Portable\sdk\Android\Android Design - Icons 20131120\Action Bar Icons\holo_light\07_location_map\drawable-xhdpi\ic_action_map.png"

"D:\prj\eve\android\prj\NichtAktuell\Telegram-master\Telegram-master\TMessagesProj\src\main\res\drawable-hdpi\ic_storage.png"

calender "D:\prj\eve\android\prj\AndroidCalendar.wrk\del\ical-import-export\iCalImportExport\res\drawable-hdpi\icon.png"







- DirectoryBuilder (+sql) mit max(imageID)
- loc-DirectoryBuilder im format 000.00,000.00 null replaced with 400



	Queriable.addResultFilter
		GalleryCursorAdapter.requery  SELECT _id, _data, 0 AS count, longitude
			FROM content://media/external/images/media
			WHERE (_data like ?)
			PARAMETERS /2015/06/15%
			ORDER BY _data, length(_data)

select count(*), strftime('%Y/%m/%d', start_time /1000, 'unixepoch', 'localtime')  
from time_slice group by strftime('%Y/%m/%d', start_time /1000, 'unixepoch', 'localtime') 
order by strftime('%Y/%m/%d', start_time /1000, 'unixepoch', 'localtime') desc

- options: order by (name, date, gps) asc/desc
- options: choose by date/dir/gps

-----------------------------------------


- settings
	debug
	
- gallery-filter
	- foto-adapter
		remember column-id by first access after requery. bind nur if col-id>=0
		laden der thumbnails in thread?
	- folder-gallery 
		- settings pannel (einstellbar optionen)
		- strict (nur direkte kinder where folder=...) sort by path
		- with sub-folder-files (path like xxx%) sort by path length
		- einstellbar item-layout klein (3), mittel(2), gross(1)
		