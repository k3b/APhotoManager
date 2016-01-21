package de.k3b.android.androFotoFinder.locationmap.bookmarks;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;

import de.k3b.android.androFotoFinder.R;
import de.k3b.geo.api.IGeoInfoHandler;
import de.k3b.geo.api.IGeoPointInfo;

/**
 * Edit a {@link de.k3b.geo.api.GeoPointDto} item
 *
 * Created by k3b on 23.03.2015.
 */
public class GeoBmpEditDialog extends Dialog implements IGeoInfoHandler, IViewHolder {
    private final IGeoInfoHandler dialogResultConsumer;

    private final Button buttonSave;
    private final Button buttonCancel;

    private GeoBmpDto currentItem = null;

    public GeoBmpEditDialog(Context context, IGeoInfoHandler dialogResultConsumer, int layoutID) {
        super(context);
        this.dialogResultConsumer = dialogResultConsumer;
        this.setContentView(layoutID); // R.layout.geobmp_edit);
        this.buttonSave = (Button) this.findViewById(R.id.cmd_save);
        this.buttonCancel = (Button) this.findViewById(R.id.cmd_cancel);

        this.buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                saveChangesAndExit(GeoBmpEditDialog.this.dialogResultConsumer);
            }

        });
        this.buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                cancel();
            }
        });
    }

    @Override
    public boolean onGeoInfo(IGeoPointInfo geoInfo) {
        this.currentItem = (GeoBmpDto) geoInfo;

        load(this.currentItem);
        return true;
    }

    void load(GeoBmpDto currentItem) {
        GeoBmpBinder.toGui(this, currentItem);
    }

    private void save(GeoBmpDto currentItem) {
        GeoBmpBinder.fromGui(this, currentItem);
    }

    private void saveChangesAndExit(final IGeoInfoHandler owner) {
        save(this.currentItem);
        if (owner != null) {
            owner.onGeoInfo(this.currentItem);
        }
        this.dismiss();
    }
}
