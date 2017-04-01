/*
 * Copyright (c) 2016-2017 by k3b.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.k3b.FotoLibGlobal;

/**
 * Persistence for all known tags.
 *
 * Created by k3b on 04.10.2016.
 */

public class TagRepository {
    // android - log compatible
    private static final String dbg_context = "TagRepository: ";
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    /** Lines starting with char are comments. These lines are not interpreted */
    public static final java.lang.String COMMENT = "#";
    private static final String DB_NAME = "tagDB.txt";
    private static final String IMPORT_ROOT = "unsorted";
    public static final String INDENT = "\t";

    private static TagRepository sInstance = null;

    /** Where data is loaded from/saved to */
    private final File mFile;

    /** The items contained in this repository */
    protected List<Tag> mItemList = null;

    /** Connect repository to a {@link File}. */
    public TagRepository(File file) {
        this.mFile = file;
    }

    public static TagRepository getInstance() {
        return sInstance;
    }

    public static void setInstance(TagRepository instance) {
        TagRepository.sInstance = instance;
    }

    public static void setInstance(File parentDir) {
        if (parentDir == null) throw new IllegalArgumentException("TagRepository.setInstance(null)");

        List<Tag> old = null;
        File newFile = new File(parentDir, DB_NAME);
        if (TagRepository.sInstance != null){
            if (TagRepository.sInstance.mFile.equals(newFile)) return; // no change: nothing to do

            old = TagRepository.sInstance.load();
        }
        TagRepository.sInstance = new TagRepository(newFile);

        if ((old != null) && (TagRepository.sInstance.includeChildTags(null, old) > 0)) {
            TagRepository.sInstance.save();
        }
    }

    public int includeChildTags(Tag parent, List<Tag> newItems) {
        int changes = 0;
        if ((newItems != null) && (newItems.size() > 0)) {
            List<Tag> existingItems = this.load();
            for (Tag newItem : newItems) {
                if (!existingItems.contains(newItem)) {
                    if (parent != null) newItem.setParent(parent);
                    existingItems.add(newItem);
                    changes++;
                }
            }
        }
        return changes;
    }

    public int include(Tag parent, List<String> children) {
        int changes = 0;
        if ((children != null) && (children.size() > 0)) {
            List<Tag> existingItems = this.load();
            for (String newItem : children) {
                changes += include(existingItems, parent, null, newItem);
            }
        }
        return changes;
    }

    /**
     * make shure that newSubItemsExpression is included in all below parent. Inserts if neccessary.
     *
     * @param all contain all items
     * @param parent where relative items are added to. if null relative items go to root.
     * @param target if not null this tag should get the name of newSubItemsExpression
     *@param childNameExpression i.e. /d,a,b,c/c1/c11 adds d to root, a,b,c as children to parent and c gets child c1 and c1 gets child c11.  @return number of (sub-)tags that where inserted
     */
    public static int include(List<Tag> all, Tag parent, Tag target, String childNameExpression) {
        int changes = 0;

        Tag firstTarget = target;
		if (childNameExpression != null) {
			String[] newSubPathExpression = childNameExpression.split("[,;:|]+");
			for(String newChild : newSubPathExpression) {
				changes += includeTagChildren(all, parent, firstTarget, newChild);
                firstTarget = null;
			}
		}
		
		return changes;
	}

