package de.k3b.media;

import de.k3b.io.FileFacade;
import de.k3b.io.FileProcessor;
import de.k3b.io.IFile;
import de.k3b.io.IItemSaver;

/** Translates every affected file (jpg/xmp) of {@link #save(IPhotoProperties)} to  fileSaver.save(File) */
public class PhotoProperties2ExistingFileSaver implements IItemSaver<IPhotoProperties> {
    private final IItemSaver<IFile> fileSaver;

    public PhotoProperties2ExistingFileSaver(IItemSaver<IFile> fileSaver) {
        this.fileSaver = fileSaver;
    }

    @Override
    public boolean save(IPhotoProperties item) {
        if (item != null) {
            String path = item.getPath();
            if (path != null) {
                return saveFiles(FileFacade.convert(path),
                        FileProcessor.getExistingSidecarOrNull(path, true),
                        FileProcessor.getExistingSidecarOrNull(path, false)) > 0;
            }
        }
        return false;
    }

    private int saveFiles(IFile... files) {
        int processed = 0;
        for (IFile f : files) {
            if ((f != null) && (f.exists()) && f.canRead() && this.fileSaver.save(f)) {
                processed++;
            }
        }
        return processed;
    }
}
