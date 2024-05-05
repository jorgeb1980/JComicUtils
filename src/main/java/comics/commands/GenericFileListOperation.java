package comics.commands;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static comics.utils.Utils.emptyIfNull;
import static comics.utils.Utils.wrapWithProgressBar;

public record GenericFileListOperation(
    Path cwd,
    String caption
) {
    private final static Logger logger = Logger.getLogger("cli");

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
        var ret = 0;
        try {
            assert cwd != null : "Please run the command on a non-null directory";
            assert cwd.toFile().isDirectory() : "Please run the command on a directory";

            var errors = new Hashtable<File, Exception>();
            var entries = Arrays.stream(emptyIfNull(cwd.toFile().listFiles())).filter(selector::filter).toList();
            try (var myPool = new ForkJoinPool(8)) {
                var counter = new AtomicInteger(0);
                myPool.submit(() ->
                    wrapWithProgressBar(entries.parallelStream(), caption).forEach(entry -> {
                        try {
                            processor.processCommand(entry);
                            logger.log(
                                Level.FINE,
                                String.format(
                                    "Processed entry %s [%d/%d]",
                                    entry,
                                    counter.incrementAndGet(),
                                    entries.size()
                                )
                            );
                        } catch (Exception e) {
                            errors.put(entry, e);
                        }
                    })
                ).get();
            }
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
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            ret = -1;
        }
        return ret;
    }
}
