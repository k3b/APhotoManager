package de.k3b.android.util;

import java.util.Date;
import java.util.List;

import de.k3b.media.IMetaApi;

/**
 * Created by k3b on 09.10.2016.
 */

public class MetaApiWrapper implements IMetaApi {
    private final IMetaApi child;

    private String filePath;
    private String title;
    private String description;
    private List<String> tags;

    public MetaApiWrapper(IMetaApi child) {

        this.child = child;
    }
    @Override
    public Date getDateTimeTaken() {
        return child.getDateTimeTaken();
    }

    @Override
    public MetaApiWrapper setDateTimeTaken(Date value) {
        child.setDateTimeTaken(value);
        return this;
    }

    @Override
    public MetaApiWrapper setLatitude(Double latitude) {
        child.setLatitude(latitude);
        return this;
    }

    @Override
    public MetaApiWrapper setLongitude(Double longitude) {
        child.setLongitude(longitude);
        return this;
    }

    @Override
    public Double getLatitude() {
        return child.getLatitude();
    }

    @Override
    public Double getLongitude() {
        return child.getLongitude();
    }

    public String getTitle() {
        return child.getTitle();
    }

    public MetaApiWrapper setTitle(String title) {
        child.setTitle(title);
        return this;
    }

    public String getDescription() {
        return child.getDescription();
    }

    public MetaApiWrapper setDescription(String description) {
        child.setDescription(description);
        return this;
    }

    public List<String> getTags() {
        return child.getTags();
    }

    public MetaApiWrapper setTags(List<String> tags) {
        child.setTags(tags);
        return this;
    }

    public String getPath() {
        return child.getPath();
    }

    public MetaApiWrapper setPath(String filePath) {
        child.setPath(filePath);
        return this;
    }
}
