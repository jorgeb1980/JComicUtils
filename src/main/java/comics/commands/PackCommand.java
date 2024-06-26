package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionService;
import comics.logic.RepeatedNamesValidator;

import java.io.File;
import java.nio.file.Path;

import static comics.logic.CompressionService.DEFAULT_FILE_EXCLUSIONS;
import static comics.utils.Utils.commonChecks;

@Command(command="pack", description="Packs every sub-directory under CWD into a .cbz file")
public class PackCommand {

    @Parameter(name="a", longName="all", description="If set, the command will include non-image files in the comics")
    public Boolean all = false;
    public void setAll(Boolean b) { all = b; }

    @Parameter(name="npb", longName="no-progress-bar", description="If set, the command will display no progress bar")
    public Boolean disableProgressBar = false;
    public void setDisableProgressBar(Boolean disable) { disableProgressBar = disable; }

    @Parameter(
        name="gc",
        longName="garbage-collector",
        description="If set, it will attempt to remove images that do not belong to the comic"
    )
    public Boolean garbageCollector = false;
    public void setGarbageCollector(Boolean gc) { garbageCollector = gc; }

    @Run
    public int run(Path cwd) throws Exception {
        commonChecks(disableProgressBar);
        return new GenericFileListOperation(cwd, "Packing comics...").execute(
            File::isDirectory,
            dir -> new CompressionService().compressComic(dir, garbageCollector, all ? null : DEFAULT_FILE_EXCLUSIONS),
            new RepeatedNamesValidator()
        );
    }
}
