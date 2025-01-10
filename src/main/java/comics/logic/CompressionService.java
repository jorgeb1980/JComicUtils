package comics.logic;

import cli.LogUtils;
import comics.logic.compression.CompressionToolFactory;
import comics.utils.BackupService;
import comics.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.zip.Deflater.NO_COMPRESSION;
import static java.util.zip.ZipOutputStream.STORED;

public class CompressionService {

    public static final String[] DEFAULT_FILE_EXCLUSIONS = new String[] { "txt", "xml", "db", "nfo" };
    static final String[] DEFAULT_GARBAGE_PATTERNS = new String[] {
        "z.+",
        ".+nerd.+",
        ".+colab.+",
        ".+collab.+",
        ".+flyer.+"
    };

    public CompressionService() { }
  
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

            CompressionToolFactory.getCompressionTool().extractFile(comicFile, targetDirectory);
            // If successful, backup the file
            new BackupService().backupFile(comicFile);
        } catch (IOException | AssertionError e) {
            throw new CompressionException(e);
        }
    }

    /**
     * Creates a zip file with the contents of an existing directory, excluding the instructed file extensions.
     * The zip file created has a normalized file name.
     * @param directory Base directory whose contents will appear in the comic
     * @param extensionsExcluded Extensions of files forbidden in the final comic
     * @throws CompressionException If any pre-condition is not met or there is any failure in the I/O operation
     */
    public void compressComic(
        File directory,
        Boolean garbageCollector,
        String... extensionsExcluded
    ) throws CompressionException {
        try {
            assert directory != null;
            assert directory.exists();
            assert !Files.isSymbolicLink(directory.toPath());
            assert directory.isDirectory();

            var targetFile = new File(
                directory.getParentFile(),
                new NameConverter().normalizeFileName(directory.getName() + ".cbz")
            );

            var specificExclusions = garbageCollector ? collectGarbage(directory, extensionsExcluded) : new LinkedList<File>();
            // Detect trivial nesting case:
            // Single subdirectory below root with every image hanging from there
            var calculatedSourceDirectory = searchForTrivialNestingCase(directory, specificExclusions, extensionsExcluded);
            compressDirectory(calculatedSourceDirectory, extensionsExcluded, specificExclusions, targetFile);

            // Zip file generated successfully - remove the original directory
            Utils.removeDirectory(directory);
        } catch (AssertionError | IOException ioe) { throw new CompressionException(ioe); }
    }

    private void compressDirectory(
        File sourceDirectory,
        String[] extensionsExcluded,
        List<File> specificExclusions,
        File targetFile
    ) throws IOException {
        var p = Files.createFile(targetFile.toPath());
        try (var zs = new ZipOutputStream(Files.newOutputStream(p))) {
            zs.setMethod(STORED);
            zs.setLevel(NO_COMPRESSION);
            var pp = sourceDirectory.toPath();
            Files.walk(pp)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    if (!isExcluded(path.toFile(), extensionsExcluded)
                            && !specificExclusions.contains(path.toFile())) {
                        var zipEntry = new ZipEntry(pp.relativize(path).toString());
                        // Set all the necessary properties for STORED
                        zipEntry.setSize(path.toFile().length());
                        zipEntry.setCompressedSize(path.toFile().length());
                        var crc = new CRC32();
                        try { crc.update(Files.readAllBytes(path)); }
                        catch (IOException e) { LogUtils.getDefaultLogger().severe(e.getMessage()); }
                        zipEntry.setCrc(crc.getValue());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            LogUtils.getDefaultLogger().severe(e.getMessage());
                        }
                    }
                });
        }
    }

    // Searches for every image file in the directory and, if all of them are in the same place, it will
    //  pack from there
    private File searchForTrivialNestingCase(File directory, List<File> garbage, String... exclusions) {
        var images = new HashMap<File, LinkedList<File>>();
        var files = new LinkedList<File>();
        files.add(directory);

        while (!files.isEmpty()) {
            var file = files.remove();
            for (var f: file.listFiles()) {
                if (f.isDirectory()) files.add(f);
                else {
                    if (!isExcluded(f, exclusions) && !garbage.contains(f)) {
                        // if this is a valid image of the comic, then store in the list
                        images.computeIfAbsent(f.getParentFile(), key -> new LinkedList<>()).add(f);
                    }
                }
            }
        }

        if (images.keySet().size() == 0 || images.keySet().size() > 1) return directory;
        else return images.keySet().stream().findFirst().get();
    }

    private List<File> collectGarbage(File directory, String... exclusions) {
        var garbage = new LinkedList<File>();
        var files = new LinkedList<File>();
        files.add(directory);
        var patterns = Arrays.stream(DEFAULT_GARBAGE_PATTERNS).map(Pattern::compile).toList();

        while (!files.isEmpty()) {
            var file = files.remove();
            if (file.isDirectory()) {
                files.addAll(Arrays.stream(file.listFiles()).filter(File::isDirectory).toList());
                garbage.addAll(Arrays.stream(file.listFiles()).filter(f -> {
                    var isGarbage = false;
                    if (!f.isDirectory() && !isExcluded(f, exclusions)) {
                        for (var pattern: patterns) {
                            isGarbage |= pattern.matcher(f.getName().toLowerCase()).matches();
                        }
                    }
                    return isGarbage;
                }).toList());
            }
        }

        return garbage;
    }

    private boolean isExcluded(File f, String... exclusions) {
        var excluded = false;
        if (exclusions != null)
            for (var s: exclusions) {
                excluded |= f.getName().toLowerCase().endsWith("." + s.toLowerCase());
            }
        return excluded;
    }
}
