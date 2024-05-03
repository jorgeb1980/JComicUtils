package comics.logic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;

import static comics.logic.CompressionUtils.check;
import static comics.logic.CompressionUtils.decompressComic;
import static comics.logic.TestUtils.copyResource;
import static comics.logic.TestUtils.md5;
import static comics.logic.TestUtils.runTest;
import static comics.logic.TestUtils.today;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestCompression {

    @Test
    public void is7zAvailable() {
        assertTrue(check());
    }

    @Test
    public void testUnpackCbz() {
        testUnpack("cbz");
    }

    @Test
    public void testUnpackCbr() {
        testUnpack("cbr");
    }

    private void testUnpack(String extension) {
        try {
            runTest((File directory) -> {
                var comicFile = new File(directory, "test." + extension);
                copyResource("/compressed/test." + extension, comicFile);
                var originalMd5 = md5(comicFile);
                decompressComic(comicFile);
                // Check correction of target
                var targetDirectory = new File(directory, "test");
                assertTrue(targetDirectory.exists());
                assertTrue(targetDirectory.isDirectory());
                var files = targetDirectory.listFiles();
                assertNotNull(files);
                assertEquals(3, files.length);
                // Check foo.txt, bar.txt, baz.txt
                for (var s: new String[]{"foo", "bar", "baz"}) {
                    var f = new File(targetDirectory, s + ".txt");
                    assertTrue(f.exists());
                    assertFalse(f.isDirectory());
                    assertFalse(Files.isSymbolicLink(f.toPath()));
                    assertEquals(s, Files.readString(f.toPath()).trim());
                }
                // Also check that the original comic has been backed up
                assertFalse(comicFile.exists());
                File backup = new File(directory, String.format(".comicutils/%s/test.%s", today(), extension));
                assertTrue(backup.exists());
                assertEquals(originalMd5, md5(backup));
            });
        } catch (Exception e) { fail(e); }
    }

    @Test
    public void testExistingDirectory() {
        try {
            runTest((File directory) -> {
                var targetFile = new File(directory, "test.cbr");
                copyResource("/compressed/test.cbr", targetFile);
                var obstacle = new File(directory, "test");
                assertFalse(obstacle.exists());
                obstacle.mkdir();
                assertTrue(obstacle.exists());
                assertTrue(obstacle.isDirectory());
                // Try to decompress into an existing directory
                assertThrowsExactly(CompressionException.class, () -> CompressionUtils.decompressComic(targetFile));
                // We should have not moved the file into a backup directory
                assertTrue(targetFile.exists());
            });
        } catch (Exception e) { fail(e); }
    }
}
