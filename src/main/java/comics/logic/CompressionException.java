package comics.logic;

import shell.ExecutionResults;

public class CompressionException extends Exception {

    public CompressionException(ExecutionResults result) {
        super(String.format("7z error (exit code: %d)%n%s%n", result.getExitCode(), result.getStandardOutput()));
    }

    public CompressionException(Throwable cause) {
        super(cause);
    }

}
