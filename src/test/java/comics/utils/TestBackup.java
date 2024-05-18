package comics.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import shell.OSDetection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static comics.utils.Tools.sandbox;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.YEAR;
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

    @Test
    public void testCleanup() {
        sandbox().runTest((File sandbox) -> {
            var sdf = new SimpleDateFormat("yyyy-MM-dd");

            var cal1 = Calendar.getInstance();
            cal1.add(YEAR, -1);
            var aYearAgo = cal1.getTime();
            var cal2 = Calendar.getInstance();
            cal2.add(DAY_OF_YEAR, -8);
            var eightDaysAgo = cal2.getTime();
            var cal3 = Calendar.getInstance();
            cal3.add(DAY_OF_YEAR, -6);
            var sixDaysAgo = cal3.getTime();

            var backupDir = new File(sandbox, ".comicutils");
            List.of(aYearAgo, eightDaysAgo, sixDaysAgo).forEach(d ->
                new File(backupDir, sdf.format(d)).mkdirs()
            );
            List.of(aYearAgo, eightDaysAgo, sixDaysAgo).forEach(d ->
                Assertions.assertTrue(new File(backupDir, sdf.format(d)).exists())
            );
            // This should clean the backup directory
            new BackupService();
            assertFalse(new File(backupDir, sdf.format(aYearAgo)).exists());
            assertFalse(new File(backupDir, sdf.format(eightDaysAgo)).exists());
            assertTrue(new File(backupDir, sdf.format(sixDaysAgo)).exists());
        });
    }
}