	/** make shure that newSubPathExpression is included in all below parent. Inserts if neccessary.
	  * newSubPathExpression i.e. c/c1/c11 adds c as child to parent and c gets child c1 and c1 gets child c11.
	  * @return number of (sub-)tags that where inserted */
    private static int includeTagChildren(List<Tag> all, Tag tagParent, Tag target, String newSubPathExpression) {
        int changes = 0;
		
        if (newSubPathExpression != null) {
            Tag tagRoot = tagParent;
            if (newSubPathExpression.startsWith("/") || newSubPathExpression.startsWith("\\")) {
                // add to root
                tagRoot = null;
            }

            Tag currentTagParent = tagRoot;
            String[] newPathNames = newSubPathExpression.split("[\\/]+");

            for(String pathName : newPathNames) {
				if (pathName != null) { 
					pathName = pathName.trim();
					if (pathName.length() > 0) {
						Tag tag = Tag.findFirstChildByName(all, currentTagParent, pathName);
						if (tag == null) {
							// there is no currentTagParent with name=pathName yet: insert
							tag = new Tag().setName(pathName);
							tag.setParent(currentTagParent);
							all.add(tag);
							changes++;						
						} // else already existing
						currentTagParent = tag;
					}
				}
            }

            if ((target != null) && (currentTagParent != null)) {
                target.setName(currentTagParent.getName());
                target.setParent(currentTagParent.getParent());
                all.remove(currentTagParent);
            }

        }
		return changes;
    }
    
