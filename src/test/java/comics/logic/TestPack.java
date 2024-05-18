package comics.logic;

import comics.commands.PackCommand;
import comics.commands.UnpackCommand;
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

import static comics.logic.CompressionService.DEFAULT_FILE_EXCLUSIONS;
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
                "left.jpg",
                "something.nfo",
                "thumbs.db",
                "should_not_be_here.xml"
            ).forEach(s -> sb.copyResource("/uncompressed/" + s, childDirectory.getName() + "/" + s));
            // Remember the directory
            var originalData = new HashMap<String, String>();
            for (var f : emptyIfNull(childDirectory.listFiles())) {
                originalData.put(f.getName(), md5(f));
            }
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.compressComic(childDirectory, false);
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
                "left.jpg",
                "something.nfo",
                "thumbs.db",
                "should_not_be_here.xml"
            ).forEach(s -> sb.copyResource("/uncompressed/" + s, childDirectory.getName() + "/" + s));
            // Remember the directory
            var originalData = new HashMap<String, String>();
            for (var f: emptyIfNull(childDirectory.listFiles())) {
                originalData.put(f.getName(), md5(f));
            }
            if (level == SERVICE) {
                var compressionService = new CompressionService();
                compressionService.compressComic(childDirectory, false, DEFAULT_FILE_EXCLUSIONS);
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
                originalData.keySet().stream().filter(s -> s.endsWith("jpg")).toList().size(),
                emptyIfNull(newChildDirectory.listFiles()).length
            );
            assertTrue(Arrays.stream(emptyIfNull(newChildDirectory.listFiles())).filter(
                s -> !s.getName().endsWith("jpg")
            ).toList().isEmpty());
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
                compressionService.compressComic(childDirectory, false, "txt");
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
            assertThrows(CompressionException.class, () -> service.compressComic(null, false));
            assertThrows(
                CompressionException.class,
                () -> service.compressComic(new File("does not exist"), false)
            );
            // Must be a directory
            var realFile = new File(sandbox, "realFile");
            createNewFile(realFile);
            assertThrows(CompressionException.class, () -> service.compressComic(realFile, false));
            if (!OSDetection.isWindows()) {
                var symlink = new File(sandbox, "symlink");
                Files.createSymbolicLink(symlink.toPath(), realFile.toPath());
                assertThrows(CompressionException.class, () -> service.compressComic(symlink, false));
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

    @Test
    public void testRemoveUndesiredDirectories() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var baseDir = new File(sandbox, "comic");
            mkdir(baseDir);
            sb.copyResource("/uncompressed/up.jpg", baseDir.getName() + "/up.jpg");

            var undesiredDir = new File(baseDir, "__MACOSX");
            mkdir(undesiredDir);

            // Compress comic
            var packCommand = new PackCommand();
            packCommand.setDisableProgressBar(true);
            packCommand.run(sandbox.toPath());

            assertTrue(new File(sandbox, "Comic.cbz").exists());
            assertFalse(baseDir.exists());

            // Unpack comic
            var unpackCommand = new UnpackCommand();
            unpackCommand.setDisableProgressBar(true);
            unpackCommand.run(sandbox.toPath());

            var newBaseDir = new File(sandbox, "Comic");
            assertTrue(newBaseDir.exists());
            assertTrue(new File(newBaseDir, "up.jpg").exists());
            assertFalse(new File(newBaseDir, "__MACOSX").exists());
        });
    }

    @Test
    public void testGarbageCollector() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            sb.copyResource("/uncompressed/up.jpg", "comic/up.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/theNerdOfHell.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/z-something.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/the collaborators of.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/y sus colaboradores.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/club Flyer.jpg");

            var packCommand = new PackCommand();
            packCommand.setGarbageCollector(true);
            packCommand.setDisableProgressBar(true);
            packCommand.run(sandbox.toPath());

            // After unpacking, we should only have up.jpg in the directory
            var unpackCommand = new UnpackCommand();
            unpackCommand.setDisableProgressBar(true);
            unpackCommand.run(sandbox.toPath());

            var newBaseDir = new File(sandbox, "Comic");
            assertTrue(newBaseDir.exists());
            assertTrue(newBaseDir.isDirectory());
            assertEquals(1, newBaseDir.listFiles().length);
            assertTrue(new File(newBaseDir, "up.jpg").exists());
        });
    }

    @Test
    public void testTrivialNestingCase() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            sb.copyResource("/uncompressed/up.jpg", "comic/unnecessary nesting/up.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/unnecessary nesting/right.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/unnecessary nesting/down.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/unnecessary nesting/left.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/unnecessary nesting/bar.txt");

            var packCommand = new PackCommand();
            packCommand.setDisableProgressBar(true);
            packCommand.run(sandbox.toPath());

            // After unpacking, we should have only the 4 images directly in the 'Comic' directory
            var unpackCommand = new UnpackCommand();
            unpackCommand.setDisableProgressBar(true);
            unpackCommand.run(sandbox.toPath());

            var comicDir = new File(sandbox, "Comic");
            assertTrue(comicDir.exists());
            assertTrue(comicDir.isDirectory());

            // No directory children
            assertTrue(Arrays.stream(comicDir.listFiles()).filter(File::isDirectory).toList().isEmpty());
            assertTrue(new File(comicDir, "left.jpg").exists());
            assertTrue(new File(comicDir, "down.jpg").exists());
            assertTrue(new File(comicDir, "right.jpg").exists());
            assertTrue(new File(comicDir, "up.jpg").exists());
        });
    }

    @Test
    public void testNonTrivialNestingCase() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/unnecessary nesting/up.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/unnecessary nesting/right.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/unnecessary nesting/down.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/unnecessary nesting/left.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/unnecessary nesting/z-left.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/unnecessary nesting/bar.txt");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/this is only trash/baz.txt");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/this is only trash/some nerd.jpg");

            var packCommand = new PackCommand();
            packCommand.setDisableProgressBar(true);
            packCommand.setGarbageCollector(true);
            packCommand.run(sandbox.toPath());

            // After unpacking, we should have only the 4 images directly in the 'Comic' directory
            var unpackCommand = new UnpackCommand();
            unpackCommand.setDisableProgressBar(true);
            unpackCommand.run(sandbox.toPath());

            var comicDir = new File(sandbox, "Comic");
            assertTrue(comicDir.exists());
            assertTrue(comicDir.isDirectory());

            // No directory children
            assertTrue(Arrays.stream(comicDir.listFiles()).filter(File::isDirectory).toList().isEmpty());
            assertTrue(new File(comicDir, "left.jpg").exists());
            assertTrue(new File(comicDir, "down.jpg").exists());
            assertTrue(new File(comicDir, "right.jpg").exists());
            assertTrue(new File(comicDir, "up.jpg").exists());
        });
    }

    @Test
    public void testThisShouldNotBeFlattened() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/comic 1/up.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/comic 1/right.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/comic 2/down.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/comic 2/left.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/comic 2/z-left.jpg");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/comic 2/bar.txt");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/this is only trash/baz.txt");
            sb.copyResource("/uncompressed/up.jpg", "comic/whatever/this is only trash/some nerd.jpg");

            var packCommand = new PackCommand();
            packCommand.setDisableProgressBar(true);
            packCommand.setGarbageCollector(true);
            packCommand.run(sandbox.toPath());

            // After unpacking, we should have only the 4 images directly in the 'Comic' directory
            var unpackCommand = new UnpackCommand();
            unpackCommand.setDisableProgressBar(true);
            unpackCommand.run(sandbox.toPath());

            var comicDir = new File(sandbox, "Comic");
            assertTrue(comicDir.exists());
            assertTrue(comicDir.isDirectory());

            // We should have kept all the children
            var whateverFile = new File(comicDir, "whatever");
            assertTrue(whateverFile.exists());
            var comic1 = new File(whateverFile, "comic 1");
            assertTrue(comic1.exists());
            var comic2 = new File(whateverFile, "comic 2");
            assertTrue(comic2.exists());

            assertTrue(new File(comic1, "right.jpg").exists());
            assertTrue(new File(comic1, "up.jpg").exists());
            assertTrue(new File(comic2, "left.jpg").exists());
            assertTrue(new File(comic2, "down.jpg").exists());
            assertFalse(new File(comic2, "bar.txt").exists());
            assertFalse(new File(comic2, "z-left.jpg").exists());
        });
    }
}
