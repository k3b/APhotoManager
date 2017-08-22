/*
 * Copyright (c) 2016-2017 by k3b.
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

package de.k3b.tagDB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.k3b.io.StringUtils;

/**
 * Represents a possible tag or keyword that can be attached to an image.
 *
 * Created by k3b on 04.10.2016.
 */

public class Tag{
    public static final Comparator<Tag> COMPARATOR_NAME_IGNORE_CASE = new Comparator<Tag>() {
        @Override
        public int compare(Tag lhs, Tag rhs) {
            String lhsName = (lhs == null) ? null : lhs.getName();
            String rhsName = (rhs == null) ? null : rhs.getName();
            if (lhsName == null) return (rhsName == null) ? 0 : -1;
            return lhsName.compareToIgnoreCase(rhsName);
        }
    };

    public static final Comparator<Tag> COMPARATOR_HIERARCHY = new Comparator<Tag>() {
        @Override
        public int compare(Tag lhs, Tag rhs) {
            String lhsName = (lhs == null) ? null : lhs.getPath();
            String rhsName = (rhs == null) ? null : rhs.getPath();
            if (lhsName == null) return (rhsName == null) ? 0 : -1;
            return lhsName.compareToIgnoreCase(rhsName);
        }
    };

    private String name;
    private Tag parent;

    public String getName() {
        return name;
    }
    public Tag setName(String name) {
        this.name = name;
        return this;
    }

    public Tag getParent() {
        return parent;
    }

    public Tag setParent(Tag parent) {
        Tag cur = parent;
        while (cur != null) {
            if (parent == this) return this; // do not allow recursion
            cur = cur.getParent();
        }

        // there is no recursion
        this.parent = parent;
        return this;
    }

    @Override
    public String toString() {
        return getName();
    }

    public Tag fromString(String line) {
        setName(line);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (name == null) return false;
        if (o instanceof String) {
            return name.equalsIgnoreCase((String)o);
        }
        if (!(o instanceof Tag)) return false;
        Tag other = (Tag) o;
        if (name.equalsIgnoreCase(other.name)) {
            if (this.parent == null) return other.parent == null;
            return parent.equals(other.parent);
        }
        return false;
        // return name.equals(((Tag)o).name);
    }

    /** return item as path where parents are appended.
     * child <- parent <- grandparent */
    public String getReversePath() {
        StringBuilder result = new StringBuilder();
        Tag cur = this;
        while (cur != null) {
            if (result.length() > 0) result.append(" <- ");
            result.append(cur.getName());
            cur = cur.getParent();
        }
        return result.toString();
    }

    /** return item as path where parents are prependet.
     * /grandparent/parent/child */
    public String getPath() {
        StringBuilder result = new StringBuilder();
        Tag cur = this;
        while (cur != null) {
            if (result.length() > 0) result.insert(0,"/");
            result.insert(0,cur.getName());
            cur = cur.getParent();
        }
		
		// leading "/"
		result.insert(0,"/");
        return result.toString();
    }

    public int getParentCount() {
        int result = -1;
        Tag cur = this;
        while (cur != null) {
            result++;
            cur = cur.getParent();
        }
        return result;
    }

    public List<Tag> getChildren(List<Tag> all, boolean recursive, boolean includeThis) {
        return getChildren(new ArrayList<Tag>(), all, recursive, includeThis);
    }

    private List<Tag> getChildren(List<Tag> result, List<Tag> all, boolean recursive, boolean includeThis) {
        if (includeThis) result.add(this);
        if (all != null) {
            for(Tag candidate : all) {
                if ((candidate != null) && (candidate.parent == this)) {
                    result.add(candidate);
                    if (recursive) candidate.getChildren(result, all, true, includeThis);
                }
            }
        }
        if (result.size() == 0) return null;
        return result;
    }

    public static Tag findFirstChildByName(List<Tag> all, Tag parent, String name) {
        if (all != null) {
            for(Tag candidate : all) {
                if ((candidate != null) && (candidate.parent == parent) && name.equals(candidate.getName())) {
					return candidate;
                }
            }
        }
        return null;
    }

    public static Tag findByPath(List<Tag> all, Tag parent, String path) {
        return findByPathElements(all, parent, TagExpression.getPathElemens(path));
    }

    public static Tag findByPathElements(List<Tag> all, Tag parent, String... pathElements) {
        if (pathElements != null) {
            boolean isRoot = (pathElements.length > 0) && (StringUtils.isNullOrEmpty(pathElements[0]));
            Tag currentTagParent = isRoot ? null : parent;
            for (String pathElement : pathElements) {
                if (pathElement != null) {
                    pathElement = pathElement.trim();
                    if (pathElement.length() > 0) {
                        Tag tag = Tag.findFirstChildByName(all, currentTagParent, pathElement);
                        if (tag == null) return null;
                        currentTagParent = tag;
                    }
                }
            }
            return currentTagParent;
        }
        return null;
    }

    public int delete(List<Tag> all, boolean recursive) {
        int result = 0;
        if (all != null) {
            List<Tag> children = getChildren(all, false, false);
            if (children != null) {
                for (Tag child : children) {
                    if (child != null) {
                        if (recursive) {
                            result += child.delete(all, recursive);
                        } else {
                            child.parent = this.parent;
                        }
                    }
                }
            }

            if (all.contains(this)) {
                all.remove(this);
                result++;
            }
        }
        return result;
    }
}
