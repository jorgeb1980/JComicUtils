package comics.logic;

import comics.commands.UnpackCommand;
import comics.utils.Tools.TestLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.nio.file.Files;

import static comics.utils.Tools.TestLevel.COMMAND;
import static comics.utils.Tools.TestLevel.SERVICE;
import static comics.utils.Tools.copyResource;
import static comics.utils.Tools.md5;
import static comics.utils.Tools.runTest;
import static comics.utils.Tools.today;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUnpack {

    @Test
    public void is7zAvailable() {
        assertTrue(new CompressionService().check());
    }

    @ParameterizedTest
    @EnumSource(TestLevel.class)
    public void testUnpackCbz(TestLevel level) {
        testUnpack(level, "cbz");
    }

    @ParameterizedTest
    @EnumSource(TestLevel.class)
    public void testUnpackCbr(TestLevel level) {
        testUnpack(level, "cbr");
    }

    private void testUnpack(TestLevel level, String extension) {
        runTest((File directory) -> {
            var comicFile = new File(directory, "test." + extension);
            copyResource("/compressed/test." + extension, comicFile);
            var originalMd5 = md5(comicFile);
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.decompressComic(comicFile);
            } else if (level == COMMAND) {
                var unpackCommand = new UnpackCommand();
                var ret = unpackCommand.run(directory.toPath());
                assertEquals(0, ret);
            }
            // Check correction of target
            var targetDirectory = new File(directory, "test");
            assertTrue(targetDirectory.exists());
            assertTrue(targetDirectory.isDirectory());
            var files = targetDirectory.listFiles();
            assertNotNull(files);
            assertEquals(7, files.length);
            // Check foo.txt, bar.txt, baz.txt
            for (var s: new String[]{"foo", "bar", "baz"}) {
                var f = new File(targetDirectory, s + ".txt");
                assertTrue(f.exists());
                assertFalse(f.isDirectory());
                assertFalse(Files.isSymbolicLink(f.toPath()));
                assertEquals(s, Files.readString(f.toPath()).trim());
            }
            // Check up.jpg, right.jpg, down.jpg, left.jpg
            for (var s: new String[]{"up", "right", "down", "left"}) {
                var f = new File(targetDirectory, s + ".jpg");
                assertTrue(f.exists());
                assertFalse(f.isDirectory());
                assertFalse(Files.isSymbolicLink(f.toPath()));
            }
            // Also check that the original comic has been backed up
            assertFalse(comicFile.exists());
            File backup = new File(directory, String.format(".comicutils/%s/test.%s", today(), extension));
            assertTrue(backup.exists());
            assertEquals(originalMd5, md5(backup));
        });
    }

    @Test
    public void testExistingDirectory() {
        runTest((File directory) -> {
            var targetFile = new File(directory, "test.cbr");
            copyResource("/compressed/test.cbr", targetFile);
            var obstacle = new File(directory, "test");
            assertFalse(obstacle.exists());
            obstacle.mkdir();
            assertTrue(obstacle.exists());
            assertTrue(obstacle.isDirectory());
            var compressionService = new CompressionService();
            // Try to decompress into an existing directory
            assertThrowsExactly(CompressionException.class, () -> compressionService.decompressComic(targetFile));
            // We should have not moved the file into a backup directory
            assertTrue(targetFile.exists());
        });
    }
}
