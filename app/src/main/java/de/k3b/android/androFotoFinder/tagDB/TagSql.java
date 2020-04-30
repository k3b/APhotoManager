/*
 * Copyright (c) 2016-2019 by k3b.
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

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.k3b.LibGlobal;
import de.k3b.android.androFotoFinder.Global;
import de.k3b.android.androFotoFinder.media.PhotoPropertiesMediaDBContentValues;
import de.k3b.android.androFotoFinder.queries.AndroidAlbumUtils;
import de.k3b.android.androFotoFinder.queries.FotoSql;
import de.k3b.android.util.PhotoPropertiesMediaFilesScanner;
import de.k3b.database.QueryParameter;
import de.k3b.io.GalleryFilterParameter;
import de.k3b.io.IFile;
import de.k3b.io.IGalleryFilter;
import de.k3b.io.ListUtils;
import de.k3b.io.VISIBILITY;
import de.k3b.media.MediaFormatter;
import de.k3b.media.PhotoPropertiesUpdateHandler;
import de.k3b.media.PhotoPropertiesUtil;
import de.k3b.media.PhotoPropertiesXmpSegment;
import de.k3b.tagDB.Tag;
import de.k3b.tagDB.TagConverter;

/**
 * Database related code to handle non standard image processing (Tags, Description)
 *
 * Created by k3b on 30.09.2016.
 */
public class TagSql extends FotoSql {


    public static final String SQL_COL_EXT_TAGS = MediaStore.Video.Media.TAGS;

    public static final String SQL_COL_EXT_DESCRIPTION = MediaStore.Images.Media.DESCRIPTION;

    /** The date & time when last non standard media-scan took place
     *  <P>Type: INTEGER (long) as milliseconds since jan 1, 1970</P> */
    public static final String SQL_COL_EXT_XMP_LAST_MODIFIED_DATE = MediaStore.Video.Media.DURATION;

    public static final int EXT_LAST_EXT_SCAN_UNKNOWN = 0;
    public static final int EXT_LAST_EXT_SCAN_NO_XMP_IN_CSV = 5;
    public static final int EXT_LAST_EXT_SCAN_NO_XMP = 10;

    protected static final String FILTER_EXPR_PATH_LIKE_XMP_DATE = FILTER_EXPR_PATH_LIKE
            + " and ("
            + SQL_COL_EXT_XMP_LAST_MODIFIED_DATE + " is null or "
            + SQL_COL_EXT_XMP_LAST_MODIFIED_DATE + " < ?) ";

    protected static final String FILTER_EXPR_TAGS_NONE         = "(" + SQL_COL_EXT_TAGS + " is null)";
    protected static final String FILTER_EXPR_TAGS_INCLUDED = "(" + SQL_COL_EXT_TAGS + " like ?)";

    protected static final String FILTER_EXPR_TAGS_NONE_OR_INCLUDED = "(" + FILTER_EXPR_TAGS_NONE
            + " or " + FILTER_EXPR_TAGS_INCLUDED +")";
    protected static final String FILTER_EXPR_TAG_EXCLUDED = "(" + SQL_COL_EXT_TAGS + " not like ?)";
    protected static final String FILTER_EXPR_TAG_NONE_OR_EXCLUDED = "(" + FILTER_EXPR_TAGS_NONE
            + " OR " + FILTER_EXPR_TAG_EXCLUDED + ")";

    protected static final String FILTER_EXPR_ANY_LIKE = "((" + SQL_COL_PATH + " like ?) OR  (" + SQL_COL_EXT_DESCRIPTION
            + " like ?) OR " + FILTER_EXPR_TAGS_INCLUDED + " OR  (" + SQL_COL_EXT_TITLE + " like ?))";


