package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionService;

import java.io.File;
import java.nio.file.Path;

import static comics.commands.PackCommand.DEFAULT_EXCLUSIONS;

@Command(command = "repack", description = "Unpacks every cbz/cbr file under CWD and repacks them into .cbz files")
public class RepackCommand {

    @Parameter(name="a", longName="all", description="If set, the command will include non-image files in the comics")
    public Boolean all = false;
    public void setAll(Boolean b) { all = b; }

    @Run
    public int execute(Path cwd) {
        var compressionService = new CompressionService();
        if (!compressionService.check()) {
            System.err.println("Compression engine is not ready!");
            return -1;
        } else {
            return new GenericFileListCommand(cwd, "Repacking comics...").execute(
                f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith("cbz") || f.getName().toLowerCase().endsWith("cbr")),
                f -> {
                    var expectedDirectory = new File(
                        cwd.toFile(),
                        f.getName().substring(0, f.getName().lastIndexOf('.'))
                    );
                    compressionService.decompressComic(f);
                    compressionService.compressComic(expectedDirectory, all ? null : DEFAULT_EXCLUSIONS);
                }
            );
        }
    }
}
