package comics.utils;

import me.tongfei.progressbar.ProgressBar;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class TestUtils {

    private final static AtomicInteger testCounter = new AtomicInteger(1);

    public enum TestLevel { SERVICE, COMMAND; }

    public static void runTest(RunnableInTempDirectory action) {
        File directory = null;
        try (var backupMock = Mockito.mockStatic(BackupService.class);
             var progressBarMock = Mockito.mockStatic(ProgressBar.class)) {
            directory = Files.createTempDirectory("tmp" + testCounter.addAndGet(1)).toFile();
            backupMock.when(BackupService::get).thenReturn(new BackupService(directory));
            // Mock progress bar too - has some issues when executing under maven
            progressBarMock.when(
                () -> ProgressBar.wrap(any(Iterable.class), anyString())
            ).thenAnswer(input -> input.getArgument(0));
            // Run everything inside the temporary directory
            action.run(directory);
        } catch (Exception e) { fail(e); }
        finally {
            try {
                // Delete recursively
                if (directory != null) FileSystemUtils.removeDirectory(directory);
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
        try (var is = TestUtils.class.getResourceAsStream(resourcePath)) {
            assertNotNull(is);
            Files.copy(is, toFile.toPath());
        } catch (IOException ioe) { fail(ioe); }
    }

}