    /** translates a query back to filter */
    public static IGalleryFilter parseQueryEx(QueryParameter query, boolean remove) {
        if (query != null) {
            GalleryFilterParameter resultFilter = (GalleryFilterParameter) parseQuery(query,remove);

            // from more complex to less complex
            String[] params;
            StringBuilder any = null;
            while ((params = getParams(query, FILTER_EXPR_ANY_LIKE, remove)) != null) {
                if (any == null) {
                    any = new StringBuilder().append(params[0]);
                } else {
                    any.append(" ").append(params[0]);

                }
            }
            if (any != null) resultFilter.setInAnyField(any.toString());

            parseTagsFromQuery(query, remove, resultFilter);

            if (getParams(query, FILTER_EXPR_PRIVATE_PUBLIC, remove) != null) {
                resultFilter.setVisibility(VISIBILITY.PRIVATE_PUBLIC);
            }
            if (getParams(query, FILTER_EXPR_PRIVATE, remove) != null) {
                resultFilter.setVisibility(VISIBILITY.PRIVATE);
            }
            if (getParams(query, FILTER_EXPR_PUBLIC, remove) != null) {
                resultFilter.setVisibility(VISIBILITY.PUBLIC);
            }

            return resultFilter;
        }
        return null;
    }

    private static void parseTagsFromQuery(QueryParameter query, boolean remove, GalleryFilterParameter resultFilter) {
        // 0..n times for excluded expressions
        String param;

        resultFilter.setWithNoTags(false);

     // FILTER_EXPR_TAG_NONE_OR_EXCLUDED or FILTER_EXPR_TAG_EXCLUDED
        String filter = FILTER_EXPR_TAG_NONE_OR_EXCLUDED;
        param = getParam(query, filter, remove);
        if (param != null) {
            resultFilter.setWithNoTags(true);
        } else {
            filter = FILTER_EXPR_TAG_EXCLUDED;
            param = getParam(query, filter, remove);
        }
        if (param != null) {
            ArrayList<String> excluded = new ArrayList<String>();
            do {
                if ((param.startsWith("%;") && (param.endsWith(";%")))) param = param.substring(2, param.length() -2);
                excluded.add(param);
                param = getParam(query, filter, remove);
            } while (remove && (param != null));
            resultFilter.setTagsAllExcluded(excluded);
        }

        // different possible combinations of "tags is null" and "tag list"
        if ((param = getParam(query, FILTER_EXPR_TAGS_NONE_OR_INCLUDED, remove)) != null) {
            resultFilter.setWithNoTags(true).setTagsAllIncluded(TagConverter.fromString(param));
        } else if ((param = getParam(query, FILTER_EXPR_TAGS_INCLUDED, remove)) != null) {
            resultFilter.setTagsAllIncluded(TagConverter.fromString(param));
        } else if (getParams(query, FILTER_EXPR_TAGS_NONE, remove) != null) {
            resultFilter.setWithNoTags(true).setTagsAllIncluded(null).setTagsAllExcluded(null);
        }


    }

    public static QueryParameter filter2NewQuery(IGalleryFilter filter) {
        return AndroidAlbumUtils.getAsMergedNewQuery(null, filter);
    }

    public static void filter2QueryEx(QueryParameter resultQuery, IGalleryFilter filter, boolean clearWhereBefore) {
        if ((resultQuery != null) && (!GalleryFilterParameter.isEmpty(filter))) {
            filter2Query(resultQuery, filter, clearWhereBefore);
            if (Global.Media.enableIptcMediaScanner) {
                String allAny = filter.getInAnyField();

                addFilterAny(resultQuery, allAny);


                List<String> includes = ListUtils.emptyAsNull(filter.getTagsAllIncluded());
                List<String> excludes = ListUtils.emptyAsNull(filter.getTagsAllExcluded());
                boolean withNoTags = filter.isWithNoTags();

                if ((includes == null) && (excludes == null) && withNoTags) {
                    resultQuery.addWhere(FILTER_EXPR_TAGS_NONE);
                } else {
                    addWhereTagsIncluded(resultQuery, includes, withNoTags);

                    if (excludes != null) {
                        for (String tag : excludes) {
                            if ((tag != null) && (tag.length() > 0)) {
                                addWhereTagExcluded(resultQuery, tag, withNoTags);
                            }
                        }
                    }
                }
                int ratingMin = filter.getRatingMin();
                if (ratingMin > 0) {
                    resultQuery.addWhere(FILTER_EXPR_RATING_MIN, ""+ratingMin);

                }

                setWhereVisibility(resultQuery, filter.getVisibility());
            }
        }
    }

