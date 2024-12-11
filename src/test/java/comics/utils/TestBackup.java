package comics.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import test.Sandbox;
import test.sandbox.SandboxTest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static comics.utils.Tools.sandbox;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.YEAR;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;

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

    @SandboxTest
    @EnabledOnOs({ LINUX, MAC })
    public void testErrorCases(Sandbox sb) throws IOException {
        var rootDir = sb.getSandbox();
        // This requires admin permissions in windows T_T
        File realFile = new File(rootDir, "realFile");
        realFile.createNewFile();
        Files.write(realFile.toPath(), "trololo".getBytes(StandardCharsets.UTF_8));
        File symlink = new File(rootDir, "symlink");
        Files.createSymbolicLink(symlink.toPath(), realFile.toPath());
        assertThrowsExactly(AssertionError.class, () -> new BackupService().backupFile(symlink));
    }

    @SandboxTest
    public void testErrorCasesBasic(Sandbox sb) {
        var rootDir = sb.getSandbox();
        assertThrowsExactly(AssertionError.class, () -> new BackupService().backupFile(null));
        assertThrowsExactly(AssertionError.class, () -> new BackupService().backupFile(rootDir));
        assertThrowsExactly(AssertionError.class, () -> new BackupService().backupFile(new File(rootDir, "does not exist")));
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
