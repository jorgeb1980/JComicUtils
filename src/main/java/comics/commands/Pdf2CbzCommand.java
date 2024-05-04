package comics.commands;

import cli.annotations.Command;
import cli.annotations.Parameter;
import cli.annotations.Run;
import comics.logic.CompressionService;
import comics.logic.PdfService;

import java.nio.file.Path;

import static comics.commands.PackCommand.DEFAULT_EXCLUSIONS;

@Command(command="pdf2cbz", description="Translates every pdf under CWD into a .cbz file")
public class Pdf2CbzCommand {

    public static final String DEFAULT_FORMAT = "jpg";

    @Parameter(name = "f", longName = "format", description = "Image format to be used during conversion")
    private String format = DEFAULT_FORMAT;
    public void setFormat(String format) { this.format = format; }

    @Run
    public int execute(Path cwd) {
        var compressionService = new CompressionService();
        var pdfService = new PdfService();
        if (!compressionService.check()) {
            System.err.println("Compression engine is not ready!");
            return -1;
        } else {
            return new GenericFileListCommand(cwd, "Converting pdf files...").execute(
                f -> !f.isDirectory() && (f.getName().toLowerCase().endsWith("pdf")),
                f -> {
                    var directory = pdfService.convertPDF(f, format);
                    compressionService.compressComic(directory, DEFAULT_EXCLUSIONS);
                }
            );
        }
    }
}