    public static void addFilterAny(QueryParameter resultQuery, String allAny) {
        if (allAny != null) {
            for (String any : allAny.split(" ")) {
                if ((any != null) && (any.length() > 0)) {
                    if (!any.contains("%")) {
                        any = "%" + any + "%";
                    }
                    resultQuery.addWhere(FILTER_EXPR_ANY_LIKE, any, any, any, any);
                }
            }
        }
    }

    private static QueryParameter addWhereTagExcluded(QueryParameter resultQuery, String tag, boolean withNoTags) {
        return resultQuery.addWhere((withNoTags) ? FILTER_EXPR_TAG_NONE_OR_EXCLUDED : FILTER_EXPR_TAG_EXCLUDED, "%;" + tag + ";%");
    }

    /** return number of applied tags */
    public static int addWhereAnyOfTags(QueryParameter resultQuery, List<Tag> tags) {
        StringBuilder sqlWhereStatement = new StringBuilder();
        int index = 0;
        String[] params = null;

        if (tags != null) {
            params = new String[tags.size()];
            int end = params.length;
            for (Tag tag : tags) {
                String tagValue = (tag != null) ? tag.getName() : null;
                if ((tagValue != null) && (tagValue.length() > 0)) {
                    if (index > 0) sqlWhereStatement.append(" OR ");
                    sqlWhereStatement
                            .append("(").append(SQL_COL_EXT_TAGS)
                            .append(" like ?)");

                    params[index++] = "%;" + tagValue + ";%";
                } else {
                    params[--end] = null;
                }
            }
        }

        if (index > 0) {
            resultQuery.addWhere(sqlWhereStatement.toString(), params);
        }
        return index;
    }
    public static void addWhereTagsIncluded(QueryParameter resultQuery, List<String> includes, boolean withNoTags) {
        if (includes != null) {
            String includesWhere = TagConverter.asDbString("%", includes);
            if (includesWhere != null) {
                if (withNoTags) {
                    resultQuery.addWhere(FILTER_EXPR_TAGS_NONE_OR_INCLUDED, includesWhere);
                } else {
                    resultQuery.addWhere(FILTER_EXPR_TAGS_INCLUDED, includesWhere);
                }
            }
        }
    }

    public static int fixPrivate() {
        // update ... set media_type=1001 where media_type=1 and tags like '%;PRIVATE;%'
        ContentValues values = new ContentValues();
        values.put(SQL_COL_EXT_MEDIA_TYPE, MEDIA_TYPE_IMAGE_PRIVATE);
        StringBuilder where = new StringBuilder();
        where
            .append(TagSql.FILTER_EXPR_PUBLIC)
            .append(" AND (")
            .append(TagSql.FILTER_EXPR_TAGS_INCLUDED);
        if (LibGlobal.renamePrivateJpg) {
            where.append(" OR ").append(TagSql.FILTER_EXPR_PATH_LIKE.replace("?","'%" +
                            PhotoPropertiesUtil.IMG_TYPE_PRIVATE + "'"));
        }
        where.append(")");
        return getMediaDBApi().exexUpdateImpl("Fix visibility private",
                values, where.toString(), new String[] {"%;" + VISIBILITY.TAG_PRIVATE +
                ";%"});
    }

