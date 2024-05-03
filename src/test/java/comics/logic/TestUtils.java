package comics.logic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class TestUtils {

    public static void runTest(RunnableInTempDirectory action) throws Exception {
        File directory = null;
        try {
            directory = Files.createTempDirectory("tmp").toFile();
            // Run everything inside the temporary directory
            action.run(directory);
        } finally {
            // Delete recursively
            Files.walk(directory.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd").format( new Date());
    }

    public static String md5(File f) {
        var ret = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(Files.readAllBytes(f.toPath()));
            byte[] digest = md.digest();
            ret = Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            fail(e);
        }
        return ret;
    }

    public static void copyResource(String resourcePath, File toFile) {
        assertNotNull(toFile);
        assertFalse(toFile.exists());
        try (var is = TestUtils.class.getResourceAsStream(resourcePath)) {
            assertNotNull(is);
            Files.copy(is, toFile.toPath());
        } catch (IOException ioe) { fail(ioe); }
    }
}
