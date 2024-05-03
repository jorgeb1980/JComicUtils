package comics.logic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class FileSystemUtils {

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
