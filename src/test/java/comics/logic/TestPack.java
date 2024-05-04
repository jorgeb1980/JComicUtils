package comics.logic;

import comics.commands.PackCommand;
import comics.utils.TestUtils.TestLevel;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import static comics.utils.TestUtils.TestLevel.COMMAND;
import static comics.utils.TestUtils.TestLevel.SERVICE;
import static comics.utils.TestUtils.copyResource;
import static comics.utils.TestUtils.md5;
import static comics.utils.TestUtils.runTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPack {

    @ParameterizedTest
    @EnumSource(TestLevel.class)
    public void testPackNoExclusions(TestLevel level) {
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
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.compressComic(childDirectory);
            } else if (level == COMMAND) {
                var packCommand = new PackCommand();
                packCommand.setAll(true);
                var ret = packCommand.run(directory.toPath());
                assertEquals(0, ret);
            }
            var comicFile = new File(directory, fileName + ".cbz");
            assertTrue(comicFile.exists());
            assertFalse(childDirectory.exists());
            // Extract and check
            new CompressionService().decompressComic(comicFile);
            assertFalse(comicFile.exists());
            assertTrue(childDirectory.exists());
            assertEquals(originalData.keySet().size(), childDirectory.listFiles().length);
            for (File f: childDirectory.listFiles()) {
                assertEquals(originalData.get(f.getName()), md5(f));
            }
        });
    }

    @ParameterizedTest
    @EnumSource(TestLevel.class)
    public void testPackWithExclusions(TestLevel level) {
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
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.compressComic(childDirectory, "txt");
            } else if (level == COMMAND) {
                var packCommand = new PackCommand();
                var ret = packCommand.run(directory.toPath());
                assertEquals(0, ret);
            }
            var comicFile = new File(directory, fileName + ".cbz");
            assertTrue(comicFile.exists());
            assertFalse(childDirectory.exists());
            // Extract and check
            new CompressionService().decompressComic(comicFile);
            assertFalse(comicFile.exists());
            assertTrue(childDirectory.exists());
            assertEquals(
                originalData.keySet().stream().filter(s -> !s.endsWith("txt")).toList().size(),
                childDirectory.listFiles().length
            );
            assertTrue(Arrays.stream(childDirectory.listFiles()).filter(s -> s.getName().endsWith("txt")).toList().isEmpty());
            for (File f: childDirectory.listFiles()) {
                assertEquals(originalData.get(f.getName()), md5(f));
            }
        });
    }

    @ParameterizedTest
    @EnumSource(TestLevel.class)
    public void testPackEmpty(TestLevel level) {
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
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.compressComic(childDirectory);
            } else if (level == COMMAND) {
                var packCommand = new PackCommand();
                var ret = packCommand.run(directory.toPath());
                assertEquals(0, ret);
            }
            var comicFile = new File(directory, fileName + ".cbz");
            assertTrue(comicFile.exists());
            assertFalse(childDirectory.exists());
            // Extract and check
            new CompressionService().decompressComic(comicFile);
            assertFalse(comicFile.exists());
            // Nothing created for an empty 7z
        });
    }
}
