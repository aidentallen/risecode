package dev.riserecode;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.riserecode.resources.ResourceArchive;
import dev.riserecode.resources.ResourceIo;
import dev.riserecode.resources.ResourcesLibrary;
import dev.riserecode.resources.ResourcesState;
import dev.riserecode.resources.VanillaTweaksProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Opt-in official-output comparisons; reference archives and downloaded assets stay in ignored build output. */
public final class ResourcesParityTool {
    private ResourcesParityTool() { }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]), output = Path.of(args[1]); boolean offline = args.length > 2 && args[2].equals("offline"); Files.createDirectories(output);
        var cases = List.of(
                List.of("StemToLog", "DirtPathToGrassPath", "CherryPicking", "NoCaveSounds"),
                List.of("QuieterRain", "QuieterThunder", "QuieterWater", "QuieterZombies", "SmootherFont"),
                List.of("3DDoors", "GlassDoors", "DarkUI", "NumberedHotbar"),
                List.of("3DRedstoneWire", "CleanRedstoneDust", "RedstonePowerLevels", "CherryPicking"),
                List.of("CopperToolOxidization", "DiminishingTools", "ShortSwords"),
                List.of("MZSteve", "MZAlex", "MZAri", "MZEfe", "MZKai", "MZMakena", "MZNoor", "MZSunny", "MZZuri", "ClearerWater", "WavyWater", "MobSpawnIndicator"),
                List.of("HorizontalNuggets", "SidewaysNuggets"),
                List.of("SidewaysNuggets", "HorizontalNuggets"));
        var report = new JsonObject(); var results = new JsonArray(); int failures = 0;
        try (var library = new ResourcesLibrary(root, 88, 0); var provider = new VanillaTweaksProvider()) {
            if (!library.progress().ready()) { throw new IllegalStateException("Full offline cache required"); }
            var catalog = library.catalog(); report.addProperty("revision", catalog.revision());
            int count = 0;
            for (var selection : cases) {
                var state = new ResourcesState(catalog.revision(), selection); var actual = library.compose(state);
                Path reference = output.resolve("reference-" + count + ".zip");
                byte[] raw = offline ? Files.readAllBytes(reference) : provider.pack(catalog, selection);
                if (!offline) { ResourceIo.writeBytes(reference, raw); }
                var archive = ResourceArchive.read(raw); archive.verifyReceipt(selection); var expected = archive.effective(88, 0);
                var result = new JsonObject(); result.add("selection", state.json()); result.addProperty("files", expected.size());
                try { ResourcesLibrary.assertEqual(actual, expected, "Mixed selection " + count); result.addProperty("matched", true); }
                catch (java.io.IOException mismatch) { failures++; result.addProperty("matched", false); result.addProperty("error", mismatch.getMessage()); }
                results.add(result); count++; System.out.println(result);
            }
        }
        report.add("cases", results); report.addProperty("passed", failures == 0); report.addProperty("offline", offline); ResourceIo.write(output.resolve("parity.json"), report);
        if (failures != 0) { throw new IllegalStateException(failures + " mixed selections differ from official output"); }
    }
}
