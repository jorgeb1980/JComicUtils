package comics.commands;

import cli.annotations.Command;
import cli.annotations.Run;
import comics.logic.CompressionService;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

@Command(command="unpack", description="Unpacks every cbz/cbr file under CWD and repacks them into .cbz files")
public class UnpackCommand {

    @Run
    public int run(Path cwd) {
        var compressionService = new CompressionService();
        if (!compressionService.check()) {
            System.err.println("Compression engine is not ready!");
            return -1;
        } else {
            return new GenericFileListCommand().execute(
                (File directory) -> Arrays.stream(directory.listFiles()).filter(
                    f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith("cbz") || f.getName().toLowerCase().endsWith("cbr"))
                ).toList(),
                (File entry) -> {
                    compressionService.decompressComic(entry);
                },
                "Unpacking comics..",
                cwd
            );
        }
    }
}
