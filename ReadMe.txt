todo

	Queriable.addResultFilter
		GalleryCursorAdapter.requery  SELECT _id, _data, 0 AS count, longitude
			FROM content://media/external/images/media
			WHERE (_data like ?)
			PARAMETERS /2015/06/15%
			ORDER BY _data, length(_data)

select count(*), strftime('%Y/%m/%d', start_time /1000, 'unixepoch', 'localtime')  
from time_slice group by strftime('%Y/%m/%d', start_time /1000, 'unixepoch', 'localtime') 
order by strftime('%Y/%m/%d', start_time /1000, 'unixepoch', 'localtime') desc

- DirList Fragment
	dirTree
		layout icons child indent

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
		
	- DirList-Textuelle Folder-Sicht
		todo lib unittest
		directories als testdaten ausgeben
			/mnt/.../media
			/mnt/.../media/photos/0206Greve/Blockland
			/mnt/.../media/photos/0206Greve/Aumund
	- foto-gallery
	- foto-detail
	
	