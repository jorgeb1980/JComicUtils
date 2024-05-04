package comics.logic;

import comics.commands.RepackCommand;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static comics.utils.Tools.copyResource;
import static comics.utils.Tools.runTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRepack {

    @Test
    public void testCommandRegularExecution() {
        runTest((File directory) -> {
            var comicFile = new File(directory, "Another test 456.cbr");
            copyResource("/compressed/test.cbr", comicFile);
            var command = new RepackCommand();
            command.execute(directory.toPath());
            assertFalse(comicFile.exists());
            var targetFile = new File(directory, "Another Test - 456.cbz");
            assertTrue(targetFile.exists());
            // unpack and check that there are no .txt files in the target file
            var compressionService = new CompressionService();
            compressionService.decompressComic(targetFile);
            var newDir = new File(directory, "Another Test - 456");
            assertTrue(newDir.exists());
            assertTrue(newDir.isDirectory());
            var expectedFiles = List.of("up.jpg", "right.jpg", "down.jpg", "left.jpg");
            var children = Arrays.asList(newDir.listFiles()).stream().map(File::getName).toList();
            assertEquals(expectedFiles.size(), children.size());
            assertTrue(children.stream().filter(s -> s.endsWith(".txt")).toList().isEmpty());
            expectedFiles.forEach(f -> assertTrue(children.contains(f)));
        });
    }

    @Test
    public void testCommandIncludeAll() {
        runTest((File directory) -> {
            var comicFile = new File(directory, "test 1.cbr");
            copyResource("/compressed/test.cbr", comicFile);
            var command = new RepackCommand();
            command.setAll(true);
            command.execute(directory.toPath());
            assertFalse(comicFile.exists());
            var targetFile = new File(directory, "Test - 1.cbz");
            assertTrue(targetFile.exists());
            // unpack and check that there are no .txt files in the target file
            var compressionService = new CompressionService();
            compressionService.decompressComic(targetFile);
            var newDir = new File(directory, "Test - 1");
            assertTrue(newDir.exists());
            assertTrue(newDir.isDirectory());
            var expectedFiles = List.of(
                "up.jpg", "right.jpg", "down.jpg", "left.jpg",
                "foo.txt", "bar.txt", "baz.txt"
            );
            var children = Arrays.asList(newDir.listFiles()).stream().map(File::getName).toList();
            assertEquals(expectedFiles.size(), children.size());
            expectedFiles.forEach(f -> assertTrue(children.contains(f)));
        });
    }
}
