package comics.logic;

import comics.commands.RepackCommand;
import test.CaptureOutput;
import test.Sandbox;
import test.sandbox.SandboxTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static comics.utils.Utils.emptyIfNull;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.*;

public class TestRepack {

    @SandboxTest
    public void testCommandRegularExecution(Sandbox sb) throws Exception {
        var comicFile = sb.copyResource("/compressed/test.cbr", "Another test 456.cbr");
        var command = new RepackCommand();
        command.setDisableProgressBar(TRUE);
        command.run(sb.getSandbox().toPath());
        assertFalse(comicFile.exists());
        var targetFile = new File(sb.getSandbox(), "Another Test - 456.cbz");
        assertTrue(targetFile.exists());
        // unpack and check that there are no .txt files in the target file
        var compressionService = new CompressionService();
        compressionService.decompressComic(targetFile);
        var newDir = new File(sb.getSandbox(), "Another Test - 456");
        assertTrue(newDir.exists());
        assertTrue(newDir.isDirectory());
        var expectedFiles = List.of("up.jpg", "right.jpg", "down.jpg", "left.jpg");
        var children = Arrays.stream(emptyIfNull(newDir.listFiles())).map(File::getName).toList();
        assertEquals(expectedFiles.size(), children.size());
        assertTrue(children.stream().filter(s -> s.endsWith(".txt")).toList().isEmpty());
        expectedFiles.forEach(f -> assertTrue(children.contains(f)));
    }

    @SandboxTest
    public void testCommandIncludeAll(Sandbox sb) throws Exception {
        var comicFile = sb.copyResource("/compressed/test.cbr", "test 1.cbr");
        var command = new RepackCommand();
        command.setDisableProgressBar(TRUE);
        command.setAll(TRUE);
        command.run(sb.getSandbox().toPath());
        assertFalse(comicFile.exists());
        var targetFile = new File(sb.getSandbox(), "Test - 1.cbz");
        assertTrue(targetFile.exists());
        // unpack and check that there are no .txt files in the target file
        var compressionService = new CompressionService();
        compressionService.decompressComic(targetFile);
        var newDir = new File(sb.getSandbox(), "Test - 1");
        assertTrue(newDir.exists());
        assertTrue(newDir.isDirectory());
        var expectedFiles = List.of(
            "up.jpg", "right.jpg", "down.jpg", "left.jpg",
            "foo.txt", "bar.txt", "baz.txt", "should_not_be_here.xml", "thumbs.db"
        );
        var children = Arrays.stream(emptyIfNull(newDir.listFiles())).map(File::getName).toList();
        assertEquals(expectedFiles.size(), children.size());
        expectedFiles.forEach(f -> assertTrue(children.contains(f)));
    }

    @SandboxTest
    public void testPreventRepeatedFiles(Sandbox sb) throws Exception {
        sb.copyResource("/compressed/test.cbr", "test.cbr");
        sb.copyResource("/compressed/test.cbr", "test [by some guy].cbr");
        var repackCommand = new RepackCommand();
        repackCommand.setDisableProgressBar(TRUE);
        var ctx = CaptureOutput.captureOutput(() -> {
            repackCommand.run(sb.getSandbox().toPath());
            // No files touched
            assertTrue(new File(sb.getSandbox(), "test.cbr").exists());
            assertTrue(new File(sb.getSandbox(), "test [by some guy].cbr").exists());
        });
        assertTrue(
            ctx.err().contains(String.format("The following files have a naming conflict:%nTest"))
        );
    }
}
