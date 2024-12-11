package comics.logic.compression;

import java.io.File;

public interface CompressionTool {
    void extractFile(
        final File comicFile,
        final File targetDirectory
    );
}