    /** Load from repository-file to memory.
     *
     * @return data loaded
     */
    public List<Tag> load() {
        if (mItemList == null) {
            mItemList = new ArrayList<>();
            if (this.mFile.exists()) {
                try {
                    load(mItemList, new FileReader(this.mFile));

                    sortByNameIgnoreCase();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            logger.debug(dbg_context + "load(): " + mItemList.size() + " items from " + this.mFile);

        } else if (FotoLibGlobal.debugEnabled) {
            // logger.debug(dbg_context + "load() cached value : " + mItemList.size() + " items from " + this.mFile);
        }
        return mItemList;
    }

    public void sortByNameIgnoreCase() {
        Collections.sort(mItemList, Tag.COMPARATOR_HIERARCHY);
    }

    /**
     * Uncached, fresh load from repository-file to memory.
     *
     * @return data loaded
     */
    public List<Tag> reload() {
        this.mItemList = null;
        return load();
    }

    /**
     * Removes item from repository-momory and file.
     *
     * @param item that should be removed
     *
     * @return true if successful
     */
    public TagRepository delete(Tag item) {
        if ((item != null) && load().remove(item)) {
            save();
        }

        return this;
    }

    /** Save from meomory to repositoryfile.
     *
     * @return false: error.
     */
    public TagRepository save() {
        try {
            if ((mItemList != null) && (mItemList.size() > 0)) {
                if (!this.mFile.exists()) {
                    this.mFile.getParentFile().mkdirs();
                }

                logger.debug(dbg_context + "save(): " + mItemList.size() + " items to " + this.mFile);

                save(mItemList, new FileWriter(this.mFile, false), INDENT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (FotoLibGlobal.debugEnabled) {
            logger.debug(dbg_context + "save(): no items for " + this.mFile);
        }
        return this;
    }

    // Load(new InputStreamReader(inputStream, "UTF-8"))
    /** Load points from reader */
    public void load(List<Tag> result, Reader reader) throws IOException {
        String rawLine;
        BufferedReader br = new BufferedReader(reader);
        int initialResultSize = result.size();
        List<Integer> indents = new ArrayList<Integer>();
        while ((rawLine = br.readLine()) != null) {
            String line = rawLine.trim();
            if ((line.length() > 0) && (!line.startsWith(COMMENT))) {
                Tag item = loadItem(line);
                final boolean valid = isValid(item);
                if (FotoLibGlobal.debugEnabled) {
                    logger.debug(dbg_context + "load(" + line + "): " + ((valid) ? "loaded" : "ignored"));
                }

                if (valid) {
                    result.add((Tag) item);
                    indents.add(getIndent(rawLine));
                }

            }
        }
        br.close();

        inferParentsFromIndents(result, initialResultSize, indents);
    }

    private void inferParentsFromIndents(List<Tag> result, int initialResultSize, List<Integer> indents) {
        Tag lastTag     = null;
        int lastIndent  = indents.get(0);
        Tag lastParent     = null;

        for (int i = 0; i < indents.size(); i++) {
            Tag cur = result.get(initialResultSize + i);
            int indent = indents.get(i);
            if (indent > lastIndent) {
                lastParent = lastTag;
            } else if (indent < lastIndent) {
                int parentIndex = findParentIndexByIndent(indents, i, indent);
                if (parentIndex >= 0) {
                    lastParent  = result.get(initialResultSize + parentIndex);
                } else {
                    lastParent     = null;
                }
            } // else if (indent == lastIndent) lastParent remains the same
            cur.setParent(lastParent);
            lastIndent = indent;
            lastTag = cur;
        }
    }

    private int findParentIndexByIndent(List<Integer> indents, int index, int indent) {
        int i = index;
        while ((i >= 0) && (indents.get(i) >= indent)) i--;
        return i;
    }

    private int getIndent(String rawLine) {
        int nonIndent = 0;
        while (nonIndent < rawLine.length()) {
            int c = rawLine.charAt(nonIndent);
            if ((c != ' ') && c != '\t') return nonIndent;
            nonIndent++;
        }
        return nonIndent;
    }

    /** Implementation detail: Load point from file line. */
    protected Tag loadItem(String line) {
        Tag item = create();
        item.fromString(line);
        return item;
    }

    /** Factory method to generate a new empy item while reading.
     *
     * The method can be overwritten to create custom Item types.
     */
    protected Tag create() {
        return (Tag) new Tag();
    }

    /** Save source-points to writer */
    protected void save(List<Tag> source, Writer writer, String indent) throws IOException {
        List<Tag> sorted = new ArrayList<>(source);
        Collections.sort(sorted, Tag.COMPARATOR_HIERARCHY);

        for (Tag item : sorted) {
            saveItem(writer, item, indent);
        }
        writer.close();
    }

    /** Saves one point to writer */
    protected boolean saveItem(Writer writer, Tag item, String indent) throws IOException {
        final boolean valid = isValid(item);

        final String line = item.toString();
        if (valid) {
            for (int indentCount = item.getParentCount(); indentCount > 0; indentCount--) {
                writer.write(indent);
            }
            writer.write(line);
            writer.write("\n");
        }
        if (FotoLibGlobal.debugEnabled) {
            logger.debug(dbg_context + "save(" + line + "): " + ((valid) ? "saved" : "ignored" ));
        }
        return valid;
    }

    /** Returns true if item should be loaded from / saved to repository */
    protected boolean isValid(Tag item) {
        return (item != null);
    }

    /** Returns true if item.id should be loaded from / saved to repository */
    private boolean isValidId(String id) {
        return ((id != null) && (!id.startsWith("#")));
    }

    public boolean contains(Tag name) {
        List<Tag> items = load();
        if (items != null) {
            return items.contains(name);
        }
        return false;
    }

    public boolean contains(String name) {
        return null != findFirstByName(name);
    }

    public Tag findFirstByName(String name) {
        return findFirstByName(load(), name);
    }

    public static Tag findFirstByName(List<Tag> items, String name) {
        if (items != null) {
            for (Tag item : items) {
                if (name.equals(item.getName())) return item;
            }
        }
        return null;
    }

    /** get or create parent-tag where alle imports are appendend as children */
    public Tag getImportRoot() {
        Tag result = findFirstByName(IMPORT_ROOT);
        if (result == null) {
            List<Tag> existingItems = this.load();
            result = new Tag().setName(IMPORT_ROOT).setParent(null);
            existingItems.add(result);
        }
        return result;
    }

    public int includeIfNotFound(List<String>... lists) {
        List<Tag> allTags = load();
        int modified = 0;
        Tag root = null;
        for (List<String> list : lists) {
            if (list != null) {
                for(String item : list) {
                    if ((item != null) && (item.length() > 0) && (null == findFirstByName(allTags, item))) {
                        if (root == null) root = getImportRoot();

                        allTags.add(new Tag().setName(item).setParent(root));
                        modified++;
                    }
                }
            }
        }
        return modified;
    }
}
