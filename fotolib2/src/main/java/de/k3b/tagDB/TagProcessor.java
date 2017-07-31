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
package de.k3b.tagDB;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculate from each selected image the tags:
 * Created by k3b on 09.01.2017.
 */

public class TagProcessor {
    private static final ArrayList<String> EMPTY_STRING_LIST = new ArrayList<>();
    /** all tags that occoured at least once in {@link #registerExistingTags(List)} call. */
    private List<String> mAffected = null;

    /** all tags that occured in all {@link #registerExistingTags(List)} calls */
    private List<String> mAllSet = null;

    /** Remember tags for later processing.
     * Called for every selected image. */
    public void registerExistingTags(List<String> tags) {
        if ((tags != null) && (tags.size() > 0)) {
            if (mAllSet == null) {
                mAffected = new ArrayList<String>(tags);
                mAllSet = new ArrayList<String>(tags);
            } else {
                for (String tag : tags) {
                    if (!mAffected.contains(tag)) {
                        mAffected.add(tag);
                    }
                }
                for (int i = mAllSet.size() - 1; i >= 0; i--) {
                    String tag = mAllSet.get(i);
                    if (!tags.contains(tag)) {
                        mAllSet.remove(i);
                    }
                }
            }
        }
    }

    /** all tags that occoured at least once in {@link #registerExistingTags(List)} call. */
    public List<String> getAffected() {
        return mAffected;
    }

    /** all tags that occoured at least once in {@link #registerExistingTags(List)} call. */
    private void setAffected(List<String> affected) {
        this.mAffected = affected;
    }

    /** all tags that occured in all {@link #registerExistingTags(List)} calls */
    public List<String> getAllSet() {
        return mAllSet;
    }

    /** all tags that occured in all {@link #registerExistingTags(List)} calls */
    private void setAllSet(List<String> allSet) {
        this.mAllSet = allSet;
    }

    /** calculate the new tags out of added and removed tags. returns null if there is no change neccessary */
    public static List<String> getUpdated(List<String> originalCurrentTags, List<String> addedTags, List<String> removedTags) {
        ArrayList<String> currentTags = (originalCurrentTags == null) ? new ArrayList<String>() : new ArrayList<String>(originalCurrentTags);
        int modifyCount = 0;
        if (addedTags != null) {
            for (String tag : addedTags) {
                if (!currentTags.contains(tag)) {
                    currentTags.add(tag);
                    modifyCount++;
                }
            }

        }
        if (removedTags != null) {
            for (int i= removedTags.size()-1; i >= 0; i--) {
                String tag = removedTags.get(i);
                if (currentTags.contains(tag)) {
                    currentTags.remove(tag);
                    modifyCount++;
                }
            }

        }
        return (modifyCount > 0) ? currentTags : null;
    }

    /** caclulates the difference between original and changed into addedTags and removedTags and returns the number of changes. */
    public static int getDiff(List<String> _original, List<String> _changed, List<String> addedTags, List<String> removedTags) {
        List<String> original = (_original != null) ? _original : EMPTY_STRING_LIST;
        List<String> changed = (_changed != null) ? _changed : EMPTY_STRING_LIST;
        int modifyCount = 0;
        if (addedTags != null) {
            for (String tag : changed) {
                if (!original.contains(tag) && !addedTags.contains(tag)) {
                    addedTags.add(tag);
                    modifyCount++;
                }
            }
        }
        if (removedTags != null) {
            for (String tag : original) {
                if (!changed.contains(tag) && !removedTags.contains(tag)) {
                    removedTags.add(tag);
                    modifyCount++;
                }
            }
        }
        return modifyCount;
    }
}
