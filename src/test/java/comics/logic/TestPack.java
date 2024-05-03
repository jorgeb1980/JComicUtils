package comics.logic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import static comics.logic.TestUtils.copyResource;
import static comics.logic.TestUtils.md5;
import static comics.logic.TestUtils.runTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPack {

    @Test
    public void testPackNoExclusions() {
        runTest((File directory) -> {
            final var fileName = "some comic";
            var childDirectory = new File(directory, fileName);
            childDirectory.mkdirs();
            // Populate the directory
            for (var s: Arrays.asList(
                "foo.txt",
                "bar.txt",
                "baz.txt",
                "up.jpg",
                "right.jpg",
                "down.jpg",
                "left.jpg"
            )) copyResource("/uncompressed/" + s, new File(childDirectory, s));
            // Remember the directory
            var originalData = new HashMap<String, String>();
            for (File f: childDirectory.listFiles()) {
                originalData.put(f.getName(), md5(f));
            }
            var compressionService = new CompressionService(new BackupService(directory));
            compressionService.compressComic(childDirectory);
            var comicFile = new File(directory, fileName + ".cbz");
            assertTrue(comicFile.exists());
            assertFalse(childDirectory.exists());
            // Extract and check
            compressionService.decompressComic(comicFile);
            assertTrue(childDirectory.exists());
            assertEquals(originalData.keySet().size(), childDirectory.listFiles().length);
            for (File f: childDirectory.listFiles()) {
                assertEquals(originalData.get(f.getName()), md5(f));
            }
        });
    }
}
