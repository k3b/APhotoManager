package de.k3b.tagDB;

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
}
