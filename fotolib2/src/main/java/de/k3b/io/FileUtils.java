package de.k3b.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.k3b.FotoLibGlobal;


/**
 * Created by k3b on 06.10.2015.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FotoLibGlobal.LOG_TAG);

    public static String readFile(File file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        br.close();
        return sb.toString();
    }

    /** tryGetCanonicalFile without exception */
    public static File tryGetCanonicalFile(String path) {
        if (path == null) return null;

        final File file = new File(path);
        return tryGetCanonicalFile(file, file);
    }

    /** tryGetCanonicalFile without exception */
    public static File tryGetCanonicalFile(File file, File errorValue) {
        if (file == null) return null;

        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            if (FotoLibGlobal.debugEnabled) {
                logger.warn("Error tryGetCanonicalFile('" + file.getAbsolutePath() + "') => '" + errorValue + "' exception " + ex.getMessage(), ex);
            }
            return errorValue;
        }
    }

    /** tryGetCanonicalFile without exception */
    public static String tryGetCanonicalPath(File file, String errorValue) {
        if (file == null) return null;

        try {
            return file.getCanonicalPath();
        } catch (IOException ex) {
            if (FotoLibGlobal.debugEnabled) {
                logger.warn("Error tryGetCanonicalPath('" + file.getAbsolutePath() + "') => '" + errorValue + "' exception " + ex.getMessage(), ex);
            }
            return errorValue;
        }
    }

    /** @return true if directory is an alias of an other (symlink-dir). */
	public static  boolean isSymlinkDir(File directory, boolean errorValue) {
        if (FotoLibGlobal.ignoreSymLinks) {
            return false;
        }

		try {
			// from http://stackoverflow.com/questions/813710/java-1-6-determine-symbolic-links
			boolean result = !directory.getAbsolutePath().equals(directory.getCanonicalPath());
            if (result && FotoLibGlobal.debugEnabled) {
                logger.debug("isSymlinkDir('" + directory.getAbsolutePath() + "') => true because CanonicalPath='" + directory.getCanonicalPath() + "'");
            }
			
			return result;
        } catch (IOException ex) {
            if (FotoLibGlobal.debugEnabled) {
                logger.warn("Error isSymlinkDir('" + directory.getAbsolutePath() + "') exception " + ex.getMessage(), ex);
            }
            return errorValue;
        }
	}
}
