package de.k3b.io.filefacade;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import de.k3b.io.IFileCommandLogger;

/**
 * A {@link IFileCommandLogger ) that logs to {@link IFile}
 */
public class FileFacadeLogger implements IFileCommandLogger {
    final PrintWriter mLogFile;

    public FileFacadeLogger(IFile file) throws FileNotFoundException {
        mLogFile = new PrintWriter(file.openOutputStream());
    }

    @Override
    public IFileCommandLogger log(Object... messages) {
        if (mLogFile != null) {
            for (Object message : messages) {
                if (message != null) mLogFile.print(message);
            }
            mLogFile.println();
            mLogFile.flush();
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        mLogFile.close();
    }
}
