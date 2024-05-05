package comics.utils;

import comics.logic.CompressionService;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.stream.Stream;

import static java.util.logging.Level.SEVERE;

public class Utils {

    // To be called by every command
    public static void commonChecks(Boolean disableProgressBar) throws Exception {
        progressBarDisabled = disableProgressBar;
        // Also disable progress bar if CLI_LOG_LEVEL is higher than standard (SEVERE)
        var cliLogLevel = System.getenv("CLI_LOG_LEVEL");
        if (cliLogLevel != null && !cliLogLevel.isEmpty()) {
            progressBarDisabled = (Level.parse(cliLogLevel.toUpperCase()).intValue() < SEVERE.intValue());
        }
        if (!new CompressionService().check()) {
            throw new Exception("Compression engine is not ready!");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] emptyIfNull(T[] array) {
        if (array == null) return (T[]) new Object[0];
        else return array;
    }

    private static Boolean progressBarDisabled = false;

    public static Stream<File> wrapWithProgressBar(Stream<File> stream, String taskName) {
        if (progressBarDisabled) return stream;
        else {
            var pgBuilder = new ProgressBarBuilder();
            pgBuilder.setTaskName(taskName);
            // Detect if we have or not a TERM variable in order to present a nicer progress bar
            if (System.getenv("TERM") == null) pgBuilder.setStyle(ProgressBarStyle.ASCII);
            return ProgressBar.wrap(stream, pgBuilder);
        }

    }
    public static void removeDirectory(File directory) throws IOException {
        assert directory != null;
        assert !Files.isSymbolicLink(directory.toPath());
        assert directory.isDirectory();

        Files.walk(directory.toPath())
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }
}
