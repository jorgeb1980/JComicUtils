package comics.logic.compression;

import cli.LogUtils;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

enum SevenZipBindingService implements CompressionTool {

    INSTANCE;

    private final Logger LOGGER = LogUtils.getDefaultLogger();
    private boolean initialized = false;

    SevenZipBindingService() {
        try {
            SevenZip.initSevenZipFromPlatformJAR();
            LOGGER.finest("7-Zip-JBinding library was initialized");
            LOGGER.finest(SevenZip.getPlatformBestMatch() + " - " + SevenZip.getSevenZipJBindingVersion());
            var nativeVersion = SevenZip.getSevenZipVersion();
            LOGGER.finest("Using native library " + nativeVersion.version);
            initialized = true;
        } catch (SevenZipNativeInitializationException e) {
            LOGGER.severe("Cannot seem to initialize 7-Zip native binding");
            LOGGER.severe(e.getMessage());
        }
    }

    public boolean isInitialized() { return initialized; }

    public void extractFile(
        final File comicFile,
        final File targetDirectory
    ) {
        try (
            var randomAccessFile = new RandomAccessFile(comicFile, "r");
            var inArchive = SevenZip.openInArchive(
                null, // autodetect archive type
                new RandomAccessFileInStream(randomAccessFile)
            )
        ) {

            LOGGER.finest("   Hash   |    Size    | Filename");
            LOGGER.finest("----------+------------+---------");

            int count = inArchive.getNumberOfItems();
            List<Integer> itemsToExtract = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if (!(Boolean) inArchive.getProperty(i, PropID.IS_FOLDER)) {
                    itemsToExtract.add(i);
                }
            }
            int[] items = new int[itemsToExtract.size()];
            int i = 0;
            for (var integer : itemsToExtract) {
                items[i++] = integer;
            }
            inArchive.extract(
                items,
                false, // Non-test mode
                new ExtractCallback(targetDirectory, inArchive)
            );
        } catch (Exception e) {
            LOGGER.severe("Error occurs: " + e);
        }
    }

    private class ExtractCallback implements IArchiveExtractCallback {
        private int hash = 0;
        private int size = 0;
        private int index;
        private final IInArchive inArchive;
        private final File targetDirectory;

        private File prepareTarget(File root, String path) {
            var targetFile = new File(root, path);
            try {
                targetFile.getParentFile().mkdirs();
                targetFile.createNewFile();
            } catch (IOException ioe) { LOGGER.severe(ioe.getMessage()); }
            return targetFile;
        }

        public ExtractCallback(File targetDirectory, IInArchive inArchive) {
            this.inArchive = inArchive;
            this.targetDirectory = targetDirectory;
        }

        public ISequentialOutStream getStream(
            int index,
            ExtractAskMode extractAskMode
        ) throws SevenZipException {
            this.index = index;
            if (extractAskMode != ExtractAskMode.EXTRACT) {
                return null;
            }

            final var path = inArchive.getProperty(index, PropID.PATH).toString();
            final var targetFile = prepareTarget(targetDirectory, path);
            return data -> {
                hash ^= Arrays.hashCode(data);
                size += data.length;
                // Write the file in the expected path
                try (var os = new FileOutputStream(targetFile, true)) {
                    os.write(data);
                } catch (IOException ioe) { LOGGER.severe(ioe.getMessage()); }
                return data.length; // Return amount of proceed data
            };
        }

        public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException {
        }

        public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
            if (extractOperationResult != ExtractOperationResult.OK) {
                LOGGER.severe("Extraction error");
            } else {
                LOGGER.finest(
                    String.format(
                        "%9X | %10s | %s",
                        hash,
                        size,
                        inArchive.getProperty(index, PropID.PATH)
                    )
                );
                hash = 0;
                size = 0;
            }
        }

        public void setCompleted(long completeValue) throws SevenZipException {
        }

        public void setTotal(long total) throws SevenZipException {
        }

    }
}
