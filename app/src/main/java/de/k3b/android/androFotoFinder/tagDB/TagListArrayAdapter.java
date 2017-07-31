/*
 * Copyright (c) 2017 by k3b.
 *
 * This file is part of AndroFotoFinder / #APhotoManager.
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.android.androFotoFinder.tagDB;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.util.ResourceUtils;
import de.k3b.android.widget.ArrayAdapterEx;
import de.k3b.tagDB.Tag;

/** listview-item-adapter for tags-list/picker */
public class TagListArrayAdapter extends ArrayAdapterEx<Tag> {
	private static final String TAG = "TagListArrayAdapter";
	public static final List<String> ALL_REMOVEABLE = new ArrayList<String>();
	private final List<String> mAffectedNames;
	private final List<String> mBookMarkNames;
	private List<String> mAddNames;
	private final List<String> mRemoveNames;
	private String mLastFilterParam = null;

	/** mImageButtonLongClicked workaround imagebutton-long-click prevent list-itemclick. */
	private boolean mImageButtonLongClicked = false;

	public TagListArrayAdapter(final Context ctx,
							   List<Tag> existingTags,
							   List<String> addNames,
							   List<String> removeNames,
							   List<String> affectedNames,
							   List<String> bookMarkNames) {
		super(ctx, 0, 0, existingTags);
		mAffectedNames = affectedNames;
		mBookMarkNames = bookMarkNames;
		mRemoveNames = removeNames;
		mAddNames = addNames;
	}

	public void setFilterParam(String filterParam) {
		this.mLastFilterParam = filterParam;
		getFilter().filter(filterParam);
	}

	/** mImageButtonLongClicked workaround imagebutton-long-click prevent list-itemclick. */
	public boolean isImageButtonLongClicked() {
		return mImageButtonLongClicked;
	}

	public void setImageButtonLongClicked(boolean mImageButtonLongClicked) {
		this.mImageButtonLongClicked = mImageButtonLongClicked;
	}

	private class Holder {
		public Tag currentTag;
		public ImageView bookmarkIcon;
		public ImageView addIcon;
		public ImageView removeIcon;
		public boolean isBookmark = false;
		public boolean isAdd = false;
		public boolean isRemove = false;
		public boolean isAffected = true;
		public TextView name;

		public void setBookmark(boolean value) {
			this.isBookmark = value;
			handleToggle(value, TagListArrayAdapter.this.mBookMarkNames,bookmarkIcon ,
					android.R.drawable.btn_star_big_on, android.R.drawable.btn_star_big_off);
		}

		public void setAdd(boolean value) {
			this.isAdd = value;
			if (TagListArrayAdapter.this.mAddNames == null) TagListArrayAdapter.this.mAddNames = new ArrayList<String>();
			handleToggle(value, TagListArrayAdapter.this.mAddNames, addIcon ,
					android.R.drawable.ic_input_add, android.R.drawable.ic_menu_add);
		}

		public void setRemove(boolean value) {
			this.isRemove = value;
			handleToggle(value, TagListArrayAdapter.this.mRemoveNames, removeIcon ,
					android.R.drawable.ic_delete, android.R.drawable.ic_menu_close_clear_cancel);
		}


		private void handleToggle(boolean onOffValue, List<String> onNames, ImageView icon, int id_drawable_on, int id_drawable_off) {
			if (onNames != null) {
				String name = (currentTag != null) ? currentTag.getName() : null;
				if (name != null) {
					if (onOffValue && !onNames.contains(name))
						onNames.add(name);
					if (!onOffValue && onNames.contains(name))
						onNames.remove(name);
					icon.setImageDrawable(ResourceUtils.getDrawable(getContext(),(onOffValue) ? id_drawable_on : id_drawable_off));
				}
			}
		}

		public void includeTagParents(List<String> addNames, List<String> removeNames) {
			Tag tag = this.currentTag;
			int modifyCount = 0;
			while (tag != null) {
				String tagName = tag.getName();

				if ((addNames != null) && (!addNames.contains(tagName))) {
					addNames.add(tagName);
					modifyCount++;
				}
				if ((removeNames != null)  && removeNames.contains(tagName)) {
					removeNames.remove(tagName);
					modifyCount++;
				}
				tag = tag.getParent();
			}
			if (modifyCount > 0) {
				reloadList();
			}
		}
	}

