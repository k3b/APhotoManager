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
