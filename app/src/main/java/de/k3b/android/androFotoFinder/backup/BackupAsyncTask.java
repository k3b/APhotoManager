package de.k3b.android.androFotoFinder.backup;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.ProgressBar;

import java.util.concurrent.atomic.AtomicBoolean;

import de.k3b.io.IProgessListener;
import de.k3b.zip.IZipConfig;
import de.k3b.zip.ZipConfigDto;
import de.k3b.zip.ZipStorage;

public class BackupAsyncTask extends AsyncTask<Object, String, IZipConfig> implements IProgessListener {
    private final Backup2ZipService service;
    private ProgressBar mProgressBar = null;
    private AtomicBoolean isActive = new AtomicBoolean(true);

    public BackupAsyncTask(Context context, ZipConfigDto mZipConfigData, ZipStorage zipStorage) {
        this.service = new Backup2ZipService(context.getApplicationContext(),
                mZipConfigData, zipStorage, null);
    }

    public void setContext(ProgressBar progressBar) {
        mProgressBar = progressBar;
        service.setProgessListener((progressBar != null) ? this : null);
    }

    @Override
    protected IZipConfig doInBackground(Object... voids) {
        try {
            return this.service.execute();
        } finally {
            this.isActive.set(false);
        }
    }

    public static boolean isActive(BackupAsyncTask backupAsyncTask) {
        return (backupAsyncTask != null) && (backupAsyncTask.isActive.get());
    }

    /**
     * called every time when command makes some little progress.
     * return true to continue
     */
    @Override
    public boolean onProgress(int itemcount, int size, String message) {
        return !this.isCancelled();
    }
}
