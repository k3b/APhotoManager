todo

- DirList Fragment
	pathbar
		text = relpath + childcount
		on click last => select
	dirTree
		layout icons child indent

- DirList Fragment
	 ExpandableListView http://developer.android.com/reference/android/app/ExpandableListActivity.html
	 	or https://github.com/Polidea/tree-view-list-android
	 example http://stackoverflow.com/questions/10046336/android-expandablelistview-implements-filterable
	 example see http://stackoverflow.com/questions/8906471/how-to-display-list-of-folders-and-files-using-androids-expandable-listview

		public class customListAdapter extends BaseExpandableListAdapter {

			private File folder1;
			private File folder2;

			private String[] groups = {};
			private String[][] children = {};

			public customListAdapter() {
				// Sample data set.  children[i] contains the children (String[]) for groups[i].
				folder1 = new File (Environment.getExternalStorageDirectory(),"/Folder1");
				folder2 = new File (Environment.getExternalStorageDirectory(),"/Folder2");

				String[] fileList1 = folder1.list();
				String[] fileList2 = folder2.list();

				Arrays.sort(fileList1);
				Arrays.sort(fileList2);

				groups = new String[] { "Folder1" , "Folder2" };
				children = new String[][] { fileList1, fileList2 };
			}//constructor


			public Object getChild(int groupPosition, int childPosition) {
				return children[groupPosition][childPosition];
			}

			public long getChildId(int groupPosition, int childPosition) {
				return childPosition;
			}

			public int getChildrenCount(int groupPosition) {
				return children[groupPosition].length;
			}

			public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
										View convertView, ViewGroup parent) {

				TextView textView = new TextView(this);
				textView.setBackgroundColor(Color.BLACK);
				textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
				textView.setPadding(100, 5, 0, 5);
				textView.setTextColor(Color.WHITE);
				textView.setTextSize(23);
				textView.setId(1000);

				textView.setText(getChild(groupPosition, childPosition).toString());
				return textView;
			}//getChildView

			public Object getGroup(int groupPosition) {
				return groups[groupPosition];
			}

			public int getGroupCount() {
				return groups.length;
			}

			public long getGroupId(int groupPosition) {
				return groupPosition;
			}

			public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
					ViewGroup parent) {
				TextView textView = new TextView(this);
				textView.setBackgroundColor(Color.WHITE);
				textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
				textView.setPadding(100, 0, 0, 0);
				textView.setTextColor(Color.BLACK);
				textView.setTextSize(25);
				textView.setText(getGroup(groupPosition).toString());

				return textView;
			}

			public boolean isChildSelectable(int groupPosition, int childPosition) {
				return true;
			}

			public boolean hasStableIds() {
				return true;
			}

		}//customListAdapter

		then, reference it using:

		setContentView(R.layout.preferedlayout);
		// Set up our adapter
		listAdapter = new customListAdapter();

		expLV = (ExpandableListView) findViewById(R.id.expList);
		expLV.setAdapter(listAdapter);

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
		
	- Textuelle Folder-Sicht
		todo lib unittest
		directories als testdaten ausgeben
			/mnt/.../media
			/mnt/.../media/photos/0206Greve/Blockland
			/mnt/.../media/photos/0206Greve/Aumund
	- foto-gallery
	- foto-detail
	
	