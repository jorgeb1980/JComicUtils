package comics.utils;

import org.junit.jupiter.api.condition.EnabledOnOs;
import test.Sandbox;
import test.sandbox.SandboxTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static comics.utils.Utils.removeDirectory;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;

public class TestUtils {

    @SandboxTest
    @EnabledOnOs({ LINUX, MAC })
    public void testRemoveDirEdgeCasesSymlinks(Sandbox sb) throws IOException {
        var directory = sb.getSandbox();
        File realFile = new File(directory, "whatever");
        realFile.createNewFile();
        File symlink = new File(sb.getSandbox(), "link_to_whatever");
        Files.createSymbolicLink(symlink.toPath(), realFile.toPath());
        assertThrowsExactly(AssertionError.class, () -> removeDirectory(symlink));
    }

    @SandboxTest
    public void testRemoveDirEdgeCasesBasic(Sandbox sb) throws IOException {
        var directory = sb.getSandbox();
        assertThrowsExactly(AssertionError.class, () -> removeDirectory(null));
        assertThrowsExactly(AssertionError.class, () -> removeDirectory(new File(directory, "does not exist")));
        File realFile = new File(directory, "whatever");
        realFile.createNewFile();
        assertThrowsExactly(AssertionError.class, () -> removeDirectory(realFile));
    }
}
