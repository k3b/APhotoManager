package de.k3b.android.androFotoFinder.backup;

import android.content.Context;
import android.os.AsyncTask;

import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipStorage;

public class BackupAsyncTask extends AsyncTask<Object, String, Object> {
    private final Backup2ZipService service;

    public BackupAsyncTask(Context context, ZipConfigDto mZipConfigData, ZipStorage zipStorage) {
        this.service = new Backup2ZipService(context, mZipConfigData, zipStorage, null);
    }

    @Override
    protected Object doInBackground(Object... voids) {
        return null;
    }
}
