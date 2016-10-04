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
import java.util.List;

/**
 * Persistence for all known tags.
 *
 * Created by k3b on 04.10.2016.
 */

public class TagRepository {
    private static final Logger logger = LoggerFactory.getLogger(TagRepository.class);

    /** Lines starting with char are comments. These lines are not interpreted */
    public static final java.lang.String COMMENT = "#";
    private static final String DB_NAME = "tagDB.txt";

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

        if (old != null) {
            if (TagRepository.sInstance.include(old) > 0) {
                TagRepository.sInstance.save();
            }
        }
    }

    public int include(List<Tag> items) {
        int changes = 0;
        if ((items != null) && (items.size() > 0)) {
            List<Tag> newItems = this.load();
            for (Tag oldItem : items) {
                if (!newItems.contains(oldItem)) {
                    newItems.add(oldItem);
                    changes++;
                }
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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("load(): " + mItemList.size() + " items from " + this.mFile);
            }
        } else if (logger.isDebugEnabled()) {
            logger.debug("load() cached value : " + mItemList.size() + " items from " + this.mFile);
        }
        return mItemList;
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
                if (logger.isDebugEnabled()) {
                    logger.debug("save(): " + mItemList.size() + " items to " + this.mFile);
                }
                save(mItemList, new FileWriter(this.mFile, false));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("save(): no items for " + this.mFile);
        }
        return this;
    }

    // Load(new InputStreamReader(inputStream, "UTF-8"))
    /** Load points from reader */
    public void load(List<Tag> result, Reader reader) throws IOException {
        String line;
        BufferedReader br = new BufferedReader(reader);
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if ((line.length() > 0) && (!line.startsWith(COMMENT))) {
                Tag item = loadItem(line);
                final boolean valid = isValid(item);
                if (logger.isDebugEnabled()) {
                    logger.debug("load(" + line + "): " + ((valid) ? "loaded" : "ignored"));
                }

                if (valid) result.add((Tag) item);
            }
        }
        br.close();
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
    protected void save(List<Tag> source, Writer writer) throws IOException {
        for (Tag item : source) {
            saveItem(writer, item);
        }
        writer.close();
    }

    /** Saves one point to writer */
    protected boolean saveItem(Writer writer, Tag item) throws IOException {
        final boolean valid = isValid(item);

        final String line = item.toString();
        if (valid) {
            writer.write(line);
            writer.write("\n");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("save(" + line + "): " + ((valid) ? "saved" : "ignored" ));
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

}
