package com.arrl.radiocraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every model the game uses (from the blockstates and item models, through their parents) loads in Minecraft 1.21.1:
 * element rotations on one axis by -45, -22.5, 0, 22.5 or 45 degrees, and every radiocraft texture it names exists.
 * A Blockbench export set to a later Minecraft version breaks the first rule, and the model shows as missing in game.
 */
class ModelFilesTest {

    private static final Path PROJECT = projectRoot();
    private static final Path MAIN = PROJECT.resolve("src/main/resources/assets/radiocraft");
    private static final Path GENERATED = PROJECT.resolve("src/generated/resources/assets/radiocraft");
    private static final Set<Double> LEGAL = Set.of(0.0D, 22.5D, -22.5D, 45.0D, -45.0D);
    /**
     * Models known not to load yet, to fix in Blockbench (see todo.md). The test fails once one of them is fine, so it
     * comes off this list.
     */
    private static final Set<String> KNOWN_BROKEN = Set.of("radiocraft:block/wefax_receiver");

    @Test
    void usedModelsLoadInMinecraft1211() throws IOException {
        ArrayDeque<String> queue = new ArrayDeque<>();
        for(Path root : List.of(MAIN, GENERATED)) {
            for(Path file : jsonFiles(root.resolve("blockstates")))
                collectModels(JsonParser.parseString(Files.readString(file)), queue);
            for(Path file : jsonFiles(root.resolve("models/item")))
                queue.add("radiocraft:item/" + file.getFileName().toString().replace(".json", ""));
        }
        Set<String> seen = new HashSet<>();
        List<String> problems = new ArrayList<>();
        while(!queue.isEmpty()) {
            String id = queue.poll();
            if(!id.startsWith("radiocraft:") || !seen.add(id))
                continue;
            Path file = find("models/" + id.substring("radiocraft:".length()) + ".json");
            if(file == null)
                continue; // Item models for blocks may come from the block; a truly missing one is a datagen problem.
            JsonObject model = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if(model.has("parent"))
                queue.add(qualify(model.get("parent").getAsString()));
            if(model.has("textures"))
                for(var entry : model.getAsJsonObject("textures").entrySet()) {
                    String texture = entry.getValue().getAsString();
                    if(texture.startsWith("radiocraft:") && find("textures/" + texture.substring("radiocraft:".length()) + ".png") == null)
                        problems.add(id + ": texture " + texture + " does not exist");
                }
            if(model.has("elements"))
                for(JsonElement element : model.getAsJsonArray("elements")) {
                    JsonObject rotation = element.getAsJsonObject().getAsJsonObject("rotation");
                    if(rotation == null)
                        continue;
                    if(!rotation.has("axis"))
                        problems.add(id + ": a rotation without an axis (Blockbench's form for later Minecraft versions)");
                    else if(!LEGAL.contains(rotation.get("angle").getAsDouble()))
                        problems.add(id + ": a rotation of " + rotation.get("angle").getAsDouble() + " degrees");
                }
        }
        List<String> unexpected = problems.stream().filter(problem -> KNOWN_BROKEN.stream().noneMatch(problem::startsWith)).toList();
        assertTrue(unexpected.isEmpty(), String.join("\n", unexpected));
        for(String broken : KNOWN_BROKEN)
            assertTrue(problems.stream().anyMatch(problem -> problem.startsWith(broken + ":")), broken + " loads now: take it off KNOWN_BROKEN");
        assertTrue(seen.size() > 20, "Only found " + seen.size() + " models; is the test running from the project folder?");
    }

    private static void collectModels(JsonElement json, ArrayDeque<String> out) {
        if(json.isJsonObject()) {
            for(var entry : json.getAsJsonObject().entrySet()) {
                if(entry.getKey().equals("model") && entry.getValue().isJsonPrimitive())
                    out.add(qualify(entry.getValue().getAsString()));
                else
                    collectModels(entry.getValue(), out);
            }
        }
        else if(json.isJsonArray())
            for(JsonElement element : json.getAsJsonArray())
                collectModels(element, out);
    }

    /** The project folder: the working folder or the nearest one above it with the mod's resources. */
    private static Path projectRoot() {
        for(Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent())
            if(Files.isDirectory(dir.resolve("src/main/resources/assets/radiocraft")))
                return dir;
        return Path.of("");
    }

    private static String qualify(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static Path find(String relative) {
        for(Path root : List.of(MAIN, GENERATED))
            if(Files.exists(root.resolve(relative)))
                return root.resolve(relative);
        return null;
    }

    private static List<Path> jsonFiles(Path dir) throws IOException {
        if(!Files.isDirectory(dir))
            return List.of();
        try(Stream<Path> files = Files.list(dir)) {
            return files.filter(file -> file.toString().endsWith(".json")).toList();
        }
    }
}
