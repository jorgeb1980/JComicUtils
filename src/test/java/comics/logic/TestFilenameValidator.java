package comics.logic;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static comics.utils.Tools.sandbox;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class TestFilenameValidator {

    @Test
    public void testDirectoriesNoRepetition() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var directories = List.of("dir1", "dir2", "dir 3");
            directories.forEach(d -> sb.copyResource("/uncompressed/up.jpg", d + "/up.jpg"));
            var validator = new RepeatedNamesValidator();
            directories.forEach(d -> validator.readFile(new File(sandbox, d)));
            try {
                validator.validate();
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    public void testDirectoriesWithRepetition() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var directories = List.of("dir 1", "dir 2", "dir 1 [by some guy]");
            directories.forEach(d -> sb.copyResource("/uncompressed/up.jpg", d + "/up.jpg"));
            var validator = new RepeatedNamesValidator();
            directories.forEach(d -> validator.readFile(new File(sandbox, d)));
            assertThrows(
                Exception.class,
                validator::validate,
                String.format("The following files have a naming conflict:%nDir - 1")
            );
        });
    }
}
