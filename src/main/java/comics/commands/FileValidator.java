package comics.commands;

import java.io.File;

public interface FileValidator {

    void readFile(File file);
    void validate() throws Exception;
}
