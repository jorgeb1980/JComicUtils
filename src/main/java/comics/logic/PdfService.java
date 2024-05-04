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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            + formats.stream().collect(Collectors.joining("\n"));

        var document = Loader.loadPDF(pdf);
        // Dump the images into the parent directory
        var imagesFromPDF = getImagesFromPDF(document);
        var total = imagesFromPDF.size();
        var newDirectory = new File(pdf.getParent(), pdf.getName().substring(0, pdf.getName().lastIndexOf('.')));
        newDirectory.mkdir();
        for (int i = 0; i < total; i++) {
            var img = imagesFromPDF.get(i);
            var bytes = toByteArray(img, sanitizedFormat);
            writeBytes(newDirectory, bytes, calculateFileName(i, total, sanitizedFormat));
        }
        // Remove original file
        BackupService.get().backupFile(pdf);
        return newDirectory;
    }

    private String calculateFileName(int i, int total, String format) {
        int width = Integer.toString(total).length();
        return String.format("image_%0" + width + "d." + format, i);
    }

    private void writeBytes(File directory, byte[] bytes, String fileName) throws IOException {
        var targetFile = new File(directory, fileName);
        targetFile.createNewFile();
        Files.write(targetFile.toPath(), bytes);
    }

    public static byte[] toByteArray(RenderedImage bi, String format)
        throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, format, baos);
        byte[] bytes = baos.toByteArray();
        return bytes;

    }

    private List<RenderedImage> getImagesFromPDF(PDDocument document) throws IOException {
        var images = new ArrayList<RenderedImage>();
        for (var page : document.getDocumentCatalog().getPages()) {
            images.addAll(getImagesFromResources(page.getResources()));
        }

        return images;
    }

    private List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
        var images = new ArrayList<RenderedImage>();

        for (var xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
            } else if (xObject instanceof PDImageXObject) {
                images.add(((PDImageXObject) xObject).getImage());
            }
        }

        return images;
    }
}
