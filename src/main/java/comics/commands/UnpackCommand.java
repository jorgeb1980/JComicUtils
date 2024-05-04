package comics.commands;

import cli.annotations.Command;
import cli.annotations.Run;
import comics.logic.CompressionException;
import comics.logic.CompressionService;
import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;

import static comics.utils.Utils.pgBuilder;

@Command(command="unpack", description="Unpacks every cbz/cbr file under CWD and repacks them into .cbz files")
public class UnpackCommand {

    @Run
    public int run(Path cwd) {
        var compressionService = new CompressionService();
        if (!compressionService.check()) {
            System.err.println("Compression engine is not ready!");
            return -1;
        } else {
            var comicsCounter = 0;
            var comics = Arrays.stream(cwd.toFile().listFiles()).filter(
                f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith("cbz") || f.getName().toLowerCase().endsWith("cbr"))
            ).toList();
            var errors = new HashMap<File, CompressionException>();
            for (var file: ProgressBar.wrap(comics, pgBuilder("Unpacking comics..."))) {
                try {
                    compressionService.decompressComic(file);
                    comicsCounter++;
                } catch (CompressionException ce) {
                    errors.put(file, ce);
                }
            }
            // Report
            System.out.printf("Unpacked %d comics%n", comicsCounter);
            if (!errors.isEmpty())
                System.out.printf(
                    "Could not create comics for the following directories:%n%s",
                    String.join(
                        "\n",
                        errors.entrySet().stream().map(
                            e -> String.format("%s - %s", e.getKey().getName(), e.getValue().getMessage())
                        ).toList()
                    )
                );
            return 0;
        }
    }
}
