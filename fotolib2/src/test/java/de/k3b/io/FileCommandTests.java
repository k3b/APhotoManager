package de.k3b.io;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.mockito.Mockito.*;
/**
 * Created by k3b on 06.08.2015.
 */
public class FileCommandTests {
    private static final File X_FAKE_OUTPUT_DIR = new File("x:/fakeOutputDir");
    FileCommands sut;
    @Before
    public void setup() {
        sut = spy(new FileCommands(null));
        doReturn(true).when(sut).createDirIfNeccessary(any(File.class));
        doNothing().when(sut).osFileMoveOrCopy(anyBoolean(), any(File.class), any(File.class));
    }

    @Test
    public void shouldCopy() {
        registerFakeFilesInDestDir(sut);
        sut.copyFilesTo(false, X_FAKE_OUTPUT_DIR, createTestFiles("a.jpg"));

        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "/a.jpg"), new File("a.jpg"));

    }

    @Test
    public void shouldCopyWitRenameExistingMultible() {
        registerFakeFilesInDestDir(sut, "a.jpg", "b.png", "b(1).png");
        sut.copyFilesTo(false, X_FAKE_OUTPUT_DIR, createTestFiles("a.jpg", "b.png"));

        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "a(1).jpg"), new File("a.jpg"));
        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "b(2).png"), new File("b.png"));
    }

    @Test
    public void shouldCopyRenameExistingWithXmp() {
        registerFakeFilesInDestDir(sut, "a.jpg", "a(1).xmp", "a(2).jpg"); // a(3) is next possible
        sut.copyFilesTo(false, X_FAKE_OUTPUT_DIR, createTestFiles("a.jpg", "a.xmp"));

        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "a(3).jpg"), new File("a.jpg"));
        verify(sut).osFileMoveOrCopy(false, new File(X_FAKE_OUTPUT_DIR, "a(3).xmp"), new File("a.xmp"));
    }

    private static void registerFakeFilesInDestDir(FileCommands sut, String... filenames) {
        if (filenames.length == 0) {
            doReturn(false).when(sut).osFileExists(any(File.class));
        } else {
            for (String filename : filenames) {
                doReturn(true).when(sut).osFileExists(new File(X_FAKE_OUTPUT_DIR, filename));
            }
        }
    }

    static File[] createTestFiles(String... files) {
        File[] result = new File[files.length];
        int pos = 0;
        for (String file : files) {
            result[pos++] = new File(file);
        }
        return result;
    }
}
