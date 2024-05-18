package comics.utils;

import cli.LogUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

// This class manages a backup of discarded files inside $HOME/.comicutils
public class BackupService {

    private final static Object LOCK = new Object();
    private final static Integer MAX_TENURE_DAYS = 7;
    private final static Logger logger = LogUtils.getDefaultLogger();
    private static final String DIRECTORY_NAME = ".comicutils";

    private final File baseDir;

    public BackupService() {
        this.baseDir = new File(System.getProperty("user.home"));
        // Check for backup directories older than the max tenure
        synchronized (LOCK) {
            cleanupBackupDirectory();
        }
    }

    private void cleanupBackupDirectory() {
        var backupDir = new File(baseDir, DIRECTORY_NAME);
        if (backupDir.exists() && backupDir.isDirectory())
            Arrays.stream(backupDir.listFiles()).filter(BackupService::isEligible).forEach(d -> {
                try {
                    Utils.removeDirectory(d);
                } catch (IOException ioe) {
                    logger.log(SEVERE, ioe.getMessage(), ioe);
                }
            });
    }

    private static boolean isEligible(File f) {
        boolean ret = true;
        if (f.exists() && f.isDirectory()) {
            try {
                var date = DATE_FORMAT.parse(f.getName());
                var now = new Date();
                if (now.getTime() - date.getTime() < MAX_TENURE_DAYS * 86400 * 1000 ) ret = false;
            } catch (ParseException pe) {
                ret = false;
            }
        } else ret = false;
        return ret;
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
