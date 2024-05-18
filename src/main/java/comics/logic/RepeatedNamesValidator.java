package comics.logic;

import comics.commands.FileValidator;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

// Will be executed by the generic file list operation in order to detect possible repetitions of file names after normalization
public class RepeatedNamesValidator implements FileValidator {

    private final List<File> files;

    public RepeatedNamesValidator() {
        files = new LinkedList<>();
    }

    @Override
    public void readFile(File file) {
        files.add(file);
    }

    @Override
    public void validate() throws Exception {
        var converter = new NameConverter();
        var normalizedNames = files.stream().map(
            f -> converter.normalizeFileName(f.getName())
        ).toList();
        var repeatedNormalizedFileNames = normalizedNames.stream().filter(
            normalizedName -> Collections.frequency(normalizedNames, normalizedName) > 1
        ).collect(Collectors.toSet());
        if (!repeatedNormalizedFileNames.isEmpty()) {
            throw new Exception(
                String.format(
                    "The following files have a naming conflict:%n%s",
                    String.join(System.lineSeparator(), repeatedNormalizedFileNames.stream().toList())
                )
            );
        }
    }
}
