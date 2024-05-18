package comics.logic;

import comics.utils.BackupService;
import comics.utils.Utils;
import shell.ShellCommandLauncher;
import shell.ShellException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CompressionService {

    public CompressionService() { }

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
     * @param comicFile Not null, existing, non-directory, non-symlink, 7z-compatible compressed file into a directory
     *                  with the same name, without extension, that must not exist
     * @throws CompressionException If any of the aforementioned conditions is not met
     */
    public void decompressComic(File comicFile) throws CompressionException {
        try {
            assert comicFile != null : "Please specify a non-null file";
            assert comicFile.exists() : "Please specify an existing file";
            assert !comicFile.isDirectory() : String.format("Cannot decompress %s - it is a directory", comicFile);
            assert !Files.isSymbolicLink(comicFile.toPath()) : String.format("Cannot decompress %s - it is a symlink", comicFile);

            var targetDirectory = new File(
                comicFile.getParentFile(),
                // Remove extension
                comicFile.getName().substring(0, comicFile.getName().lastIndexOf('.'))
            );
            assert !targetDirectory.exists() : String.format("Cannot decompress %s - there is something in the way", comicFile);;

            var result = ShellCommandLauncher.builder().
                cwd(comicFile.getParentFile()).
                command("7z").
                parameter("e").
                parameter(comicFile.getAbsolutePath()).
                parameter("-o" + targetDirectory.getAbsolutePath()).
                parameter("*").
                parameter("-r").
                parameter("-spf").build().launch();
            if (result.getExitCode() != 0) throw new CompressionException(result);
            // If successful, backup the file
            new BackupService().backupFile(comicFile);
        } catch (IOException | ShellException | AssertionError e) {
            throw new CompressionException(e);
        }
    }

    /**
     * Creates a zip file with the contents of an existing directory, excluding the instructed file extensions.
     * The zip file created has a normalized file name.
     * @param directory Base directory whose contents will appear in the comic
     * @param exclusions Extensions of files forbidden in the final comic
     * @throws CompressionException If any pre-condition is not met or there is any failure in the I/O operation
     */
    public void compressComic(File directory, String... exclusions) throws CompressionException {
        try {
            assert directory != null;
            assert directory.exists();
            assert !Files.isSymbolicLink(directory.toPath());
            assert directory.isDirectory();

            var targetFile = new File(
                directory.getParentFile(),
                new NameConverter().normalizeFileName(directory.getName() + ".cbz")
            );

            var builder = ShellCommandLauncher.builder().
                cwd(directory).
                command("7z").
                parameter("a").
                parameter("-tzip").
                parameter("-xr!__MACOSX");
            if (exclusions != null)
                for (var exclusion: exclusions) builder.parameter("-xr!*." + exclusion);
            var result = builder.parameter(targetFile.getAbsolutePath()).
                parameter("*").build().launch();
            if (result.getExitCode() != 0) throw new CompressionException(result);
            // Zip file generated successfully - remove the original directory
            Utils.removeDirectory(directory);
        } catch (ShellException | AssertionError | IOException ioe) { throw new CompressionException(ioe); }
    }
}
