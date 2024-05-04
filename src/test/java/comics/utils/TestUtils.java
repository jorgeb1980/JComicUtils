package comics.utils;

import org.junit.jupiter.api.Test;
import shell.OSDetection;

import java.io.File;
import java.nio.file.Files;

import static comics.utils.Tools.runTest;
import static comics.utils.Utils.removeDirectory;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class TestUtils {

    @Test
    public void testRemoveDirEdgeCases() {
        runTest((File directory) -> {
            assertThrowsExactly(AssertionError.class, () -> removeDirectory(null));
            assertThrowsExactly(AssertionError.class, () -> removeDirectory(new File(directory, "does not exist")));
            File realFile = new File(directory, "whatever");
            realFile.createNewFile();
            assertThrowsExactly(AssertionError.class, () -> removeDirectory(realFile));
            if (!OSDetection.isWindows()) {
                File symlink = new File(directory, "link_to_whatever");
                Files.createSymbolicLink(symlink.toPath(), realFile.toPath());
                assertThrowsExactly(AssertionError.class, () -> removeDirectory(symlink));
            }
        });
    }
}
