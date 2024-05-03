package comics.logic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static comics.logic.TestUtils.copyResource;
import static comics.logic.TestUtils.md5;
import static comics.logic.TestUtils.runTest;
import static comics.logic.TestUtils.today;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestBackup {

    @Test
    public void testBackup() {
        try {
            runTest((File directory) -> {
                // Try to move something to the backup directory
                var targetFile = new File(directory, "test.cbz");
                copyResource("/compressed/test.cbz", targetFile);
                assertTrue(targetFile.exists());
                var originalHash = md5(targetFile);
                new BackupService(directory).backupFile(targetFile);
                assertFalse(targetFile.exists());
                var today = today();
                var backupFile = new File(directory, String.format(".comicutils/%s/test.cbz", today));
                assertTrue(backupFile.exists());
                assertFalse(backupFile.isDirectory());
                assertFalse(Files.isSymbolicLink(backupFile.toPath()));
                assertEquals(originalHash, md5(backupFile));
            });
        } catch (Exception e) {
            fail(e);
        }
    }
}
