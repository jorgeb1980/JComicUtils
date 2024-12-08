package comics.logic;

import comics.commands.UnpackCommand;
import comics.utils.Tools.TestLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import shell.OSDetection;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static comics.utils.Tools.TestLevel.COMMAND;
import static comics.utils.Tools.TestLevel.SERVICE;
import static comics.utils.Tools.createNewFile;
import static comics.utils.Tools.md5;
import static comics.utils.Tools.mkdir;
import static comics.utils.Tools.sandbox;
import static comics.utils.Tools.today;
import static comics.utils.Utils.emptyIfNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test.CaptureOutput.captureOutput;

public class TestUnpack {

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
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var comicFile = sb.copyResource("/compressed/test." + extension, "test." + extension);
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
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var targetFile = sb.copyResource("/compressed/test.cbr", "test.cbr");
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
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            sb.copyResource("/compressed/test.cbz", "test.cbz");
            // Try to unpack it but the file name is taken
            var obstacle = new File(sandbox, "test");
            createNewFile(obstacle);
            var ctx = captureOutput(() -> {
                var command = new UnpackCommand();
                command.setDisableProgressBar(true);
                command.run(sandbox.toPath());
            });
            assertTrue(ctx.out().contains("something in the way"));
        });
    }

    @Test
    public void testCommandErrorNullDirectory() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var ctx = captureOutput(() -> {
                var command = new UnpackCommand();
                command.setDisableProgressBar(true);
                var result = command.run(null);
                assertNotEquals(0, result);
            });
            assertTrue(ctx.err().contains("run the command on a non-null directory"));
        });
    }

    @Test
    public void testServiceErrors() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var service = new CompressionService();
            assertThrowsExactly(CompressionException.class, () -> service.decompressComic(null));
            // Existing file
            var comicFile = sb.copyResource("/compressed/test.cbz", "test.cbz");
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

    private void checkDirectory(File f) {
        assertTrue(f.exists());
        assertTrue(f.isDirectory());
    }

    private void checkChildrenFiles(File dir, List<String> children) {
        children.forEach(f -> {
            var file = new File(dir, f);
            assertTrue(file.exists());
            assertFalse(file.isDirectory());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "cbr", "cbz" })
    public void testDirectoryHierarchy(String extension) {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var targetFile = sb.copyResource(
                "/compressed/test_with_directories." + extension,
                "test_with_directories." + extension
            );
            var command = new UnpackCommand();
            command.setDisableProgressBar(true);
            command.run(sandbox.toPath());
            assertFalse(targetFile.exists());
            File newDirectory = new File(sandbox, "test_with_directories");
            checkDirectory(newDirectory);
            // Contents
            var dir1 = new File(newDirectory, "dir1");
            var dir2 = new File(newDirectory, "dir2");
            checkDirectory(dir1);
            checkDirectory(dir2);
            // Base directory has no files
            assertTrue(Arrays.stream(emptyIfNull(newDirectory.listFiles())).filter(f -> !f.isDirectory()).toList().isEmpty());
            var childrenDir1 = List.of("down.jpg", "left.jpg", "foo.txt", "bar.txt", "baz.txt");
            var childrenDir2 = List.of("right.jpg", "up.jpg", "should_not_be_here.xml", "thumbs.db");
            checkChildrenFiles(dir1, childrenDir1);
            checkChildrenFiles(dir2, childrenDir2);
        });
    }
}
