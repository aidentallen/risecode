package dev.riserecode;

import dev.riserecode.resources.ResourcesLibrary;
import dev.riserecode.resources.ResourceIo;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Explicit developer tool; neither downloaded packs nor this tool enter the release JAR. */
public final class ResourcesCacheTool {
    private ResourcesCacheTool() { }
    public static void main(String[] args) throws Exception {
        if (args.length < 3) { throw new IllegalArgumentException("cache-directory pack-format-major pack-format-minor [refresh]"); }
        try (var library = new ResourcesLibrary(Path.of(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]))) {
            library.start(args.length > 3 && args[3].equals("refresh"));
            String previous = "";
            while (library.progress().running()) {
                var progress = library.progress(); String status = progress.phase() + " " + progress.completed() + "/" + progress.total();
                if (!status.equals(previous)) { System.out.println(status); previous = status; }
                TimeUnit.SECONDS.sleep(2);
            }
            var report = library.report(); report.addProperty("providerRequests", dev.riserecode.resources.VanillaTweaksProvider.requestCount()); System.out.println(report);
            ResourceIo.write(Path.of(args[0]).resolve("verification.json"), report);
            if (!library.progress().ready()) { throw new IllegalStateException("Offline cache is incomplete: " + library.progress().failures()); }
        }
    }
}
