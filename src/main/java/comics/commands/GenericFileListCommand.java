package comics.commands;

import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static comics.utils.Utils.pgBuilder;

public class GenericFileListCommand {

    private Path cwd;
    private String caption;

    public GenericFileListCommand(Path cwd, String caption) {
        this.cwd = cwd;
        this.caption = caption;
    }

    @FunctionalInterface
    interface FileSelector {
        boolean filter(File file);
    }

    @FunctionalInterface
    interface CommandProcessor {
        void processCommand(File file) throws Exception;
    }

    int execute(
        FileSelector selector,
        CommandProcessor processor
    ) {
        assert cwd != null : "Please run the command on a non-null directory";
        assert cwd.toFile().isDirectory() : "Please run the command on a directory";

        var counter = new AtomicInteger(0);
        var errors = new Hashtable<File, Exception>();
        var entries = Arrays.stream(cwd.toFile().listFiles()).filter(f -> selector.filter(f)).toList();
        ProgressBar.wrap(entries.parallelStream(), pgBuilder(caption)).forEach(entry -> {
            try {
                processor.processCommand(entry);
                counter.incrementAndGet();
            } catch (Exception e) {
                errors.put(entry, e);
            }
        });
        // Report
        if (!errors.isEmpty())
            System.out.printf(
                "Could not process the following entries:%n%s",
                String.join(
                    "\n",
                    errors.entrySet().stream().map(
                        e -> String.format("%s - %s", e.getKey().getName(), e.getValue().getMessage())
                    ).toList()
                )
            );
        return 0;
    }
}
