package comics.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestBackup {

    @Test
    public void testBackup() {
        try {
            TestUtils.runTest((File directory) -> {
                // Try to move something to the backup directory
                var targetFile = new File(directory, "test.cbz");
                TestUtils.copyResource("/compressed/test.cbz", targetFile);
                assertTrue(targetFile.exists());
                var originalHash = TestUtils.md5(targetFile);
                new BackupService(directory).backupFile(targetFile);
                assertFalse(targetFile.exists());
                var today = TestUtils.today();
                var backupFile = new File(directory, String.format(".comicutils/%s/test.cbz", today));
                assertTrue(backupFile.exists());
                assertFalse(backupFile.isDirectory());
                assertFalse(Files.isSymbolicLink(backupFile.toPath()));
                Assertions.assertEquals(originalHash, TestUtils.md5(backupFile));
            });
        } catch (Exception e) {
            fail(e);
        }
    }
}
