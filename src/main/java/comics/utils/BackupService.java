package comics.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.logging.Level.FINE;

// This class manages a backup of discarded files inside $HOME/.comicutils
public class BackupService {

    private final static Logger logger = Logger.getLogger("cli");

    private final File baseDir;

    public BackupService() {
        this.baseDir = new File(System.getProperty("user.home"));
    }

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private File backupDirectory() throws IOException {
        var backupDir = new File(baseDir, ".comicutils");
        if (backupDir.exists() && !backupDir.isDirectory()) {
            throw new IOException(String.format("Cannot proceed; %s exists and is not a directory!!!%n", backupDir.getAbsolutePath()));
        }
        return backupDir;
    }

    private Path calculatePath(File backupDir, File sourceFile) {
        var dir = new File(backupDir, DATE_FORMAT.format(new Date()));
        if (dir.mkdir()) logger.log(FINE, String.format("Created backup directory %s", dir));
        return new File(dir, sourceFile.getName()).toPath();
    }

    // Moves a certain existing, non-directory, non-symlink file into the backup directory
    public void backupFile(File f) throws IOException {
        assert f != null;
        assert f.exists();
        assert !f.isDirectory();
        assert !Files.isSymbolicLink(f.toPath());

        File toDir = backupDirectory();
        Path targetPath = calculatePath(toDir, f);
        // Make it sure the directory exists up to the day
        File parentDir = targetPath.getParent().toFile();
        if (!parentDir.exists()) {
            if (parentDir.mkdirs()) logger.log(FINE, String.format("Recreating path to backup directory %s", parentDir));
        }
        Files.move(f.toPath(), calculatePath(toDir, f), REPLACE_EXISTING);
        logger.log(FINE, String.format("'%s' moved to backup directory '%s'", f, targetPath));
    }
}
