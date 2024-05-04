package comics.utils;

import java.io.File;

@FunctionalInterface
public interface RunnableInTempDirectory {
    void run(File directory) throws Exception;
}
