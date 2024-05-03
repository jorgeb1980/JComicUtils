package comics.logic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

// This class manages a backup of discarded files inside $HOME/.comicutils
public class BackupUtils {

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static File backupDirectory() {
        var backupDir = new File(new File(System.getProperty("user.home")), ".comicutils");
        if (!backupDir.exists() && !backupDir.isDirectory()) {
            System.err.printf("Cannot proceed; %s exists and is not a directory!!!%n", backupDir.getAbsolutePath());
            System.exit(-1);
        }
        return backupDir;
    }

    static Path calculatePath(File backupDir, File sourceFile) {
        File dir = new File(backupDir, DATE_FORMAT.format(new Date()));
        dir.mkdir();
        return new File(dir, sourceFile.getName()).toPath();
    }

    // Moves a certain existing, non-directory, non-symlink file into the backup directory
    public static void backupFile(File f) throws IOException {
        assert f != null;
        assert f.exists();
        assert !f.isDirectory();
        assert !Files.isSymbolicLink(f.toPath());

        File toDir = backupDirectory();
        if (!toDir.exists()) {
            toDir.mkdirs();
        }
        Files.move(f.toPath(), calculatePath(toDir, f));
    }
}
