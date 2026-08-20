package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionService;
import lombok.Setter;

import java.nio.file.Path;

import static comics.utils.Utils.commonChecks;

@Setter
@Command(
        command="unpack",
        description="Unpacks every cbz/cbr file under CWD",
        // Kind of ham-fisted - prevents warnings of restricted access by jline3 by reverting to the free-for-all native access
        // Not great options provided in the library https://github.com/jline/jline3/issues/1067
        jvmArgs = "--enable-native-access=ALL-UNNAMED"
)
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
