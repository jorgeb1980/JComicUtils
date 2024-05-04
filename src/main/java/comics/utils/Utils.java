package comics.utils;

import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Utils {

    public static ProgressBarBuilder pgBuilder(String taskName) {
        var pgBuilder = new ProgressBarBuilder();
        pgBuilder.setTaskName(taskName);
        // Detect if we have or not a TERM variable in order to present a nicer progress bar
        if (System.getenv("TERM") == null) pgBuilder.setStyle(ProgressBarStyle.ASCII);
        return pgBuilder;

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
