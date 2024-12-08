package comics.logic;

import test.Sandbox;
import test.sandbox.SandboxTest;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class TestFilenameValidator {

    @SandboxTest
    public void testDirectoriesNoRepetition(Sandbox sb) {
        var directories = List.of("dir1", "dir2", "dir 3");
        directories.forEach(d -> sb.copyResource("/uncompressed/up.jpg", d + "/up.jpg"));
        var validator = new RepeatedNamesValidator();
        directories.forEach(d -> validator.readFile(new File(sb.getSandbox(), d)));
        try {
            validator.validate();
        } catch (Exception e) {
            fail(e);
        }
    }

    @SandboxTest
    public void testDirectoriesWithRepetition(Sandbox sb) {
        var directories = List.of("dir 1", "dir 2", "dir 1 [by some guy]");
        directories.forEach(d -> sb.copyResource("/uncompressed/up.jpg", d + "/up.jpg"));
        var validator = new RepeatedNamesValidator();
        directories.forEach(d -> validator.readFile(new File(sb.getSandbox(), d)));
        assertThrows(
            Exception.class,
            validator::validate,
            String.format("The following files have a naming conflict:%nDir - 1")
        );
    }
}
