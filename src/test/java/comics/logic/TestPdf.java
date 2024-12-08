package comics.logic;

import comics.commands.Pdf2CbzCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import test.Sandbox;
import test.sandbox.SandboxTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static comics.commands.Pdf2CbzCommand.DEFAULT_FORMAT;
import static comics.utils.Tools.mkdir;
import static comics.utils.Tools.sandbox;
import static comics.utils.Utils.emptyIfNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;

public class TestPdf {

    private void checkAllImages(File directory, String format) {
        assertTrue(directory.exists());
        var newFiles = Arrays.stream(emptyIfNull(directory.listFiles())).map(File::getName).filter(s -> s.endsWith(format)).toList();
        assertEquals(6, newFiles.size());
        var expectedFiles = List.of(
            "image_1." + format,
            "image_2." + format,
            "image_3." + format,
            "image_4." + format,
            "image_5." + format,
            "image_6." + format
        );
        expectedFiles.forEach(s -> assertTrue(newFiles.contains(s)));
    }

    @SandboxTest
    public void testServiceEdgeCases(Sandbox sb) throws IOException {
        var pdfService = new PdfService();
        assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(null, "jpg"));
        var pdf = sb.copyResource("/compressed/test.pdf", "test.pdf");
        assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(pdf, null));
        assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(pdf, "trololololo"));
        var dir = new File(sb.getSandbox(), "childDir");
        mkdir(dir);
        assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(dir, "jpg"));
    }

    @SandboxTest
    @EnabledOnOs( { LINUX, MAC })
    public void testServiceEdgeCasesSymlinks(Sandbox sb) throws IOException {
        var pdfService = new PdfService();
        var pdf = sb.copyResource("/compressed/test.pdf", "test.pdf");
        var symlink = new File(sb.getSandbox(), "symlink");
        Files.createSymbolicLink(symlink.toPath(), pdf.toPath());
        assertThrowsExactly(AssertionError.class, () -> pdfService.convertPDF(symlink, "jpg"));
    }

    @SandboxTest
    public void testService(Sandbox sb) throws IOException {
        var format = "jpg";
        var pdf = sb.copyResource("/compressed/test.pdf", "test.pdf");
        var createdDirectory = new PdfService().convertPDF(pdf, format);
        // We removed the original file
        assertFalse(pdf.exists());
        assertEquals("test", createdDirectory.getName());
        assertTrue(createdDirectory.exists());
        assertTrue(createdDirectory.isDirectory());
        checkAllImages(createdDirectory, format);
    }

    @Test
    public void testCommand() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var pdf = sb.copyResource("/compressed/test.pdf", "test.pdf");
            var command = new Pdf2CbzCommand();
            command.setDisableProgressBar(true);
            var ret = command.run(sandbox.toPath());
            assertEquals(0, ret);
            assertFalse(pdf.exists());
            // We have created a directory as intermediate step that should not be there any more
            var intermediateDirectory = new File(sandbox, "test");
            assertFalse(intermediateDirectory.exists());
            // Result
            var expectedFile = new File(sandbox, "Test.cbz");
            assertTrue(expectedFile.exists());
            assertFalse(expectedFile.isDirectory());
            new CompressionService().decompressComic(expectedFile);
            checkAllImages(new File(sandbox, "Test"), DEFAULT_FORMAT);
        });
    }

    @Test
    public void testCommandPng() {
        var sb = sandbox();
        sb.runTest((File sandbox) -> {
            var format = "png";
            var pdf = sb.copyResource("/compressed/test.pdf", "test.pdf");

            var command = new Pdf2CbzCommand();
            command.setDisableProgressBar(true);
            command.setFormat(format);
            var ret = command.run(sandbox.toPath());
            assertEquals(0, ret);
            // We have created a directory as intermediate step that should not be there any more
            var intermediateDirectory = new File(sandbox, "test");
            assertFalse(intermediateDirectory.exists());
            // Result
            var expectedFile = new File(sandbox, "Test.cbz");
            assertTrue(expectedFile.exists());
            assertFalse(expectedFile.isDirectory());
            new CompressionService().decompressComic(expectedFile);
            checkAllImages(new File(sandbox, "Test"), format);
        });
    }
}
