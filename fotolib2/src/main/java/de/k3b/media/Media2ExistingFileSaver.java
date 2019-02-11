package de.k3b.media;

import java.io.File;

import de.k3b.io.FileProcessor;
import de.k3b.io.IItemSaver;

public class Media2ExistingFileSaver implements IItemSaver<IMetaApi> {
    private final IItemSaver<File> fileSaver;
    public Media2ExistingFileSaver(IItemSaver<File> fileSaver) {
        this.fileSaver = fileSaver;
    }

    @Override
    public boolean save(IMetaApi item) {
        if (item != null) {
            String path = item.getPath();
            if (path != null) {
                return saveFiles(new File(path),
                        FileProcessor.getExistingSidecarOrNull(path, true),
                        FileProcessor.getExistingSidecarOrNull(path, false)) > 0;
            }
        }
        return false;
    }

    private int saveFiles(File... files) {
        int processed = 0;
        for (File f: files) {
            if ((f != null) && (f.exists()) && f.canRead() && this.fileSaver.save(f))  processed++;
        }
        return processed;
    }
}
