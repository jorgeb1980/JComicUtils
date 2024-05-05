package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionService;
import comics.logic.PdfService;

import java.nio.file.Path;

import static comics.commands.PackCommand.DEFAULT_EXCLUSIONS;
import static comics.utils.Utils.commonChecks;

@Command(
    command="pdf2cbz",
    description="Translates every pdf under CWD into a .cbz file",
    // May need a considerable heap for PDF collections
    jvmArgs="-Xmx35G"
)
public class Pdf2CbzCommand {

    public static final String DEFAULT_FORMAT = "jpg";

    @Parameter(name = "f", longName = "format", description = "Image format to be used during conversion")
    private String format = DEFAULT_FORMAT;
    public void setFormat(String format) { this.format = format; }

    @Parameter(name="npb", longName="no-progress-bar", description="If set, the command will display no progress bar")
    public Boolean disableProgressBar = false;
    public void setDisableProgressBar(Boolean disable) { disableProgressBar = disable; }

    @Run
    public int execute(Path cwd) throws Exception {
        commonChecks(disableProgressBar);
        return new GenericFileListOperation(cwd, "Converting pdf files...").execute(
            f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith("pdf")),
            f -> {
                var directory = new PdfService().convertPDF(f, format);
                new CompressionService().compressComic(directory, DEFAULT_EXCLUSIONS);
            }
        );
    }
}