    public static void setTags(ContentValues values, Date xmpFileModifyDate, String... tags) {
        values.put(SQL_COL_EXT_TAGS, TagConverter.asDbString("", tags));
        setXmpFileModifyDate(values, xmpFileModifyDate);
    }

    public static void setDescription(ContentValues values, Date xmpFileModifyDate, String description) {
        values.put(SQL_COL_EXT_DESCRIPTION, description);
        setXmpFileModifyDate(values, xmpFileModifyDate);
    }

    public static void setRating(ContentValues values, Date xmpFileModifyDate, Integer value) {
        values.put(SQL_COL_EXT_RATING, value);
        setXmpFileModifyDate(values, xmpFileModifyDate);
    }

    public static void setXmpFileModifyDate(ContentValues values, Date xmpFileModifyDate) {
        long lastScan = (xmpFileModifyDate != null)
                ? xmpFileModifyDate.getTime()/1000 // secs since 1970-01-01
                : EXT_LAST_EXT_SCAN_UNKNOWN;
        setXmpFileModifyDate(values, lastScan);
    }

    public static void setXmpFileModifyDate(ContentValues values, long xmpFileModifyDateSecs) {
        if ((values != null)
                && (xmpFileModifyDateSecs != EXT_LAST_EXT_SCAN_UNKNOWN)) {
            values.put(SQL_COL_EXT_XMP_LAST_MODIFIED_DATE, xmpFileModifyDateSecs);
        }
    }

    public static void setFileModifyDate(ContentValues values, String path) {
        File f = new File(path);

        if ((values != null)
                && (f != null)) {
            long millisecs = f.lastModified();
            if (millisecs != 0) {
                setFileModifyDate(values, millisecs / 1000);
            }
        }
    }

    public static void setFileModifyDate(ContentValues values, long fileModifyDateSecs) {
        if (fileModifyDateSecs != 0) {
            values.put(SQL_COL_LAST_MODIFIED, fileModifyDateSecs);
        }
    }

    /**
     * Copies non null content of jpg to existing media database item.
     *
     * @param dbgContext debug message
     * @param allowSetNulls     if one of these columns are null, the set null is copied, too
     * @return number of changed db items
     */
    public static int updateDB(String dbgContext, String oldFullJpgFilePath,
                               PhotoPropertiesUpdateHandler jpg, MediaFormatter.FieldID... allowSetNulls) {
        if ((jpg != null) && (!PhotoPropertiesMediaFilesScanner.isNoMedia(oldFullJpgFilePath))) {
            ContentValues dbValues = new ContentValues();
            PhotoPropertiesMediaDBContentValues mediaValueAdapter = new PhotoPropertiesMediaDBContentValues();

            // dbValues.clear();
            final int modifiedColumCout = PhotoPropertiesUtil.copyNonEmpty(mediaValueAdapter.set(dbValues, null), jpg, allowSetNulls);
            if (modifiedColumCout >= 1) {
                String newFullJpgFilePath = null;
                if (LibGlobal.renamePrivateJpg) {
                    newFullJpgFilePath = PhotoPropertiesUtil.getModifiedPath(jpg);
                }

                if (newFullJpgFilePath == null) {
                    newFullJpgFilePath = oldFullJpgFilePath;
                }

                mediaValueAdapter.setPath(newFullJpgFilePath);

                PhotoPropertiesXmpSegment xmp = jpg.getXmp();
                long xmpFilelastModified = (xmp != null) ? xmp.getFilelastModified() : 0;
                if (xmpFilelastModified == 0) xmpFilelastModified = TagSql.EXT_LAST_EXT_SCAN_NO_XMP;
                TagSql.setXmpFileModifyDate(dbValues, xmpFilelastModified);
                TagSql.setFileModifyDate(dbValues, newFullJpgFilePath);

                return TagSql.execUpdate(dbgContext, oldFullJpgFilePath,
                        TagSql.EXT_LAST_EXT_SCAN_UNKNOWN, dbValues, VISIBILITY.PRIVATE_PUBLIC);
            }


        }
        return 0;
    }


