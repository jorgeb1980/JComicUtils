package comics.logic;

import comics.commands.Pdf2CbzCommand;
import org.junit.jupiter.api.Test;
import shell.OSDetection;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static comics.commands.Pdf2CbzCommand.DEFAULT_FORMAT;
import static comics.utils.Tools.copyResource;
import static comics.utils.Tools.runTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPdf {

    private void checkAllImages(File directory, String format) {
        var newFiles = Arrays.stream(directory.listFiles()).map(File::getName).filter(s -> s.endsWith(format)).toList();
        assertEquals(6, newFiles.size());
        var expectedFiles = List.of(
            "image_0." + format,
            "image_1." + format,
            "image_2." + format,
            "image_3." + format,
            "image_4." + format,
            "image_5." + format
        );
        expectedFiles.forEach(s -> assertTrue(newFiles.contains(s)));
    }

    @Test
    public void testServiceEdgeCases() {
        runTest((File directory) -> {
            var pdfService = new PdfService();
            assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(null, "jpg"));
            var pdf = new File(directory, "test.pdf");
            copyResource("/compressed/test.pdf", pdf);
            assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(pdf, null));
            assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(pdf, "trololololo"));
            var dir = new File(directory, "childDir");
            dir.mkdirs();
            assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(dir, "jpg"));
            if (!OSDetection.isWindows()) {
                var symlink = new File(directory, "symlink");
                Files.createSymbolicLink(symlink.toPath(), pdf.toPath());
                assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(symlink, "jpg"));
            }
        });
    }

    @Test
    public void testService() {
        runTest((File directory) -> {
            var format = "jpg";
            var pdf = new File(directory, "test.pdf");
            copyResource("/compressed/test.pdf", pdf);
            var createdDirectory = new PdfService().convertPDF(pdf, format);
            // We removed the original file
            assertFalse(pdf.exists());
            assertEquals("test", createdDirectory.getName());
            assertTrue(createdDirectory.exists());
            assertTrue(createdDirectory.isDirectory());
            checkAllImages(createdDirectory, format);
        });
    }

    @Test
    public void testCommand() {
        runTest((File directory) -> {
            var pdf = new File(directory, "test.pdf");
            copyResource("/compressed/test.pdf", pdf);

            var command = new Pdf2CbzCommand();
            var ret = command.execute(directory.toPath());
            assertEquals(0, ret);
            // We have created a directory as intermediate step that should not be there any more
            var intermediateDirectory = new File(directory, "test");
            assertFalse(intermediateDirectory.exists());
            // Result
            var expectedFile = new File(directory, "test.cbz");
            assertTrue(expectedFile.exists());
            assertFalse(expectedFile.isDirectory());
            new CompressionService().decompressComic(expectedFile);
            assertTrue(intermediateDirectory.exists());
            checkAllImages(intermediateDirectory, DEFAULT_FORMAT);
        });
    }

    @Test
    public void testCommandPng() {
        runTest((File directory) -> {
            var format = "png";
            var pdf = new File(directory, "test.pdf");
            copyResource("/compressed/test.pdf", pdf);

            var command = new Pdf2CbzCommand();
            command.setFormat(format);
            var ret = command.execute(directory.toPath());
            assertEquals(0, ret);
            // We have created a directory as intermediate step that should not be there any more
            var intermediateDirectory = new File(directory, "test");
            assertFalse(intermediateDirectory.exists());
            // Result
            var expectedFile = new File(directory, "test.cbz");
            assertTrue(expectedFile.exists());
            assertFalse(expectedFile.isDirectory());
            new CompressionService().decompressComic(expectedFile);
            assertTrue(intermediateDirectory.exists());
            checkAllImages(intermediateDirectory, format);
        });
    }
}
