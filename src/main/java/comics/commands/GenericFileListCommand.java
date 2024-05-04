package comics.commands;

import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static comics.utils.Utils.pgBuilder;

public class GenericFileListCommand {

    @FunctionalInterface
    interface FileSelector {
        List<File> filter(File directory);
    }

    @FunctionalInterface
    interface CommandProcessor {
        void processCommand(File file) throws Exception;
    }

    int execute(
        FileSelector selector,
        CommandProcessor processor,
        String caption,
        Path cwd
    ) {
        var counter = new AtomicInteger(0);
        var errors = new Hashtable<File, Exception>();
        var entries = selector.filter(cwd.toFile());
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
