package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionService;

import java.io.File;
import java.nio.file.Path;

@Command(command="pack", description="Packs every sub-directory under CWD into a .cbz file")
public class PackCommand {

    static final String[] DEFAULT_EXCLUSIONS = new String[] { "txt" };

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
            return new GenericFileListCommand(cwd, "Packing comics...").execute(
                File::isDirectory,
                dir -> compressionService.compressComic(dir, all ? null : DEFAULT_EXCLUSIONS)
            );
        }
    }
}
