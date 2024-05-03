package comics.logic;

import shell.ShellCommandLauncher;
import shell.ShellException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CompressionService {

    private BackupService backupService;

    public CompressionService(BackupService backupService) {
        this.backupService = backupService;
    }

    // 7z is in the path and accessible
    public boolean check() {
        var ret = false;
        try {
            var result = ShellCommandLauncher.builder().command("7z").parameter("--help").build().launch();
            if (result.getExitCode() == 0) {
                ret = true;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return ret;
    }

    /**
     * Runs 7z to extract the comic file contents into a directory with the same name
     * @param comicFile Non-null, existing, non-directory, non-symlink, 7z-compatible compressed file into a directory
     *                  with the same name, without extension, that must not exist
     * @throws CompressionException If any of the aforementioned conditions is not met
     */
    public void decompressComic(File comicFile) throws CompressionException {
        try {
            assert comicFile != null;
            assert comicFile.exists();
            assert !comicFile.isDirectory();
            assert !Files.isSymbolicLink(comicFile.toPath());

            var targetDirectory = new File(
                comicFile.getParentFile(),
                // Remove extension
                comicFile.getName().substring(0, comicFile.getName().lastIndexOf('.'))
            );
            assert !targetDirectory.exists();

            var result = ShellCommandLauncher.builder().
                cwd(comicFile.getParentFile()).
                command("7z").
                parameter("e").
                parameter(comicFile.getAbsolutePath()).
                parameter("-o" + targetDirectory.getAbsolutePath()).
                parameter("*").
                parameter("-r").build().launch();
            if (result.getExitCode() != 0) throw new CompressionException(result);
            // If successful, backup the file
            backupService.backupFile(comicFile);
        } catch (IOException | ShellException | AssertionError e) {
            throw new CompressionException(e);
        }
    }
}
