package comics.utils;

import org.junit.jupiter.api.Test;
import shell.OSDetection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static comics.utils.Tools.sandbox;
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
            var sb = sandbox();
            sb.runTest((File sandbox) -> {
                of("test.cbz", "test.cbr").forEach(file -> {
                    // Try to move something to the backup directory
                    var targetFile = sb.copyResource("/compressed/" + file, file);
                    assertTrue(targetFile.exists());
                    var originalHashCbz = Tools.md5(targetFile);
                    // By running inside runTest, backup service has been rigged to write everything inside the test sandbox
                    try { new BackupService().backupFile(targetFile); } catch (IOException ioe) { fail(ioe); }
                    assertFalse(targetFile.exists());
                    var today = Tools.today();
                    var backupFile = new File(sandbox, String.format(".comicutils/%s/" + file, today));
                    assertTrue(backupFile.exists());
                    assertFalse(backupFile.isDirectory());
                    assertFalse(Files.isSymbolicLink(backupFile.toPath()));
                    assertEquals(originalHashCbz, Tools.md5(backupFile));
                });
            });
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void cannotCreateBackupDir() {
        sandbox().runTest((File sandbox) -> {
            File bogus = new File(sandbox, ".comicutils");
            bogus.createNewFile();
            File realFile = new File(sandbox, "something");
            realFile.createNewFile();
            assertThrowsExactly(IOException.class, () -> new BackupService().backupFile(realFile));
        });
    }

    @Test
    public void testErrorCases() {
        sandbox().runTest((File sandbox) -> {
            assertThrowsExactly(AssertionError.class, () -> new BackupService().backupFile(null));
            assertThrowsExactly(AssertionError.class, () -> new BackupService().backupFile(sandbox));
            assertThrowsExactly(AssertionError.class, () -> new BackupService().backupFile(new File(sandbox, "does not exist")));
            if (!OSDetection.isWindows()) {
                // This requires admin permissions in windows T_T
                File realFile = new File(sandbox, "realFile");
                realFile.createNewFile();
                Files.write(realFile.toPath(), "trololo".getBytes("UTF-8"));
                File symlink = new File(sandbox, "symlink");
                Files.createSymbolicLink(symlink.toPath(), realFile.toPath());
                assertThrowsExactly(AssertionError.class, () -> new BackupService().backupFile(symlink));
            }
        });
    }
}
