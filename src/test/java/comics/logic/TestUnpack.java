package comics.logic;

import comics.commands.UnpackCommand;
import comics.utils.Tools.TestLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import shell.OSDetection;

import java.io.File;
import java.nio.file.Files;

import static comics.utils.Tools.TestLevel.COMMAND;
import static comics.utils.Tools.TestLevel.SERVICE;
import static comics.utils.Tools.captureStdOutput;
import static comics.utils.Tools.copyResource;
import static comics.utils.Tools.createNewFile;
import static comics.utils.Tools.md5;
import static comics.utils.Tools.mkdir;
import static comics.utils.Tools.runTest;
import static comics.utils.Tools.today;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    private void checkFile(File f) {
        assertTrue(f.exists());
        assertFalse(f.isDirectory());
        assertFalse(Files.isSymbolicLink(f.toPath()));
    }

    private void testUnpack(TestLevel level, String extension) {
        runTest((File sandbox) -> {
            var comicFile = new File(sandbox, "test." + extension);
            copyResource("/compressed/test." + extension, comicFile);
            var originalMd5 = md5(comicFile);
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.decompressComic(comicFile);
            } else if (level == COMMAND) {
                var command = new UnpackCommand();
                command.setDisableProgressBar(true);
                var ret = command.run(sandbox.toPath());
                assertEquals(0, ret);
            }
            // Check correction of target
            var targetDirectory = new File(sandbox, "test");
            assertTrue(targetDirectory.exists());
            assertTrue(targetDirectory.isDirectory());
            var files = targetDirectory.listFiles();
            assertNotNull(files);
            assertEquals(9, files.length);
            // Check foo.txt, bar.txt, baz.txt
            for (var s: new String[]{"foo", "bar", "baz"}) {
                var f = new File(targetDirectory, s + ".txt");
                checkFile(f);
                assertEquals(s, Files.readString(f.toPath()).trim());
            }
            // Check up.jpg, right.jpg, down.jpg, left.jpg
            for (var s: new String[]{"up", "right", "down", "left"}) {
                checkFile(new File(targetDirectory, s + ".jpg"));
            }
            // Check for the xml and db files
            checkFile(new File(targetDirectory, "should_not_be_here.xml"));
            checkFile(new File(targetDirectory, "thumbs.db"));
            // Also check that the original comic has been backed up
            assertFalse(comicFile.exists());
            var backup = new File(sandbox, String.format(".comicutils/%s/test.%s", today(), extension));
            assertTrue(backup.exists());
            assertEquals(originalMd5, md5(backup));
        });
    }

    @Test
    public void testCommandErrorExistingDirectory() {
        runTest((File sandbox) -> {
            var targetFile = new File(sandbox, "test.cbr");
            copyResource("/compressed/test.cbr", targetFile);
            var obstacle = new File(sandbox, "test");
            assertFalse(obstacle.exists());
            mkdir(obstacle);
            assertTrue(obstacle.exists());
            assertTrue(obstacle.isDirectory());
            var compressionService = new CompressionService();
            // Try to decompress into an existing directory
            assertThrowsExactly(CompressionException.class, () -> compressionService.decompressComic(targetFile));
            // We should have not moved the file into a backup directory
            assertTrue(targetFile.exists());
        });
    }

    @Test
    public void testCommandErrorExistingFile() {
        runTest((File sandbox) -> {
            var comicFile = new File(sandbox, "test.cbz");
            copyResource("/compressed/test.cbz", comicFile);
            // Try to unpack it but the file name is taken
            var obstacle = new File(sandbox, "test");
            createNewFile(obstacle);
            var standardOutput = captureStdOutput(() -> {
                var command = new UnpackCommand();
                command.setDisableProgressBar(true);
                command.run(sandbox.toPath());
            });
            assertTrue(standardOutput.contains("something in the way"));
        });
    }

    @Test
    public void testCommandErrorNullDirectory() {
        runTest((File sandbox) -> {
            var standardOutput = captureStdOutput(() -> {
                var command = new UnpackCommand();
                command.setDisableProgressBar(true);
                var result = command.run(null);
                assertNotEquals(0, result);
            });
            assertTrue(standardOutput.contains("run the command on a non-null directory"));
        });
    }

    @Test
    public void testServiceErrors() {
        runTest((File sandbox) -> {
            var service = new CompressionService();
            assertThrowsExactly(CompressionException.class, () -> service.decompressComic(null));
            // Existing file
            var comicFile = new File(sandbox, "test.cbz");
            copyResource("/compressed/test.cbz", comicFile);
            var obstacle = new File(sandbox, "test");
            createNewFile(obstacle);
            assertThrowsExactly(CompressionException.class, () -> service.decompressComic(comicFile));
            // Decompress a directory
            var newDir = new File(sandbox, "childDir");
            mkdir(newDir);
            assertThrowsExactly(CompressionException.class, () -> service.decompressComic(newDir));
            // non existing file
            assertThrowsExactly(CompressionException.class, () -> service.decompressComic(new File(sandbox, "lalala")));
            if (!OSDetection.isWindows()) {
                var symlink = new File(sandbox, "symlink");
                Files.createSymbolicLink(symlink.toPath(), comicFile.toPath());
                assertThrowsExactly(CompressionException.class, () -> service.decompressComic(symlink));
            }
        });
    }
}
