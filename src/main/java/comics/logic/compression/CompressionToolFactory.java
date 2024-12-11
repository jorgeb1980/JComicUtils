package comics.logic.compression;

import java.util.logging.Logger;

import cli.LogUtils;

public class CompressionToolFactory {

    private static final Logger LOGGER = LogUtils.getDefaultLogger();
    
    public static CompressionTool getCompressionTool() {
        if (SevenZipBindingService.INSTANCE.isInitialized()) {
            LOGGER.finest("Using the 7-zip java binding");
            return SevenZipBindingService.INSTANCE;
        } else {
            LOGGER.finest("Could not initialize 7zipJBinding; falling back to command line 7-zip client");
            return new SevenZipCommandLineService();
        }
    }
}
