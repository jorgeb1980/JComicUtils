package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionService;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
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
            return new GenericFileListCommand().execute(
                (File directory) -> Arrays.stream(directory.listFiles()).filter(f -> f.isDirectory()).toList(),
                (File entry) -> {
                    var exclusions = new LinkedList<String>();
                    if (!all) {
                        exclusions.add("txt");
                    }
                    compressionService.compressComic(entry, exclusions.toArray(new String[0]));
                },
                "Packing comics...",
                cwd
            );
        }
    }
}
