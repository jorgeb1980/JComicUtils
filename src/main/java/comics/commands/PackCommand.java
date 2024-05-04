package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionException;
import comics.logic.CompressionService;
import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

@Command(command="pack", description="Packs every sub-directory under CWD into a .cbz file")
public class PackCommand {

    @Parameter(name="a", longName="all", description="If set, the command will include non-image files in the comics")
    public Boolean all = false;
    public void setAll(Boolean b) { all = b; }

    @Run
    public int run(Path cwd) {
        var compressionService = new CompressionService();
        if (!compressionService.check()) {
            System.err.println("Compression engine is not ready!");
            return -1;
        } else {
            var comicsCounter = 0;
            var errors = new HashMap<File, CompressionException>();
            var childrenDirectories = Arrays.stream(cwd.toFile().listFiles()).filter(f -> f.isDirectory()).toList();
            for (var dir: ProgressBar.wrap(childrenDirectories, "Creating comics...")) {
                try {
                    var exclusions = new LinkedList<String>();
                    if (!all) {
                        exclusions.add("txt");
                    }
                    compressionService.compressComic(dir, exclusions.toArray(new String[0]));
                    comicsCounter++;
                } catch (CompressionException ce) {
                    errors.put(dir, ce);
                }
            }
            // Report
            System.out.printf("Packed %d comics%n", comicsCounter);
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
