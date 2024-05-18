package comics.logic;

import comics.commands.PackCommand;
import comics.utils.Tools.TestLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import shell.OSDetection;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static comics.utils.Tools.TestLevel.COMMAND;
import static comics.utils.Tools.TestLevel.SERVICE;
import static comics.utils.Tools.createNewFile;
import static comics.utils.Tools.md5;
import static comics.utils.Tools.mkdir;
import static comics.utils.Tools.sandbox;
import static comics.utils.Utils.emptyIfNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPack {

    @ParameterizedTest
    @EnumSource(TestLevel.class)
    public void testPackNoExclusions(TestLevel level) {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            final var fileName = "some comic";
            var childDirectory = new File(sandbox, fileName);
            mkdir(childDirectory);
            // Populate the directory
            List.of(
                "foo.txt",
                "bar.txt",
                "baz.txt",
                "up.jpg",
                "right.jpg",
                "down.jpg",
                "left.jpg"
            ).forEach(s -> sb.copyResource("/uncompressed/" + s, childDirectory.getName() + "/" + s));
            // Remember the directory
            var originalData = new HashMap<String, String>();
            for (var f : emptyIfNull(childDirectory.listFiles())) {
                originalData.put(f.getName(), md5(f));
            }
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.compressComic(childDirectory);
            } else if (level == COMMAND) {
                var packCommand = new PackCommand();
                packCommand.setAll(true);
                packCommand.setDisableProgressBar(true);
                var ret = packCommand.run(sandbox.toPath());
                assertEquals(0, ret);
            }
            var comicFile = new File(sandbox, new NameConverter().normalizeFileName(fileName + ".cbz"));
            assertTrue(comicFile.exists());
            assertFalse(childDirectory.exists());
            // Extract and check
            new CompressionService().decompressComic(comicFile);
            var newChildDirectory = new File(sandbox, new NameConverter().normalizeFileName(fileName));
            assertFalse(comicFile.exists());
            assertTrue(newChildDirectory.exists());
            assertEquals(originalData.keySet().size(), emptyIfNull(newChildDirectory.listFiles()).length);
            for (var f : emptyIfNull(newChildDirectory.listFiles())) {
                assertEquals(originalData.get(f.getName()), md5(f));
            }
        });
    }

    @ParameterizedTest
    @EnumSource(TestLevel.class)
    public void testPackWithExclusions(TestLevel level) {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            final var fileName = "some comic";
            var childDirectory = new File(sandbox, fileName);
            mkdir(childDirectory);
            // Populate the directory
            List.of(
                "foo.txt",
                "bar.txt",
                "baz.txt",
                "up.jpg",
                "right.jpg",
                "down.jpg",
                "left.jpg"
            ).forEach(s -> sb.copyResource("/uncompressed/" + s, childDirectory.getName() + "/" + s));
            // Remember the directory
            var originalData = new HashMap<String, String>();
            for (var f: emptyIfNull(childDirectory.listFiles())) {
                originalData.put(f.getName(), md5(f));
            }
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.compressComic(childDirectory, "txt");
            } else if (level == COMMAND) {
                var packCommand = new PackCommand();
                packCommand.setDisableProgressBar(true);
                var ret = packCommand.run(sandbox.toPath());
                assertEquals(0, ret);
            }
            var comicFile = new File(sandbox, new NameConverter().normalizeFileName(fileName+ ".cbz"));
            assertTrue(comicFile.exists());
            var newChildDirectory = new File(sandbox, new NameConverter().normalizeFileName(fileName));
            assertFalse(newChildDirectory.exists());
            // Extract and check
            new CompressionService().decompressComic(comicFile);
            assertFalse(comicFile.exists());
            assertTrue(newChildDirectory.exists());
            assertEquals(
                originalData.keySet().stream().filter(s -> !s.endsWith("txt")).toList().size(),
                emptyIfNull(newChildDirectory.listFiles()).length
            );
            assertTrue(Arrays.stream(emptyIfNull(newChildDirectory.listFiles())).filter(s -> s.getName().endsWith("txt")).toList().isEmpty());
            for (var f: emptyIfNull(newChildDirectory.listFiles())) {
                assertEquals(originalData.get(f.getName()), md5(f));
            }
        });
    }

    @ParameterizedTest
    @EnumSource(TestLevel.class)
    public void testPackEmpty(TestLevel level) {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            final var fileName = "some comic";
            var childDirectory = new File(sandbox, fileName);
            mkdir(childDirectory);
            // Populate the directory
            List.of(
                "foo.txt",
                "bar.txt",
                "baz.txt"
            ).forEach(s -> sb.copyResource("/uncompressed/" + s));
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.compressComic(childDirectory, "txt");
            } else if (level == COMMAND) {
                var packCommand = new PackCommand();
                packCommand.setDisableProgressBar(true);
                var ret = packCommand.run(sandbox.toPath());
                assertEquals(0, ret);
            }
            var comicFile = new File(sandbox, new NameConverter().normalizeFileName(fileName + ".cbz"));
            assertTrue(comicFile.exists()); // But should be empty! we have used no images and excluded txt by default
            assertFalse(childDirectory.exists());
            // Extract and check
            new CompressionService().decompressComic(comicFile);
            assertFalse(comicFile.exists());
            // Nothing created for an empty 7z
            assertFalse(childDirectory.exists());
        });
    }

    @Test
    public void testServiceErrors() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var service = new CompressionService();
            assertThrows(CompressionException.class, () -> service.compressComic(null));
            assertThrows(CompressionException.class, () -> service.compressComic(new File("does not exist")));
            // Must be a directory
            var realFile = new File(sandbox, "realFile");
            createNewFile(realFile);
            assertThrows(CompressionException.class, () -> service.compressComic(realFile));
            if (!OSDetection.isWindows()) {
                var symlink = new File(sandbox, "symlink");
                Files.createSymbolicLink(symlink.toPath(), realFile.toPath());
                assertThrows(CompressionException.class, () -> service.compressComic(symlink));
            }
        });
    }

    @Test
    public void testPreventRepeatedFiles() {
        var sb = sandbox();
        var ctx = sb.runTest((File sandbox) -> {
            sb.copyResource("/uncompressed/up.jpg", "some directory [by some guy]/up.jpg");
            sb.copyResource("/uncompressed/up.jpg", "some directory/up.jpg");
            var packCommand = new PackCommand();
            packCommand.setDisableProgressBar(true);
            var ret = packCommand.run(sandbox.toPath());
            // The directories have not been removed
            assertTrue(new File(sandbox, "some directory [by some guy]").exists());
            assertTrue(new File(sandbox, "some directory").exists());
            return ret;
        }, true);
        assertTrue(
            ctx.err().contains(String.format("The following files have a naming conflict:%nSome Directory"))
        );
    }
}
