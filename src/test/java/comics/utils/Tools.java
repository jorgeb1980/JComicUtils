package comics.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class Tools {

    private final static AtomicInteger testCounter = new AtomicInteger(1);

    public enum TestLevel { SERVICE, COMMAND; }

    @FunctionalInterface
    public interface CouldThrowSomething {
        void run() throws Exception;
    }

    public static String captureStdOutput(CouldThrowSomething action) {
        final var myOut = new ByteArrayOutputStream();
        final PrintStream originalOut = System.out;
        System.setOut(new PrintStream(myOut));
        String standardOutput = "";
        try {
            action.run();
        } catch (Exception e) {
            fail(e);
        } finally {
            standardOutput = myOut.toString();
            System.setOut(originalOut);
        }
        return standardOutput;
    }

    public static void runTest(RunnableInTempDirectory action) {
        File directory = null;
        try {
            directory = Files.createTempDirectory("tmp" + testCounter.addAndGet(1)).toFile();
            // Terrible hacks used to avoid having to use mockito
            System.setProperty("user.home", directory.getAbsolutePath());
            action.run(directory);
        } catch (Exception e) { fail(e); }
        finally {
            try {
                // Delete recursively
                if (directory != null) Utils.removeDirectory(directory);
            } catch (IOException ioe) {
                // Fail too if cleanup was not possible for whatever reason
                fail(ioe);
            }
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
        try (var is = Tools.class.getResourceAsStream(resourcePath)) {
            assertNotNull(is);
            Files.copy(is, toFile.toPath());
        } catch (IOException ioe) { fail(ioe); }
    }

    public static void createNewFile(File f) throws IOException {
        if (!f.createNewFile()) fail(String.format("Could not create file %s", f));
    }

    public static void mkdir(File f) throws IOException {
        if (!f.mkdir()) fail(String.format("Could not create directory %s", f));
    }
}
