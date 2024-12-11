package comics.logic.compression;

import java.io.File;

import cli.LogUtils;
import shell.CommandLauncher;

class SevenZipCommandLineService implements CompressionTool {
    @Override
    public void extractFile(File comicFile, File targetDirectory) {
        try {
            var result = CommandLauncher.builder().
                cwd(comicFile.getParentFile()).
                program("7z").
                parameter("e").
                parameter(comicFile.getAbsolutePath()).
                parameter("-o" + targetDirectory.getAbsolutePath()).
                parameter("*").
                parameter("-r").
                parameter("-spf").build().launch();
            if (result.getExitCode() != 0) throw new Exception(
                String.format("Error trying to decompress %s", comicFile.getAbsolutePath())
            );
        } catch (Exception e) {
            LogUtils.getDefaultLogger().severe("Error occurs: " + e);
        }
    }
}
