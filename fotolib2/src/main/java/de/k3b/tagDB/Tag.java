/*
 * Copyright (c) 2016 by k3b.
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
import java.util.List;

/**
 * Represents a possible tag or keyword that can be attached to an image.
 *
 * Created by k3b on 04.10.2016.
 */

public class Tag{
    private String name;

    public String getName() {
        return name;
    }

    public Tag setName(String name) {
        this.name = name;
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
        if (!(o instanceof Tag)) return false;
        return name.equals(((Tag)o).name);
    }

    public static List<Tag> toList(String... items) {
        if ((items != null) && (items.length > 0)) {
            List<Tag> result = new ArrayList<>();

            for (String oldItem : items) {
                result.add(new Tag().setName(oldItem));
            }
            return result;
        }
        return null;
    }
}
