/*
 * Copyright (c) 2017 by k3b.
 *
 * This file is part of AndroFotoFinder.
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
import android.widget.Filter;
import android.widget.TextView;

import java.util.List;

import de.k3b.android.androFotoFinder.R;
import de.k3b.android.widget.ArrayAdapterEx;
import de.k3b.tagDB.Tag;

/** listview-item-adapter for tags-list/picker */
public class TagListArrayAdapter extends ArrayAdapterEx<Tag> {
	private static final String TAG = "TagListArrayAdapter";

	/**
	 * Lock used to modify the content . Any write operation
	 * performed on the array should be synchronized on this lock. This lock is also
	 * used by the filter (see {@link #getFilter()} to make a synchronized copy of
	 * the original array of data.
	 */
	public TagListArrayAdapter(final Context ctx, List<Tag> items) {
		super(ctx, 0, 0, items);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Get the data item for this position
		Tag currentTag = getItem(position);
		// Check if an existing view is being reused, otherwise inflate the view
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_tag_search, parent, false);
		}
		// Lookup view for data population
		TextView tvName = (TextView) convertView.findViewById(R.id.titel);
		// Populate the data into the template view using the data object
		tvName.setText(currentTag.getName());
		// Return the completed view to render on screen
		return convertView;
	}

	/** replace  */
	@Override
	protected boolean match(String lowerCaseSearchString, Tag value) {
		String name = (value != null) ? value.getName() : null;
		if (value != null) {
			final String valueText = name.toLowerCase();

			// First match against the whole, non-splitted value
			if (valueText.contains(lowerCaseSearchString)) {
				return true;
			}
		}

		return false;
	}
}
