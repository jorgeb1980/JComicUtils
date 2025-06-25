package comics.logic;

import comics.utils.BackupService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageWriterSpi;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static comics.utils.Utils.emptyIfNull;

// By far the class with the greatest amount of code straight from stackoverflow ¯\_(ツ)_/¯
public class PdfService {

    private static Set<String> formats = null;

    static {
        formats = new HashSet<>();
        // Obtain supported image formats for this JDK
        var registry = IIORegistry.getDefaultInstance();
        var serviceProviders = registry.getServiceProviders(ImageWriterSpi.class, false);
        while(serviceProviders.hasNext()) {
            var next = serviceProviders.next();
            formats.addAll(Arrays.stream(next.getFormatNames()).map(String::toLowerCase).toList());
        }
    }

    private static class Counter {
        private int counter = 1;

        public int postIncrease() {
            return counter++;
        }
    }

    private int extractIndex(String imageName, String sanitizedFormat) {
        var ret = -1;
        var matcher = Pattern.compile("image_(\\d+)\\." + sanitizedFormat).matcher(imageName);
        if (matcher.find()) {
            ret = Integer.parseInt(matcher.group(1));
        }
        return ret;
    }

    /**
     * Will convert the pdf contents into images inside a new directory with the same name
     * of the pdf, without extension
     * @param pdf PDF original file
     * @param format Desired format, as admitted by ImageIO
     * @return Reference to the directory just created with the images
     * @throws IOException Sums up all the possible problems here
     */
    public File convertPDF(File pdf, String format) throws IOException {
        assert pdf != null : "Please provide a non-null PDF file";
        assert !pdf.isDirectory() : "Please do not try to convert a directory";
        assert !Files.isSymbolicLink(pdf.toPath()) : "It is not possible to convert a symbolic link";
        assert format != null : "Please provide a non-null format";
        var sanitizedFormat = format.toLowerCase();
        assert formats.contains(sanitizedFormat) : "Please provide a valid format.  Available formats: "
            + String.join("\n", formats);

        var document = Loader.loadPDF(pdf);
        // Dump the images into the parent directory
        var newDirectory = new File(pdf.getParent(), pdf.getName().substring(0, pdf.getName().lastIndexOf('.')));
        if (!newDirectory.mkdir()) throw new IOException(String.format("Unable to create directory %s", newDirectory));
        dumpImagesFromPDF(document, newDirectory, sanitizedFormat);
        /* Images will be like:
            image_1.jpg
            image_2.jpg
            ...
            image_99.jpg
            ...
            image_999.jpg

            Now we rename them, left padding page number with zeroes
        */
        var images = emptyIfNull(newDirectory.list((f, s) -> s.toLowerCase().endsWith(sanitizedFormat)));
        var total = images.length;
        for (var image: images) {
            var imageFile = new File(newDirectory, image);
            var index = extractIndex(image, sanitizedFormat);
            Files.move(imageFile.toPath(), calculateFilePath(newDirectory, index, total, sanitizedFormat));
        }
        // Remove original file
        new BackupService().backupFile(pdf);
        return newDirectory;
    }

    private Path calculateFilePath(File directory, int i, int total, String format) {
        int width = Integer.toString(total).length();
        return new File(directory, String.format("image_%0" + width + "d.%s", i, format)).toPath();
    }

    private String calculateTemporalFilename(Counter counter, String format) {
        return String.format("image_%d.%s", counter.postIncrease(), format);
    }

    private void writeBytes(File directory, byte[] bytes, String fileName) throws IOException {
        var targetFile = new File(directory, fileName);
        if (targetFile.createNewFile()) Files.write(targetFile.toPath(), bytes);
    }

    public static byte[] toByteArray(RenderedImage bi, String format)
        throws IOException {

        var baos = new ByteArrayOutputStream();
        ImageIO.write(bi, format, baos);
        return baos.toByteArray();

    }

    private void dumpImagesFromPDF(
        PDDocument document,
        File directory,
        String format
    ) throws IOException {
        var counter = new Counter();
        for (var page : document.getDocumentCatalog().getPages())
            dumpImagesFromResources(page.getResources(), directory, format, counter);
    }

    private void dumpImagesFromResources(
        PDResources resources,
        File directory,
        String format,
        Counter counter
    ) throws IOException {
        for (var xObjectName : resources.getXObjectNames()) {
            var xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject)
                dumpImagesFromResources(
                    ((PDFormXObject) xObject).getResources(),
                    directory,
                    format,
                    counter
                );
            else if (xObject instanceof PDImageXObject) {
                writeBytes(
                    directory,
                    toByteArray(
                        ((PDImageXObject) xObject).getImage(),
                        format
                    ),
                    calculateTemporalFilename(counter, format)
                );
            }
        }
    }
}