    public static int execUpdate(String dbgContext, String path, long xmpFileDate, ContentValues values, VISIBILITY visibility) {
        if ((!Global.Media.enableXmpNone) || (xmpFileDate == EXT_LAST_EXT_SCAN_UNKNOWN)) {
            return getMediaDBApi().execUpdate(dbgContext, path, values, visibility);
        }
        return getMediaDBApi().exexUpdateImpl(dbgContext, values, FILTER_EXPR_PATH_LIKE_XMP_DATE, new String[]{path, Long.toString(xmpFileDate)});
    }

    /** return how many photos exist that have one or more tags from list */
    public static int getTagRefCount(List<Tag> tags) {
        QueryParameter query = new QueryParameter()
                .addColumn("count(*)").addFrom(SQL_TABLE_EXTERNAL_CONTENT_URI_FILE_NAME);
        if (addWhereAnyOfTags(query, tags) > 0) {
            Cursor c = null;
            try {
                c = getMediaDBApi().createCursorForQuery(null, "getTagRefCount", query, VISIBILITY.PRIVATE_PUBLIC, null);
                if (c.moveToFirst()) {
                    return c.getInt(0);
                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, "FotoSql.getTagRefCount(): error executing " + query, ex);
            } finally {
                if (c != null) c.close();
            }
        }
        return 0;
    }

    /**
     * converts selectedItemPks and/or anyOfTags to TagWorflowItem-s
     * @param selectedItemPks if not null list of comma seperated item-pks
     * @param anyOfTags if not null list of tag-s where at least one oft the tag must be in the photo.
     * @return
     */
    public static List<TagWorflowItem> loadTagWorflowItems(String selectedItemPks, List<Tag> anyOfTags) {
        QueryParameter query = new QueryParameter()
                .addColumn(TagSql.SQL_COL_PK, TagSql.SQL_COL_PATH, TagSql.SQL_COL_EXT_TAGS, TagSql.SQL_COL_EXT_XMP_LAST_MODIFIED_DATE);

        int filterCount = 0;
        if (selectedItemPks != null) {
            filterCount += selectedItemPks.trim().length();
            TagSql.setWhereSelectionPks(query, selectedItemPks);
        }

        if (anyOfTags != null) {
            filterCount += TagSql.addWhereAnyOfTags(query, anyOfTags);
        }

        Cursor c = null;
        List<TagWorflowItem> result = new ArrayList<TagWorflowItem>();

        if (filterCount > 0) {
            try {
                c = getMediaDBApi().createCursorForQuery(null, "loadTagWorflowItems", query, VISIBILITY.PRIVATE_PUBLIC, null);
                if (c.moveToFirst()) {
                    do {
                        result.add(new TagWorflowItem(c.getLong(0), c.getString(1), TagConverter.fromString(c.getString(2)),
                                c.getLong(3), null));
                    } while (c.moveToNext());
                }
            } catch (Exception ex) {
                Log.e(Global.LOG_CONTEXT, "TagSql.loadTagWorflowItems(): error executing " + query, ex);
            } finally {
                if (c != null) c.close();
            }
        } else {
            Log.e(Global.LOG_CONTEXT, "TagSql.loadTagWorflowItems(): error no items because no filter in " + query);
        }
        return result;
    }

    static class TagWorflowItem {
        public final long id;
        public final long xmpLastModifiedDate;
        public final String path;
        public final List<String> tags;
        public boolean xmpMoreRecentThanSql = false;
        public final IFile file;

        public TagWorflowItem(long id, String path, List<String> tags, long xmpLastModifiedDate, IFile file) {
            this.tags = tags;
            this.path = path;
            this.id = id;
            this.xmpLastModifiedDate = xmpLastModifiedDate;
            this.file = file;
        }
    }

}
