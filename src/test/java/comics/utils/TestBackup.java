package comics.utils;

import org.junit.jupiter.api.Test;
import shell.OSDetection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static comics.utils.TestUtils.copyResource;
import static comics.utils.TestUtils.runTest;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestBackup {

    @Test
    public void testBackup() {
        try {
            runTest((File directory) -> {
                of("test.cbz", "test.cbr").forEach(file -> {
                    // Try to move something to the backup directory
                    var targetFile = new File(directory, file);
                    copyResource("/compressed/" + file, targetFile);
                    assertTrue(targetFile.exists());
                    var originalHashCbz = TestUtils.md5(targetFile);
                    try { new BackupService(directory).backupFile(targetFile); } catch (IOException ioe) { fail(ioe); }
                    assertFalse(targetFile.exists());
                    var today = TestUtils.today();
                    var backupFile = new File(directory, String.format(".comicutils/%s/" + file, today));
                    assertTrue(backupFile.exists());
                    assertFalse(backupFile.isDirectory());
                    assertFalse(Files.isSymbolicLink(backupFile.toPath()));
                    assertEquals(originalHashCbz, TestUtils.md5(backupFile));
                });
            });
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void cannotCreateBackupDir() {
        runTest((File directory) -> {
            File bogus = new File(directory, ".comicutils");
            bogus.createNewFile();
            File realFile = new File(directory, "something");
            realFile.createNewFile();
            assertThrowsExactly(IOException.class, () -> new BackupService(directory).backupFile(realFile));
        });
    }

    @Test
    public void testErrorCases() {
        runTest((File directory) -> {
            assertThrowsExactly(AssertionError.class, () -> new BackupService(directory).backupFile(null));
            assertThrowsExactly(AssertionError.class, () -> new BackupService(directory).backupFile(directory));
            assertThrowsExactly(AssertionError.class, () -> new BackupService(directory).backupFile(new File(directory, "does not exist")));
            if (!OSDetection.isWindows()) {
                // This requires admin permissions in windows T_T
                File realFile = new File(directory, "realFile");
                realFile.createNewFile();
                Files.write(realFile.toPath(), "trololo".getBytes("UTF-8"));
                File symlink = new File(directory, "symlink");
                Files.createSymbolicLink(symlink.toPath(), realFile.toPath());
                assertThrowsExactly(AssertionError.class, () -> new BackupService(directory).backupFile(symlink));
            }
        });
    }
}
