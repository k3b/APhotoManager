FotoGallery\ReadMe.txt"

mappicker
	load initial data
	on ok => call parent

	async load if zoomlevel changed empty hash and different recycle

	nur ein asyc load gleichzeitig. ggf in on loadFinished nächsten anstossen

	statt zoomutil
			mapview.zoomToBoundingBox(final BoundingBoxE6 boundingBox) {

// symlink
mklink /D /J osmdroid-android d:\prj\eve\android\prj\fotos-android.wrk\osmdroid\osmdroid-android

ImageDetail - 
	Menu: Properties
		path
		lat/lon
		date
		description
        via
		
		
#################################



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
				Place (DialogFragment gps-tree or Edits From...To)
				??Place (DialogFragment Map to select area)
				
		SortBy (popup or combox/spinner) path/date/location/pathlength

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
		