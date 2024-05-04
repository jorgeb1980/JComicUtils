package comics.commands;

import cli.annotations.Command;
import cli.annotations.Run;
import comics.logic.CompressionService;

import java.nio.file.Path;

@Command(command="unpack", description="Unpacks every cbz/cbr file under CWD")
public class UnpackCommand {

    @Run
    public int run(Path cwd) {
        var compressionService = new CompressionService();
        if (!compressionService.check()) {
            System.err.println("Compression engine is not ready!");
            return -1;
        } else {
            return new GenericFileListCommand(cwd, "Unpacking comics...").execute(
                f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith("cbz") || f.getName().toLowerCase().endsWith("cbr")),
                f -> compressionService.decompressComic(f)
            );
        }
    }
}