	public void reloadList() {
		setFilterParam(mLastFilterParam);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View _convertView, ViewGroup parent) {
		final Holder holder;
		View convertView = _convertView;
		// Check if an existing view is being reused, otherwise inflate the view
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_tag_search, parent, false);
			holder = new Holder();
			convertView.setTag(holder);
			holder.name = (TextView) convertView.findViewById(R.id.name);
			holder.name.setTag(holder);

			holder.bookmarkIcon = (ImageView) convertView.findViewById(R.id.bookmark);
			if (mBookMarkNames != null) {
				holder.bookmarkIcon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						holder.setBookmark(!holder.isBookmark);
					}
				});
				holder.bookmarkIcon.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						setImageButtonLongClicked(true);
						holder.includeTagParents(mBookMarkNames, null);
						return true;
					}
				});
			} else {
				holder.bookmarkIcon.setVisibility(View.GONE);
			}

			holder.addIcon = (ImageView) convertView.findViewById(R.id.add);
			holder.addIcon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					holder.setAdd(!holder.isAdd);
					if (holder.isAdd) holder.setRemove(false);
				}
			});
			holder.addIcon.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					setImageButtonLongClicked(true);
					holder.includeTagParents(mAddNames, mRemoveNames);
					return true;
				}
			});

			holder.removeIcon = (ImageView) convertView.findViewById(R.id.remove);
			if ((mRemoveNames != null) && (mAffectedNames != null)  && ((TagListArrayAdapter.ALL_REMOVEABLE == mAffectedNames) || (mAffectedNames.size() > 0))) {
				holder.removeIcon.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						holder.setRemove(!holder.isRemove);
						if (holder.isRemove) holder.setAdd(false);
					}
				});
				holder.removeIcon.setOnLongClickListener(new View.OnLongClickListener() {
					@Override
					public boolean onLongClick(View v) {
						setImageButtonLongClicked(true);
						holder.includeTagParents(mRemoveNames, mAddNames);
						return true;
					}
				});
			} else {
				holder.removeIcon.setVisibility(View.GONE);
			}

		} else {
			holder = (Holder) convertView.getTag();
		}

		// Get the data item for this position
		holder.currentTag = getItem(position);
		String tagName = holder.currentTag.getName();

		if (mRemoveNames != null) {
			holder.setRemove(mRemoveNames.contains(tagName));
		}
		if (mAddNames != null) {
			holder.setAdd(mAddNames.contains(tagName));
		}
		if ((mAffectedNames != null) && (TagListArrayAdapter.ALL_REMOVEABLE != mAffectedNames)){
			holder.isAffected = mAffectedNames.contains(tagName);
			if (mRemoveNames != null) {
				holder.removeIcon.setVisibility(holder.isAffected ? View.VISIBLE : View.INVISIBLE);
			}
		}

		holder.name.setText(holder.currentTag.getPath());
		if (mBookMarkNames != null) {
			holder.setBookmark(mBookMarkNames.contains(tagName));
		}
		return convertView;
	}

	public static Tag getTag(Object v) {
		Object parent = v;
		while (parent instanceof View) {
			View parentView = (View) parent;
			Object tag = parentView.getTag();
			if (tag instanceof Holder) return ((Holder)tag).currentTag;
			parent =  parentView.getParent();
		}
		return null;
	}

	/** replace  */
	@Override
	protected boolean match(String lowerCaseSearchString, Tag value) {
		String name = (value != null) ? value.getName() : null;
		if (value != null) {
			// name is always visible
			// favorite is always visible
			if ((mBookMarkNames != null) && (mBookMarkNames.contains(name))) return true;
			if ((mAddNames != null) && (mAddNames.contains(name))) return true;
			if ((mRemoveNames != null) && (mRemoveNames.contains(name))) return true;
			if ((mAffectedNames != null) && (mAffectedNames.contains(name))) return true;

			final String valueText = name.toLowerCase();

			// First match against the whole, non-splitted value
			if (valueText.contains(lowerCaseSearchString)) {
				return true;
			}
		}

		return false;
	}
}
