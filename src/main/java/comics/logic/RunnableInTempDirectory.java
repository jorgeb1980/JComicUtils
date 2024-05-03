package comics.logic;

import java.io.File;

@FunctionalInterface
public interface RunnableInTempDirectory {
    void run(File directory) throws Exception;
}
