package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionService;
import lombok.Setter;

import java.nio.file.Path;

import static comics.utils.Utils.commonChecks;

@Setter
@Command(command="unpack", description="Unpacks every cbz/cbr file under CWD")
public class UnpackCommand {

    @Parameter(name="npb", longName="no-progress-bar", description="If set, the command will display no progress bar")
    public Boolean disableProgressBar = false;

    @Run
    public int run(Path cwd) throws Exception {
        commonChecks(disableProgressBar);
        return new GenericFileListOperation(cwd, "Unpacking comics...").execute(
            f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith("cbz") || f.getName().toLowerCase().endsWith("cbr")),
            comic -> new CompressionService().decompressComic(comic)
        );
    }
}
